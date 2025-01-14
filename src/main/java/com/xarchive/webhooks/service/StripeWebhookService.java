package com.xarchive.webhooks.service;


import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionListLineItemsParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import com.xarchive.auditing.util.AuditLogger;
import com.xarchive.authentication.entity.User;
import com.xarchive.authentication.repository.UserRepository;
import com.stripe.model.*;
import com.xarchive.billing.entity.Payment;
import com.xarchive.billing.entity.Plan;
import com.xarchive.billing.repository.PaymentRepository;
import com.xarchive.billing.repository.PlanRepository;
import com.xarchive.config.ApplicationProperties;
import com.xarchive.licensing.entity.License;
import com.xarchive.licensing.repository.LicenseRepository;
import java.util.UUID;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
public class StripeWebhookService {

    private final LicenseRepository licenseRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final AuditLogger auditLogger;
    private final PlanRepository planRepository;
    private final ApplicationProperties appProperties;

    public StripeWebhookService(
            LicenseRepository licenseRepository,
            PaymentRepository paymentRepository,
            UserRepository userRepository,
            AuditLogger auditLogger, PlanRepository planRepository, ApplicationProperties appProperties) {
        this.licenseRepository = licenseRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.auditLogger = auditLogger;
        this.planRepository = planRepository;
        this.appProperties = appProperties;
        Stripe.apiKey= appProperties.getStripeApiKey();
    }

    public void handleEvent(Event event) throws Exception {
        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutSessionCompleted((Session) event.getDataObjectDeserializer().getObject().orElseThrow());
                break;
            case "charge.succeeded":
                handlePaymentSucceeded((Charge) event.getDataObjectDeserializer().getObject().orElseThrow());
                break;
            case "customer.subscription.updated":
                handleSubscriptionUpdated((Subscription) event.getDataObjectDeserializer().getObject().orElseThrow());
                break;
            case "customer.subscription.deleted":
                handleSubscriptionDeleted((Subscription) event.getDataObjectDeserializer().getObject().orElseThrow());
                break;
            default:
                auditLogger.log("Unsupported event type: " + event.getType(), "WEBHOOK", null);
        }
    }

    private void handleCheckoutSessionCompleted(Session session) throws StripeException {
        RequestOptions requestOptions = RequestOptions.builder().build();

        Session resource = Session.retrieve(
                session.getId(),
                SessionRetrieveParams.builder()
                        .addExpand("payment_intent")
                        .build(),
                requestOptions
        );
        SessionListLineItemsParams params = SessionListLineItemsParams.builder().build();
        LineItemCollection lineItems = resource.listLineItems(params);

        String customerId = resource.getCustomer();

        String paymentIntentId = session.getPaymentIntent();
        if (resource.getSubscription() != null && paymentIntentId == null) {
            Subscription subscription = Subscription.retrieve(resource.getSubscription());
            if (subscription.getLatestInvoice() != null) {
                Invoice invoice = Invoice.retrieve(subscription.getLatestInvoice());
                paymentIntentId = invoice.getPaymentIntent();
            }
        }
        log.info(paymentIntentId + "Should match whats in payment table!!!!");
        String subscriptionId = resource.getSubscription();
        String priceId = lineItems.getData().getFirst().getPrice().getId();
        Plan licenseType = getLicenseTypeFromPriceId(priceId);

        User user = userRepository.findByStripeCustomerId(customerId).orElse(null);
        if (user != null) {
            assert licenseType != null;
            License license = License.builder()
                    .user(user)
                    .stripeSubscriptionId(subscriptionId) // Set subscription id if there is one (monthly licenses)
                    .licenseNumber(UUID.randomUUID().toString()) // generate license number
                    .licenseType(licenseType)
                    .activationDate(Instant.now())
                    .expirationDate(licenseType.getBillingCycle().equals("ONE_TIME") ? null : Instant.now().plusSeconds(2629800)) // one month expiry from initial checkout session
                    .build();

            licenseRepository.save(license);
            Payment paymentRecord = paymentRepository.findByStripePaymentIntentId(paymentIntentId).orElse(null);
            if (paymentRecord != null) {
                paymentRecord.setLicense(license);
                paymentRepository.save(paymentRecord);
            } else {
                auditLogger.log("Could not associate License with payment record", "WEBHOOK", customerId); // implement queue or staging to fix this.
            }
            auditLogger.log("License activated for user: " + user.getId(), "WEBHOOK", customerId);
        }
    }

    private void handlePaymentSucceeded(Charge charge) {
        String customerId = charge.getCustomer();
        long amountPaid = charge.getAmount();
        String paymentIntentId = charge.getPaymentIntent();

        // Fetch the user and log payment
        User user = userRepository.findByStripeCustomerId(customerId).orElse(null);

        if (user != null) {
            Payment payment = new Payment();
            payment.setUser(user);
            payment.setAmount(BigDecimal.valueOf(amountPaid / 100.0));
            payment.setPaymentDate(Instant.now());
            payment.setStripePaymentIntentId(paymentIntentId);
            payment.setCreatedAt(Instant.ofEpochSecond(charge.getCreated()));
            paymentRepository.save(payment);
            auditLogger.log("Payment recorded for user: " + user.getId(), "WEBHOOK", customerId);
        }

    }

    private void handleSubscriptionUpdated(Subscription subscription) {
        String customerId = subscription.getCustomer();
        String status = subscription.getStatus();
        String subscriptionId = subscription.getId();

        // Verify user has a stripe customer object.
        User user = userRepository.findByStripeCustomerId(customerId).orElse(null);

        if (user != null) {
            License license = licenseRepository.findByStripeSubscriptionId(subscriptionId).orElse(null);
            if (license != null) {
                license.setStatus(status.toUpperCase()); // ACTIVE, CANCELED, EXPIRED
                licenseRepository.save(license);
                auditLogger.log("Subscription updated for user: " + user.getId(), "WEBHOOK", customerId);
            } else {
                auditLogger.log("Could not find the License associated with the subscription Id, possible this is the first update event and the license hasnt been created as of this point in time b/c this event fires before license creation subsequent webhooks of updates will work", "WEBHOOK", customerId);
            }
        }
    }

    private void handleSubscriptionDeleted(Subscription subscription) {
        String customerId = subscription.getCustomer();

        // Fetch the user and deactivate license
        User user = userRepository.findByStripeCustomerId(customerId).orElse(null);
        if (user != null) {
            License license = licenseRepository.findByUserId(user.getId()).orElse(null);
            if (license != null) {
                license.setEnabled(false);
                license.setStatus("CANCELLED");
                licenseRepository.save(license);

                auditLogger.log("Subscription cancelled for user: " + user.getId(), "WEBHOOK", customerId);
            }
        }
    }

    private Plan getLicenseTypeFromPriceId(String priceId) {
        Plan plan = planRepository.findByStripePriceId(priceId).orElse(null);
        if (plan != null) {
            return plan;
        }
        return null;
    }
}
