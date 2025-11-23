package com.saasbeauty.authservice.components; // Asegúrate que sea tu paquete correcto

import com.saasbeauty.authservice.entities.UserRole; // Importa UserRole de tu paquete de entidades
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap; // Para los claims
import java.util.Map; // Para los claims
import java.util.Set; // Para los roles
import java.util.function.Function;
import java.util.stream.Collectors; // Para procesar los roles

@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMillis;

    // Constructor que inyecta el secreto y la expiración desde application.properties
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expirationMs}") long expirationMillis) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalArgumentException("La clave JWT debe tener mínimo 32 caracteres y no ser nula");
        }
        // Crea la clave segura para firmar y verificar (usando HS256)
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMillis = expirationMillis;
    }

    // --- GENERACIÓN DE TOKEN (MODIFICADO) ---

    /**
     * Genera un nuevo AccessToken para el usuario, incluyendo roles como claims.
     * @param username El nombre de usuario (subject del token).
     * @param roles El conjunto de UserRole del usuario.
     * @return El token JWT como una cadena.
     */
    public String generateToken(String username, Set<UserRole> roles) {
        Map<String, Object> claims = new HashMap<>();
        // Extraer los códigos de rol y añadirlos como una lista al claim "roles"
        if (roles != null && !roles.isEmpty()) {
            claims.put("roles", roles.stream()
                    // Asegura que no haya nulos intermedios
                    .filter(userRole -> userRole.getRole() != null && userRole.getRole().getCode() != null)
                    .map(userRole -> userRole.getRole().getCode())
                    .collect(Collectors.toList()));
        }
        // Aquí podrías añadir otros claims si los necesitaras en el futuro
        // ej., claims.put("userId", user.getId());

        return createToken(claims, username);
    }

    /**
     * Método helper para construir el token JWT con los claims y el subject dados.
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationMillis);

        return Jwts.builder()
                .setClaims(claims) // Añade los claims personalizados (ej. roles)
                .setSubject(subject) // Establece el username
                .setIssuedAt(now) // Fecha de emisión
                .setExpiration(expiryDate) // Fecha de expiración
                .signWith(key, SignatureAlgorithm.HS256) // Firma con la clave y algoritmo
                .compact();
    }

    // --- VALIDACIÓN Y EXTRACCIÓN DE TOKEN ---

    /**
     * Extrae TODOS los claims (información) desde un token JWT.
     * Valida la firma en el proceso.
     * @param token El token JWT.
     * @return Un objeto Claims que contiene toda la información del token.
     * @throws JwtException Si el token es inválido o ha sido manipulado.
     */
    public Claims extractAllClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(key) // Especifica la clave para verificar la firma
                .build()
                .parseClaimsJws(token) // Verifica firma y decodifica
                .getBody(); // Obtiene el payload (claims)
    }

    /**
     * Extrae un claim específico del token usando una función resolver.
     * @param token El token JWT.
     * @param claimsResolver Función para extraer el claim deseado (ej. Claims::getSubject).
     * @return El valor del claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae el nombre de usuario (subject) desde un token JWT.
     * @param token El token JWT.
     * @return El nombre de usuario.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrae la fecha de expiración desde un token JWT.
     * @param token El token JWT.
     * @return La fecha de expiración.
     */
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Verifica si un token JWT ha expirado.
     * @param token El token JWT.
     * @return true si el token ha expirado, false en caso contrario.
     */
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (JwtException e) {
            // Si no se puede extraer la expiración, el token es inválido de todos modos
            return true;
        }
    }

    /**
     * Valida si un token es correcto (firma válida Y no expirado).
     * @param token El token JWT a validar.
     * @return true si el token es válido, false en caso contrario (firma inválida, expirado, malformado).
     */
    public boolean validateToken(String token) {
        try {
            // extractAllClaims ya valida la firma. Solo necesitamos chequear la expiración.
            extractAllClaims(token);
            return !isTokenExpired(token);
        } catch (JwtException | IllegalArgumentException e) {
            // Loggear el error sería bueno aquí para depuración
            // log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}