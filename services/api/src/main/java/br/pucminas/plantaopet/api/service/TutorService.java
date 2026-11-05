package br.pucminas.plantaopet.api.service;

import br.pucminas.plantaopet.api.domain.Tutor;
import br.pucminas.plantaopet.api.exception.ConflictException;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.repository.TutorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class TutorService {

    private static final Logger log = LoggerFactory.getLogger(TutorService.class);

    private final TutorRepository tutorRepository;
    private final IdGenerator idGenerator;
    private final Clock clock;

    public TutorService(TutorRepository tutorRepository, IdGenerator idGenerator, Clock clock) {
        this.tutorRepository = tutorRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<Tutor> list() {
        return tutorRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Tutor get(UUID id) {
        return tutorRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Tutor", id));
    }

    @Transactional
    public Tutor create(String name, String email) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (tutorRepository.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new ConflictException("A tutor with this email already exists");
        }
        Instant now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
        Tutor tutor = tutorRepository.save(new Tutor(idGenerator.newId(), name.trim(), normalizedEmail, now));
        log.info("Tutor created tutorId={}", tutor.getId());
        return tutor;
    }
}
