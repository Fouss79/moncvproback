package Fouss.moncvproback.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusResponse {

    private String planType; // "FREE" | "PRO" | "PREMIUM"

    private boolean unlimitedDownloads;

    // Les 3 champs suivants sont null quand unlimitedDownloads == true ou
    // quand planType == "FREE" (aucun téléchargement possible de toute façon)
    private Integer downloadsUsed;
    private Integer downloadsLimit;
    private Integer downloadsRemaining;
}