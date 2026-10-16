package com.ecoaula.model;

import com.ecoaula.model.enums.EstadoContenido;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Entidad que representa una lección dentro de un curso.
 * Todo cambio de contenido se versiona a través de ContenidoVersion (ADR-08).
 */
@Entity
@Table(name = "contenidos")
public class Contenido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titulo;

    @Column(nullable = false)
    private Integer orden;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", nullable = false)
    private Curso curso;

    @OneToMany(mappedBy = "contenido", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroVersion DESC")
    private List<ContenidoVersion> versiones = new ArrayList<>();

    public Contenido() {
    }

    public Contenido(String titulo, Integer orden, Curso curso) {
        this.titulo = titulo;
        this.orden = orden;
        this.curso = curso;
    }

    /**
     * Retorna la versión vigente (PUBLICADA) de la lección (ADR-08).
     */
    public Optional<ContenidoVersion> getVersionPublicada() {
        return versiones.stream()
                .filter(v -> v.getEstado() == EstadoContenido.PUBLICADA)
                .findFirst();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public Integer getOrden() {
        return orden;
    }

    public void setOrden(Integer orden) {
        this.orden = orden;
    }

    public Curso getCurso() {
        return curso;
    }

    public void setCurso(Curso curso) {
        this.curso = curso;
    }

    public List<ContenidoVersion> getVersiones() {
        return versiones;
    }

    public void setVersiones(List<ContenidoVersion> versiones) {
        this.versiones = versiones;
    }
}
