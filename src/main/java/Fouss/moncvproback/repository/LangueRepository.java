package Fouss.moncvproback.repository;

import Fouss.moncvproback.entity.Competence;
import Fouss.moncvproback.entity.Langue;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LangueRepository extends JpaRepository<Langue, Long> {
    void deleteByCvId(Long id);
}
