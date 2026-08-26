package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.PaymentResponse;
import Fouss.moncvproback.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final PaymentService paymentService;

    public static class CheckoutRequest {
        public String planType; // "PRO" ou "PREMIUM"
    }

    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(
            @RequestBody CheckoutRequest body,
            Authentication authentication
    ) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Non authentifié"));
        }

        try {
            PaymentResponse response =
                    paymentService.createSubscriptionPayment(body.planType, authentication);

            String checkoutUrl = response.getData() != null
                    ? response.getData().getPaymentUrl() // mappé depuis "checkout_url" en JSON
                    : null;

            if (checkoutUrl == null) {
                return ResponseEntity.status(502).body(Map.of(
                        "message", "Impossible de créer le paiement d'abonnement"));
            }

            return ResponseEntity.ok(Map.of("checkoutUrl", checkoutUrl));

        } catch (Exception e) {
            System.out.println("ERREUR CHECKOUT ABONNEMENT = " + e.getMessage());
            return ResponseEntity.status(400).body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> currentPlan(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("message", "Non authentifié"));
        }

        String planType = paymentService.getCurrentPlan(authentication.getName());
        return ResponseEntity.ok(Map.of("planType", planType));
    }
}