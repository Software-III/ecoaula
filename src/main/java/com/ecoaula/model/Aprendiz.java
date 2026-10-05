package com.ecoaula.model;

import com.ecoaula.model.enums.Rol;
import com.ecoaula.security.CondicionVisualConverter;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad principal que representa a un aprendiz en la plataforma EcoAula.
 * Incluye campos normativos para la Ley 1581/2012 (consentimiento y cifrado de datos sensibles).
 */
@Entity
@Table(name = "aprendices")
public class Aprendiz {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreCompleto;

    @Column(nullable = false, unique = true)
    private String correo;

    @Column(nullable = false)
    private String telefono;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol = Rol.APRENDIZ;

    @Convert(converter = CondicionVisualConverter.class)
    @Column(columnDefinition = "TEXT")
    private String condicionVisual;

    @Column(nullable = false)
    private boolean consentimientoDatos = false;

    @Column(nullable = false)
    private LocalDateTime fechaConsentimiento;

    @Column(nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    @OneToOne(mappedBy = "aprendiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PerfilAccesibilidad perfilAccesibilidad;

    @PrePersist
    protected void onPrePersist() {
        if (creadoEn == null) {
            creadoEn = LocalDateTime.now();
        }
        if (fechaConsentimiento == null && consentimientoDatos) {
            fechaConsentimiento = LocalDateTime.now();
        }
    }

    public Aprendiz() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public void setNombreCompleto(String nombreCompleto) {
        this.nombreCompleto = nombreCompleto;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    public String getCondicionVisual() {
        return condicionVisual;
    }

    public void setCondicionVisual(String condicionVisual) {
        this.condicionVisual = condicionVisual;
    }

    public boolean isConsentimientoDatos() {
        return consentimientoDatos;
    }

    public void setConsentimientoDatos(boolean consentimientoDatos) {
        this.consentimientoDatos = consentimientoDatos;
    }

    public LocalDateTime getFechaConsentimiento() {
        return fechaConsentimiento;
    }

    public void setFechaConsentimiento(LocalDateTime fechaConsentimiento) {
        this.fechaConsentimiento = fechaConsentimiento;
    }

    public LocalDateTime getCreadoEn() {
        return creadoEn;
    }

    public void setCreadoEn(LocalDateTime creadoEn) {
        this.creadoEn = creadoEn;
    }

    public PerfilAccesibilidad getPerfilAccesibilidad() {
        return perfilAccesibilidad;
    }

    public void setPerfilAccesibilidad(PerfilAccesibilidad perfilAccesibilidad) {
        this.perfilAccesibilidad = perfilAccesibilidad;
        if (perfilAccesibilidad != null) {
            perfilAccesibilidad.setAprendiz(this);
        }
    }
}
