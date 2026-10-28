package br.pucminas.plantaopet.api.service;

import br.pucminas.plantaopet.api.domain.Pet;
import br.pucminas.plantaopet.api.domain.PetSize;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.repository.PetRepository;
import br.pucminas.plantaopet.api.repository.TutorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class PetService {

    private static final Logger log = LoggerFactory.getLogger(PetService.class);

    private final PetRepository petRepository;
    private final TutorRepository tutorRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public PetService(PetRepository petRepository, TutorRepository tutorRepository,
                      IdGenerator idGenerator, Clock clock) {
        this.petRepository = petRepository;
        this.tutorRepository = tutorRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Pet get(UUID id) {
        return petRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Pet", id));
    }

    @Transactional(readOnly = true)
    public List<Pet> listByTutor(UUID tutorId) {
        requireTutor(tutorId);
        return petRepository.findByTutorIdOrderByNameAsc(tutorId);
    }

    @Transactional
    public Pet create(UUID tutorId, String name, String species, PetSize size, String notes) {
        requireTutor(tutorId);
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Pet pet = petRepository.save(new Pet(idGenerator.newId(), tutorId, name.trim(), species.trim(),
                size, notes, now));
        log.info("Pet created petId={} tutorId={}", pet.getId(), tutorId);
        return pet;
    }

    private void requireTutor(UUID tutorId) {
        if (!tutorRepository.existsById(tutorId)) {
            throw new ResourceNotFoundException("Tutor", tutorId);
        }
    }
}
