package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.PaymentRequest;
import Fouss.moncvproback.dto.PaymentResponse;
import Fouss.moncvproback.dto.PaymentStatusResponse;
import Fouss.moncvproback.entity.Payment;
import Fouss.moncvproback.repository.PaymentRepository;
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
    private PaymentRepository paymentRepository;


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
    @GetMapping("/status")
    public PaymentStatusResponse getStatus(
            @RequestParam String reference) {

        Payment payment = paymentService.getPaymentStatus(reference);

        return new PaymentStatusResponse(
                payment.getReference(),
                payment.getStatus()
        );
    }

    @GetMapping("/status/me")
    public ResponseEntity<Map<String, Boolean>> getMyStatus(Authentication authentication) {
        boolean paid = paymentService.hasCompletedPayment(authentication.getName());
        return ResponseEntity.ok(Map.of("paid", paid));
    }
}