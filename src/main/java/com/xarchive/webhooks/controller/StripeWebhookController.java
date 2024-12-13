package com.xarchive.webhooks.controller;

import com.xarchive.config.ApplicationProperties;
import com.xarchive.webhooks.service.StripeWebhookService;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/api/webhook")
public class StripeWebhookController {

    private static final Logger logger = Logger.getLogger(StripeWebhookController.class.getName());
    private final StripeWebhookService stripeWebhookService;
    private final String endpointSecret;

    public StripeWebhookController(StripeWebhookService stripeWebhookService, ApplicationProperties appProperties) {
        endpointSecret = appProperties.getStripeWebhookSecret();
        this.stripeWebhookService = stripeWebhookService;
    }

    @PostMapping("/stripe")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String signature) {
        try {
            // Verify Stripe signature
            Event event = Webhook.constructEvent(payload, signature, endpointSecret);

            // Handle the event type
            stripeWebhookService.handleEvent(event);

            return ResponseEntity.ok("Webhook handled");
        } catch (Exception e) {
            logger.severe("Error handling webhook: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Webhook handling error");
        }
    }
}
