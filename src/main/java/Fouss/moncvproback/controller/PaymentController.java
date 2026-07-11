package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.PaymentRequest;
import Fouss.moncvproback.dto.PaymentResponse;
import Fouss.moncvproback.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;


    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request,
            Authentication authentication) {

        return ResponseEntity.ok(
                paymentService.createPayment(request, authentication)
        );
    }


    @GetMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(
            @RequestParam String reference) {

        return ResponseEntity.ok(
                paymentService.verifyPayment(reference)
        );
    }
    @PostMapping("/webhook")
    public ResponseEntity<?> webhook(
            @RequestHeader(value = "X-Webhook-Event", required = false) String event,
            @RequestBody String payload
    ) {

        System.out.println("WEBHOOK EVENT = " + event);
        System.out.println("WEBHOOK PAYLOAD = " + payload);

        paymentService.handleWebhook(event, payload);

        return ResponseEntity.ok().build();
    }

}