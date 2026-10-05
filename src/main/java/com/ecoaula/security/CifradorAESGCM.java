package com.ecoaula.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Componente centralizado de cifrado simétrico AES-GCM (ADR-09).
 * Garantiza confidencialidad e integridad para datos sensibles (condición visual, Ley 1581/2012).
 */
@Component
public class CifradorAESGCM {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int LONGITUD_TAG_BITS = 128;
    private static final int LONGITUD_IV_BYTES = 12;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public CifradorAESGCM(@Value("${ecoaula.seguridad.clave-cifrado:ecoaula-clave-secreta-aes-256-ok!}") String claveRaw) {
        this.secretKey = derivarClave256(claveRaw);
    }

    /**
     * Cifra un texto en claro utilizando AES-256-GCM con un IV aleatorio.
     * Retorna el IV concatenado con el texto cifrado y codificado en Base64.
     */
    public String cifrar(String textoPlano) {
        if (textoPlano == null || textoPlano.isBlank()) {
            return null;
        }
        try {
            byte[] iv = new byte[LONGITUD_IV_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(LONGITUD_TAG_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] cipherText = cipher.doFinal(textoPlano.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + cipherText.length);
            byteBuffer.put(iv);
            byteBuffer.put(cipherText);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("Error al cifrar dato sensible con AES-GCM", e);
        }
    }

    /**
     * Descifra un texto codificado en Base64 extrayendo su IV y validando la autenticación GCM.
     */
    public String descifrar(String textoCifradoBase64) {
        if (textoCifradoBase64 == null || textoCifradoBase64.isBlank()) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(textoCifradoBase64);
            if (decoded.length < LONGITUD_IV_BYTES) {
                throw new IllegalArgumentException("Dato cifrado inválido o corrupto");
            }

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[LONGITUD_IV_BYTES];
            byteBuffer.get(iv);

            byte[] cipherText = new byte[byteBuffer.remaining()];
            byteBuffer.get(cipherText);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(LONGITUD_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] plainTextBytes = cipher.doFinal(cipherText);
            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Error al descifrar dato sensible con AES-GCM", e);
        }
    }

    private SecretKey derivarClave256(String claveRaw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(claveRaw.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algoritmo SHA-256 no disponible en la JVM", e);
        }
    }
}
