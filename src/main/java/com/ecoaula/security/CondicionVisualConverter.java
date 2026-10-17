package com.ecoaula.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Convertidor JPA para cifrar y descifrar la condición visual del aprendiz (ADR-09).
 */
@Component
@Converter
public class CondicionVisualConverter implements AttributeConverter<String, String> {

    private static CifradorAESGCM staticCifrador;

    @Autowired
    public void setCifrador(CifradorAESGCM cifrador) {
        CondicionVisualConverter.staticCifrador = cifrador;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank() || staticCifrador == null) {
            return attribute;
        }
        return staticCifrador.cifrar(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || staticCifrador == null) {
            return dbData;
        }
        return staticCifrador.descifrar(dbData);
    }
}
