package Fouss.moncvproback.repository;

import Fouss.moncvproback.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByPaymentReference(String paymentReference);

    Optional<Subscription> findTopByUser_EmailAndStatusOrderByCreatedAtDesc(
            String email, String status);

    /**
     * ✅ NOUVEAU — Incrémentation ATOMIQUE et conditionnelle.
     *
     * Le UPDATE et la vérification ("downloads_used < :limit") se font en une
     * seule opération au niveau de la base : si deux requêtes arrivent en même
     * temps sur la même ligne, la base les sérialise automatiquement (verrou
     * de ligne le temps du UPDATE) — la deuxième requête relit forcément la
     * valeur déjà incrémentée par la première avant d'évaluer sa propre
     * condition. Impossible de dépasser la limite, même sous forte
     * concurrence, sans avoir à gérer de verrou applicatif nous-mêmes.
     *
     * @return le nombre de lignes modifiées : 1 si l'incrémentation a réussi,
     *         0 si la limite était déjà atteinte (ou id introuvable).
     */
    @Modifying
    @Query("UPDATE Subscription s SET s.downloadsUsed = s.downloadsUsed + 1 " +
            "WHERE s.id = :id AND s.downloadsUsed < :limit")
    int incrementDownloadsIfUnderLimit(@Param("id") Long id, @Param("limit") int limit);
}