package com.xarchive.billing.controller;

import com.stripe.model.checkout.Session;
import com.xarchive.authentication.entity.User;
import com.xarchive.authentication.security.JwtTokenProvider;
import com.xarchive.authentication.service.CustomUserDetailsService;
import com.xarchive.billing.payload.PaymentCenterResponse;
import com.xarchive.billing.payload.PaymentIntentRequest;
import com.xarchive.billing.repository.PaymentRepository;
import com.xarchive.billing.service.StripeService;
import com.xarchive.billing.service.BillingService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController()
@RequestMapping("/api/billing")
public class BillingController {

    private final StripeService stripeService;
    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final BillingService billingService;


    public BillingController(StripeService stripeService, CustomUserDetailsService customUserDetailsService, JwtTokenProvider jwtTokenProvider, PaymentRepository paymentRepository, BillingService billingService) {
        this.stripeService = stripeService;
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.billingService = billingService;
    }

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody PaymentIntentRequest paymentIntentRequest, HttpServletRequest request) {
        String accessToken = jwtTokenProvider.getJwtFromRequest(request);
        String username = jwtTokenProvider.getUsernameFromJWT(accessToken);
        User user = customUserDetailsService.getUserByUsername(username);

        if (user.getStripeCustomerId().isEmpty()) {
            return ResponseEntity.badRequest().body("Could not determine stripe customer id.");
        }

        // Generate Stripe Checkout session
        Session checkoutSession = stripeService.createCheckoutSession(paymentIntentRequest.getPriceId(), user.getStripeCustomerId());

        if (checkoutSession == null) {
            return ResponseEntity.badRequest().body("Failed to create payment session.");
        }

        // Return the Checkout URL to the frontend
        return ResponseEntity.ok().body(Map.of("url", checkoutSession.getUrl()));
    }

    @GetMapping("/payments")
    public ResponseEntity<?> getPayments(HttpServletRequest request) {
        String accessToken = jwtTokenProvider.getJwtFromRequest(request);
        String username = jwtTokenProvider.getUsernameFromJWT(accessToken);
        User user = customUserDetailsService.getUserByUsername(username);
        PaymentCenterResponse payments = billingService.getPayments(user.getId());
        return ResponseEntity.ok().body(payments);
    }

}
