package Fouss.moncvproback.controller;

import Fouss.moncvproback.dto.PaymentRequest;
import Fouss.moncvproback.dto.PaymentResponse;
import Fouss.moncvproback.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PaymentController {

    private final PaymentService paymentService;


    @PostMapping("/create")
    public ResponseEntity<PaymentResponse> createPayment(
            @RequestBody PaymentRequest request
    ) {

        PaymentResponse response = paymentService.createPayment(request);

        return ResponseEntity.ok(response);
    }
}