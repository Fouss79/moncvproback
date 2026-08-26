package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.PaymentRequest;
import Fouss.moncvproback.dto.PaymentResponse;
import Fouss.moncvproback.dto.PaymentStatusResponse;
import Fouss.moncvproback.entity.Payment;
import Fouss.moncvproback.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ✅ SUPPRIMÉ : "private PaymentRepository paymentRepository;" existait ici
    // sans "final" et sans être utilisé dans la classe. Comme il n'était pas
    // final, Lombok @RequiredArgsConstructor ne l'injectait pas : il serait
    // resté "null" si jamais on l'avait utilisé (NullPointerException garanti
    // au premier appel). Inutile ici puisque tout passe déjà par paymentService.

    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.createPayment(request, authentication)
        );
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestHeader Map<String,String> headers,
            @RequestBody String payload
    ) {

        String event = headers.get("x-webhook-event");

        if (event == null) {
            return ResponseEntity.badRequest().build();
        }

        if ("webhook.test".equals(event)) {
            return ResponseEntity.ok().build();
        }
        System.out.println("EVENT = " + event);
        System.out.println("PAYLOAD = " + payload);

        paymentService.handleWebhook(event, payload);

        return ResponseEntity.ok().build();
    }

    /**
     * ✅ CORRIGÉ — Retourne un 404 avec message clair quand la référence
     * n'existe pas, au lieu de laisser fuiter l'exception générique levée par
     * getPaymentStatus() (probablement convertie en 400 par un handler global
     * ailleurs dans le projet, d'où le "400 Bad Request" observé côté frontend).
     *
     * Ça ne résout pas la boucle infinie côté frontend, mais ça donne au moins
     * une réponse claire et cohérente (404 = "introuvable", pas 400 = "requête
     * invalide" — la requête, elle, était parfaitement valide).
     */
    @GetMapping("/status")
    public ResponseEntity<?> getStatus(
            @RequestParam String reference) {

        try {
            Payment payment = paymentService.getPaymentStatus(reference);
            return ResponseEntity.ok(new PaymentStatusResponse(
                    payment.getReference(), payment.getStatus()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "message", "Aucun paiement trouvé pour cette référence"));
        }
    }

    @GetMapping("/status/me")
    public ResponseEntity<Map<String, Boolean>> getMyStatus(Authentication authentication) {
        boolean paid = paymentService.hasCompletedPayment(authentication.getName());
        return ResponseEntity.ok(Map.of("paid", paid));
    }
}