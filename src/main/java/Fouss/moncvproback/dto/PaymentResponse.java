package Fouss.moncvproback.dto;

import lombok.Data;

@Data
public class PaymentResponse {

    private String id;
    private String status;
    private String message;
    private String paymentUrl;

}