package br.pucminas.plantaopet.api.controller;

import br.pucminas.plantaopet.api.domain.Caregiver;
import br.pucminas.plantaopet.api.exception.ResourceNotFoundException;
import br.pucminas.plantaopet.api.service.CaregiverService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CaregiverController.class)
class CaregiverControllerTest {

    private static final UUID ACTIVE_ID = UUID.fromString("33333333-3333-3333-3333-333333333331");
    private static final UUID INACTIVE_ID = UUID.fromString("33333333-3333-3333-3333-333333333334");
    private static final Instant NOW = Instant.parse("2026-10-28T12:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CaregiverService caregiverService;

    @Test
    void listsOnlyActiveByDefault() throws Exception {
        when(caregiverService.list(true))
                .thenReturn(List.of(new Caregiver(ACTIVE_ID, "Carla Cuidadora", "carla@example.com", true, NOW)));

        mvc.perform(get("/caregivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void filtersInactiveWhenRequested() throws Exception {
        when(caregiverService.list(false))
                .thenReturn(List.of(new Caregiver(INACTIVE_ID, "Fábio Inativo", "fabio@example.com", false, NOW)));

        mvc.perform(get("/caregivers").param("active", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(INACTIVE_ID.toString()))
                .andExpect(jsonPath("$[0].active").value(false));
    }

    @Test
    void getUnknownReturns404() throws Exception {
        when(caregiverService.get(ACTIVE_ID)).thenThrow(new ResourceNotFoundException("Caregiver", ACTIVE_ID));

        mvc.perform(get("/caregivers/{id}", ACTIVE_ID))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }
}
