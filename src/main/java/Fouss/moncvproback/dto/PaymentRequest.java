package Fouss.moncvproback.dto;

import lombok.Data;

@Data
public class PaymentRequest {

    private Integer amount;
    private Customer customer;

    @Data
    public static class Customer {
        private String phone;
        private String name;
        private String email;
    }
}