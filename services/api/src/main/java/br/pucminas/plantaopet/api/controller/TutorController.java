package br.pucminas.plantaopet.api.controller;

import br.pucminas.plantaopet.api.controller.dto.CreateTutorRequest;
import br.pucminas.plantaopet.api.controller.dto.PetResponse;
import br.pucminas.plantaopet.api.controller.dto.TutorResponse;
import br.pucminas.plantaopet.api.domain.Tutor;
import br.pucminas.plantaopet.api.service.PetService;
import br.pucminas.plantaopet.api.service.TutorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tutors")
public class TutorController {

    private final TutorService tutorService;
    private final PetService petService;

    public TutorController(TutorService tutorService, PetService petService) {
        this.tutorService = tutorService;
        this.petService = petService;
    }

    @GetMapping
    public List<TutorResponse> list() {
        return tutorService.list().stream().map(TutorResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<TutorResponse> create(@Valid @RequestBody CreateTutorRequest request) {
        Tutor tutor = tutorService.create(request.name(), request.email());
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(tutor.getId()).toUri())
                .body(TutorResponse.from(tutor));
    }

    @GetMapping("/{tutorId}")
    public TutorResponse get(@PathVariable UUID tutorId) {
        return TutorResponse.from(tutorService.get(tutorId));
    }

    @GetMapping("/{tutorId}/pets")
    public List<PetResponse> listPets(@PathVariable UUID tutorId) {
        return petService.listByTutor(tutorId).stream().map(PetResponse::from).toList();
    }
}
