package Fouss.moncvproback.dto;
import lombok.Data;
import java.util.Map;

@Data
public class PaymentResponse {

    private Boolean success;
    private PaymentData data;


    @Data
    public static class PaymentData {

        private Long id;
        private String reference;
        private Integer amount;
        private Integer fees;
        private Integer netAmount;
        private String status;
        private String paymentUrl;
        private String gateway;
        private String environment;
        private Map<String, String> metadata;
    }
}