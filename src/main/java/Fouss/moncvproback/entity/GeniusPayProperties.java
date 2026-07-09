package Fouss.moncvproback.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "geniuspay")
@Data
public class GeniusPayProperties {

    private String apiUrl;
    private String apiKey;
    private String apiSecret;

}