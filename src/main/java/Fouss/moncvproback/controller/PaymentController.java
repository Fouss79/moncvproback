package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.PaymentRequest;
import Fouss.moncvproback.dto.PaymentResponse;
import Fouss.moncvproback.entity.Payment;
import Fouss.moncvproback.repository.PaymentRepository;
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
            @RequestHeader(value = "X-Webhook-Event", required = false) String event,
            @RequestBody String payload
    ) {

        System.out.println("WEBHOOK EVENT = " + event);
        System.out.println("WEBHOOK PAYLOAD = " + payload);

        paymentService.handleWebhook(event, payload);

        return ResponseEntity.ok().build();
    }
    @GetMapping("/status")
    public Payment getStatus(@RequestParam String reference) {
        return paymentService.getPaymentStatus(reference);
    }

}