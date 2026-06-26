package Fouss.moncvproback.repository;

import Fouss.moncvproback.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CvRepository extends JpaRepository<Cv, Long> {

    List<Cv> findByUserId(Long userId);
    Optional<Cv> findFirstByUserId(Long userId);
}