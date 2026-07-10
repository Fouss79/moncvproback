package Fouss.moncvproback.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentResponse {

    private Boolean success;

    private String message;

    private PaymentData data;


    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PaymentData {

        private Long id;

        private String reference;

        private Integer amount;

        private String currency;

        private Integer fees;

        @JsonProperty("net_amount")
        private Integer netAmount;

        private String status;

        @JsonProperty("payment_method")
        private String paymentMethod;

        @JsonProperty("payment_provider")
        private String paymentProvider;

        private String gateway;

        private String environment;

        @JsonProperty("checkout_url")
        private String paymentUrl;

        @JsonProperty("success_url")
        private String successUrl;

        @JsonProperty("error_url")
        private String errorUrl;

        @JsonProperty("completed_at")
        private String completedAt;

        private Map<String, Object> metadata;
    }}