package com.ecoaula.model;

import com.ecoaula.model.enums.ResultadoRevision;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que registra la revisión técnica y pedagógica de accesibilidad sobre una versión de contenido.
 * Garantiza la trazabilidad (quién revisó, cuándo, lista de chequeo y resultado).
 */
@Entity
@Table(name = "revisiones_accesibilidad")
public class RevisionAccesibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "version_id", nullable = false)
    private ContenidoVersion version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revisor_id", nullable = false)
    private Aprendiz revisor;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ResultadoRevision resultado;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "revision_chequeos", joinColumns = @JoinColumn(name = "revision_id"))
    @Column(name = "item_codigo")
    private Set<String> itemsChequeados = new HashSet<>();

    @PrePersist
    protected void onPrePersist() {
        if (fecha == null) {
            fecha = LocalDateTime.now();
        }
    }

    public RevisionAccesibilidad() {
    }

    public RevisionAccesibilidad(ContenidoVersion version, Aprendiz revisor,
                                 ResultadoRevision resultado, String motivo,
                                 Set<String> itemsChequeados) {
        this.version = version;
        this.revisor = revisor;
        this.resultado = resultado;
        this.motivo = motivo;
        if (itemsChequeados != null) {
            this.itemsChequeados = new HashSet<>(itemsChequeados);
        }
        this.fecha = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ContenidoVersion getVersion() {
        return version;
    }

    public void setVersion(ContenidoVersion version) {
        this.version = version;
    }

    public Aprendiz getRevisor() {
        return revisor;
    }

    public void setRevisor(Aprendiz revisor) {
        this.revisor = revisor;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public ResultadoRevision getResultado() {
        return resultado;
    }

    public void setResultado(ResultadoRevision resultado) {
        this.resultado = resultado;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public Set<String> getItemsChequeados() {
        return itemsChequeados;
    }

    public void setItemsChequeados(Set<String> itemsChequeados) {
        this.itemsChequeados = itemsChequeados;
    }
}
