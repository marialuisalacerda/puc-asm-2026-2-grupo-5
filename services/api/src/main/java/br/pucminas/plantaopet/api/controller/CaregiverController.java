package br.pucminas.plantaopet.api.controller;

import br.pucminas.plantaopet.api.controller.dto.CaregiverResponse;
import br.pucminas.plantaopet.api.service.CaregiverService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/caregivers")
public class CaregiverController {

    private final CaregiverService caregiverService;

    public CaregiverController(CaregiverService caregiverService) {
        this.caregiverService = caregiverService;
    }

    @GetMapping
    public List<CaregiverResponse> list(@RequestParam(defaultValue = "true") boolean active) {
        return caregiverService.list(active).stream().map(CaregiverResponse::from).toList();
    }

    @GetMapping("/{caregiverId}")
    public CaregiverResponse get(@PathVariable UUID caregiverId) {
        return CaregiverResponse.from(caregiverService.get(caregiverId));
    }
}
