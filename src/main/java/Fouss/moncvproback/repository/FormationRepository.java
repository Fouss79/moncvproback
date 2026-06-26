package Fouss.moncvproback.repository;

import Fouss.moncvproback.entity.Competence;
import Fouss.moncvproback.entity.Formation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FormationRepository extends JpaRepository<Formation, Long> {
    void deleteByCvId(Long id);
}
