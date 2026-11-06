package br.pucminas.plantaopet.api.controller;

import br.pucminas.plantaopet.api.domain.Pet;
import br.pucminas.plantaopet.api.domain.PetSize;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.service.PetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PetController.class)
class PetControllerTest {

    private static final UUID TUTOR_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PET_ID = UUID.fromString("22222222-2222-2222-2222-222222222221");
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private PetService petService;

    @Test
    void createReturns201() throws Exception {
        when(petService.create(TUTOR_ID, "Thor", "DOG", PetSize.LARGE, null))
                .thenReturn(new Pet(PET_ID, TUTOR_ID, "Thor", "DOG", PetSize.LARGE, null, NOW));

        mvc.perform(post("/pets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tutorId":"11111111-1111-1111-1111-111111111111","name":"Thor","species":"DOG","size":"LARGE"}
                        """))
                .andExpect(status().isCreated())
                .andExpect(header().string(HttpHeaders.LOCATION, endsWith("/pets/" + PET_ID)))
                .andExpect(jsonPath("$.id").value(PET_ID.toString()))
                .andExpect(jsonPath("$.notes").isEmpty());
    }

    @Test
    void missingFieldsReturn400() throws Exception {
        mvc.perform(post("/pets").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Thor\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", hasItem("tutorId")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("species")));
        verifyNoInteractions(petService);
    }

    @Test
    void unknownTutorReturns404() throws Exception {
        when(petService.create(TUTOR_ID, "Thor", "DOG", null, null))
                .thenThrow(new ResourceNotFoundException("Tutor", TUTOR_ID));

        mvc.perform(post("/pets").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tutorId":"11111111-1111-1111-1111-111111111111","name":"Thor","species":"DOG"}
                        """))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void getReturnsPet() throws Exception {
        when(petService.get(PET_ID)).thenReturn(new Pet(PET_ID, TUTOR_ID, "Thor", "DOG", PetSize.LARGE, "dócil", NOW));

        mvc.perform(get("/pets/{id}", PET_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("dócil"));
    }
}
