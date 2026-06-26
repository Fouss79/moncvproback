package Fouss.moncvproback.repository;

import Fouss.moncvproback.entity.Competence;
import Fouss.moncvproback.entity.Cv;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CompetenceRepository  extends JpaRepository<Competence, Long> {
    void deleteByCvId(Long id);
}
