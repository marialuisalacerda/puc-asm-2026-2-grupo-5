package br.pucminas.plantaopet.api.controller;

import br.pucminas.plantaopet.api.controller.dto.CreatePetRequest;
import br.pucminas.plantaopet.api.controller.dto.PetResponse;
import br.pucminas.plantaopet.api.domain.Pet;
import br.pucminas.plantaopet.api.service.PetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

@RestController
@RequestMapping("/pets")
public class PetController {

    private final PetService petService;

    public PetController(PetService petService) {
        this.petService = petService;
    }

    @PostMapping
    public ResponseEntity<PetResponse> create(@Valid @RequestBody CreatePetRequest request) {
        Pet pet = petService.create(request.tutorId(), request.name(), request.species(),
                request.size(), request.notes());
        return ResponseEntity
                .created(ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(pet.getId()).toUri())
                .body(PetResponse.from(pet));
    }

    @GetMapping("/{petId}")
    public PetResponse get(@PathVariable UUID petId) {
        return PetResponse.from(petService.get(petId));
    }
}
