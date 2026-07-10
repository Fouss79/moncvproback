package Fouss.moncvproback.dto;

import lombok.Data;
import lombok.Value;

import java.util.Map;

@Data
public class PaymentRequest {

    private Integer amount;
    private String paymentMethod;
    private String description;
    private Customer customer;
    private String success_url;
    private String error_url;
    private Map<String, String> metadata;


    @Data
    public static class Customer {
        private String name;
        private String email;
        private String phone;
    }
}