package com.xarchive.billing.service;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.checkout.Session;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.xarchive.authentication.entity.User;
import com.xarchive.authentication.repository.UserRepository;
import com.xarchive.config.ApplicationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StripeService {

    private final StripeClient client;
    private final UserRepository userRepository;


    public StripeService(ApplicationProperties appProperties, UserRepository userRepository) {
        Stripe.apiKey= appProperties.getStripeApiKey();
        client = new StripeClient(appProperties.getStripeApiKey());
        this.userRepository = userRepository;
    }

    public boolean createCustomer(User user) {
        CustomerCreateParams params =
                CustomerCreateParams
                        .builder()
                        .setName(user.getFirstName() + " " + user.getLastName())
                        .setEmail(user.getUsername())
                        .build();

        try {
            Customer customer = client.customers().create(params);
            String customerId = customer.getId();
            user.setStripeCustomerId(customerId);
            userRepository.save(user);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return false;
    }

    public Session createCheckoutSession(String stripePriceId, String stripeCustomerId) {
        String DOMAIN = "http://localhost:5173/";
        // determine mode based off of lifetime license. Lifetime = Payment mode, anything else = Subscription
        SessionCreateParams.Mode mode = stripePriceId.equals("price_1QTxNYGYxMWOnGCC5PfAcJg6") ? SessionCreateParams.Mode.PAYMENT : SessionCreateParams.Mode.SUBSCRIPTION;

        SessionCreateParams params =
                SessionCreateParams.builder()
                        .addExpand("line_items")
                        .setMode(mode)
                        .setSuccessUrl(DOMAIN + "?success=true")
                        .setCancelUrl(DOMAIN + "?canceled=true")
                        .setCustomer(stripeCustomerId)
                        .addLineItem(
                                SessionCreateParams.LineItem.builder()
                                        .setQuantity(1L)
                                        .setPrice(stripePriceId)
                                        .build())
                        .build();
        try {
            return Session.create(params);
        } catch (StripeException e) {
            log.error(e.getMessage());
            return null;
        }
    }
}
