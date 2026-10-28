package br.pucminas.plantaopet.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Pet de um tutor. O relacionamento é mantido pelo identificador (tutor_id) para manter
 * o agregado pequeno e evitar carregamento implícito entre entidades.
 */
@Entity
@Table(name = "pets")
public class Pet {

    @Id
    private UUID id;

    @Column(name = "tutor_id", nullable = false, updatable = false)
    private UUID tutorId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(nullable = false, length = 40)
    private String species;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PetSize size;

    @Column(length = 500)
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Pet() {
        // JPA
    }

    public Pet(UUID id, UUID tutorId, String name, String species, PetSize size, String notes, Instant now) {
        this.id = Objects.requireNonNull(id);
        this.tutorId = Objects.requireNonNull(tutorId);
        this.name = Objects.requireNonNull(name);
        this.species = Objects.requireNonNull(species);
        this.size = size;
        this.notes = notes;
        this.createdAt = Objects.requireNonNull(now);
        this.updatedAt = now;
    }

    public boolean belongsTo(UUID tutorId) {
        return this.tutorId.equals(tutorId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getTutorId() {
        return tutorId;
    }

    public String getName() {
        return name;
    }

    public String getSpecies() {
        return species;
    }

    public PetSize getSize() {
        return size;
    }

    public String getNotes() {
        return notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
