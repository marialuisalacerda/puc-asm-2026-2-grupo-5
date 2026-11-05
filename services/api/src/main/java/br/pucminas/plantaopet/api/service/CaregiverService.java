package br.pucminas.plantaopet.api.service;

import br.pucminas.plantaopet.api.domain.Caregiver;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.repository.CaregiverRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CaregiverService {

    private final CaregiverRepository caregiverRepository;

    public CaregiverService(CaregiverRepository caregiverRepository) {
        this.caregiverRepository = caregiverRepository;
    }

    /** Filtra pelo valor de {@code active} (o controller usa {@code true} por padrão). */
    @Transactional(readOnly = true)
    public List<Caregiver> list(boolean active) {
        return caregiverRepository.findByActiveOrderByNameAsc(active);
    }

    @Transactional(readOnly = true)
    public Caregiver get(UUID id) {
        return caregiverRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Caregiver", id));
    }
}
