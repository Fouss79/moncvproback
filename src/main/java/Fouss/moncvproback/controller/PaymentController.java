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
            @RequestBody(required = false) String payload
    ) {

        try {

            System.out.println("========== WEBHOOK RECU ==========");
            System.out.println("EVENT = " + event);
            System.out.println("PAYLOAD = " + payload);

            paymentService.handleWebhook(event, payload);

            return ResponseEntity.ok().build();

        } catch(Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .status(500)
                    .body(e.getMessage());
        }
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

}