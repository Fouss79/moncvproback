package Fouss.moncvproback.enums;

/**
 * Montants, durées et limites de téléchargement définis ICI, côté serveur.
 * Ne jamais faire confiance à un montant/quota envoyé par le frontend.
 */
public enum PlanType {
    PRO(500, 30, "Abonnement Pro - 1 mois", 5),
    PREMIUM(6000, 365, "Abonnement Premium - 1 an", null); // null = illimité

    private final long amountXof;
    private final int durationDays;
    private final String description;
    private final Integer downloadLimit; // null = illimité

    PlanType(long amountXof, int durationDays, String description, Integer downloadLimit) {
        this.amountXof = amountXof;
        this.durationDays = durationDays;
        this.description = description;
        this.downloadLimit = downloadLimit;
    }

    public long getAmountXof() {
        return amountXof;
    }

    public int getDurationDays() {
        return durationDays;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDownloadLimit() {
        return downloadLimit;
    }

    public boolean isUnlimitedDownloads() {
        return downloadLimit == null;
    }
}