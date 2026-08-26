package Fouss.moncvproback.controller;

import Fouss.moncvproback.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    // ✅ SUPPRIMÉS — le paiement à l'unité (200 FCFA/téléchargement) est
    // remplacé par l'abonnement. Les endpoints /create, /status et /status/me
    // n'existent plus pour éviter que quelqu'un puisse encore déclencher un
    // paiement à l'unité par erreur (ancien lien, ancien bookmark, etc.).
    //
    // Les 3 paiements déjà effectués restent en base (table "payment") pour
    // l'historique — rien n'a été supprimé côté données, seulement les routes
    // qui permettaient d'en créer de nouveaux.
    //
    // Si tu veux un jour consulter cet historique depuis un back-office, on
    // pourra réintroduire un endpoint GET en lecture seule, réservé à un rôle
    // ADMIN — mais plus d'endpoint de création ni de polling public.

    /**
     * Reste indispensable : c'est GeniusPay qui appelle cette URL, pour les
     * abonnements Pro/Premium comme pour tout paiement encore en attente créé
     * avant la bascule. On ne touche pas à cette route.
     */
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestHeader Map<String, String> headers,
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
}