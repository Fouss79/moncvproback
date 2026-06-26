package Fouss.moncvproback.repository;

import Fouss.moncvproback.entity.Contact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContactRepository extends JpaRepository<Contact, Long> {

    void deleteByCvId(Long cvId);

    Optional<Contact> findByCvId(Long cvId);
}