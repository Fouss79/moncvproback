package Fouss.moncvproback.repository;

import Fouss.moncvproback.entity.Competence;
import Fouss.moncvproback.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceRepository extends JpaRepository<Experience, Long> {
    void deleteByCvId(Long id);
}
