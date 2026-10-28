package br.pucminas.plantaopet.api.repository;

import br.pucminas.plantaopet.api.domain.Caregiver;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CaregiverRepository extends JpaRepository<Caregiver, UUID> {

    List<Caregiver> findByActiveOrderByNameAsc(boolean active);

    /**
     * Carrega o cuidador com SELECT ... FOR UPDATE. Usado na criação de reservas para
     * serializar solicitações concorrentes do mesmo cuidador e evitar que duas reservas
     * sobrepostas passem pela verificação de conflito ao mesmo tempo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Caregiver c where c.id = :id")
    Optional<Caregiver> findByIdForUpdate(@Param("id") UUID id);
}
