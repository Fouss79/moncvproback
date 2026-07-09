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

        private String status;

        private String gateway;

        private String environment;

        @JsonProperty("checkout_url")
        private String paymentUrl;

        private Map<String, Object> metadata;
    }
}