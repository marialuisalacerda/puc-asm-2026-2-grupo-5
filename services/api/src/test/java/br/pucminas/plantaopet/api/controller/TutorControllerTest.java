package br.pucminas.plantaopet.api.controller;

import br.pucminas.plantaopet.api.domain.Pet;
import br.pucminas.plantaopet.api.domain.PetSize;
import br.pucminas.plantaopet.api.domain.Tutor;
import br.pucminas.plantaopet.api.exception.ConflictException;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.service.PetService;
import br.pucminas.plantaopet.api.service.TutorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TutorController.class)
class TutorControllerTest {

    private static final UUID TUTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TutorService tutorService;

    @MockBean
    private PetService petService;

    @Test
    void listReturnsTutors() throws Exception {
        when(tutorService.list()).thenReturn(List.of(new Tutor(TUTOR_ID, "Ana Tutora", "ana.tutora@example.com", NOW)));

        mvc.perform(get("/tutors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(TUTOR_ID.toString()))
                .andExpect(jsonPath("$[0].email").value("ana.tutora@example.com"));
    }

    @Test
    void createReturns201() throws Exception {
        when(tutorService.create("Ana Tutora", "ana@example.com"))
                .thenReturn(new Tutor(TUTOR_ID, "Ana Tutora", "ana@example.com", NOW));

        mvc.perform(post("/tutors").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana Tutora\",\"email\":\"ana@example.com\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/tutors/" + TUTOR_ID)))
                .andExpect(jsonPath("$.id").value(TUTOR_ID.toString()));
    }

    @Test
    void invalidEmailReturns400WithErrors() throws Exception {
        mvc.perform(post("/tutors").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")));
        verifyNoInteractions(tutorService);
    }

    @Test
    void malformedJsonReturns400() throws Exception {
        mvc.perform(post("/tutors").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void duplicatedEmailReturns409() throws Exception {
        when(tutorService.create("Ana", "ana@example.com")).thenThrow(new ConflictException("duplicated"));

        mvc.perform(post("/tutors").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Ana\",\"email\":\"ana@example.com\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getUnknownTutorReturns404() throws Exception {
        when(tutorService.get(TUTOR_ID)).thenThrow(new ResourceNotFoundException("Tutor", TUTOR_ID));

        mvc.perform(get("/tutors/{id}", TUTOR_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listPetsOfTutor() throws Exception {
        UUID petId = UUID.fromString("22222222-2222-2222-2222-222222222221");
        when(petService.listByTutor(TUTOR_ID))
                .thenReturn(List.of(new Pet(petId, TUTOR_ID, "Thor", "DOG", PetSize.LARGE, null, NOW)));

        mvc.perform(get("/tutors/{id}/pets", TUTOR_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(petId.toString()))
                .andExpect(jsonPath("$[0].tutorId").value(TUTOR_ID.toString()))
                .andExpect(jsonPath("$[0].size").value("LARGE"));
    }

    @Test
    void listPetsOfUnknownTutorReturns404() throws Exception {
        when(petService.listByTutor(TUTOR_ID)).thenThrow(new ResourceNotFoundException("Tutor", TUTOR_ID));

        mvc.perform(get("/tutors/{id}/pets", TUTOR_ID)).andExpect(status().isNotFound());
    }
}
