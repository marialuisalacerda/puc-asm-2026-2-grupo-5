package br.pucminas.plantaopet.api.controller.dto;

import jakarta.validation.constraints.Size;

public record ReasonRequest(@Size(max = 300) String reason) {
}
