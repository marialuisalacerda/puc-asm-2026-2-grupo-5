package br.pucminas.plantaopet.api.service;

import java.util.UUID;

/** Gera identificadores na aplicação (o schema não usa DEFAULT para UUID). Substituível em testes. */
@FunctionalInterface
public interface IdGenerator {

    UUID newId();
}
