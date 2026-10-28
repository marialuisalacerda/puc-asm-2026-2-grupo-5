package br.pucminas.plantaopet.api.repository;

import br.pucminas.plantaopet.api.domain.Tutor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TutorRepository extends JpaRepository<Tutor, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    List<Tutor> findAllByOrderByNameAsc();
}
