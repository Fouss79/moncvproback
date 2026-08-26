package Fouss.moncvproback.enums;

/**
 * Montants et durées définis ICI, côté serveur.
 * Ne jamais faire confiance à un montant envoyé par le frontend.
 */
public enum PlanType {

    PRO(2500, 30, "Abonnement Pro - 1 mois"),
    PREMIUM(6000, 365, "Abonnement Premium - 1 an");

    private final long amountXof;
    private final int durationDays;
    private final String description;

    PlanType(long amountXof, int durationDays, String description) {
        this.amountXof = amountXof;
        this.durationDays = durationDays;
        this.description = description;
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
}