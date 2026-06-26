package Fouss.moncvproback.repository;

import Fouss.moncvproback.entity.Competence;
import Fouss.moncvproback.entity.Loisir;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoisirRepository extends JpaRepository<Loisir, Long> {
    void deleteByCvId(Long id);
}
