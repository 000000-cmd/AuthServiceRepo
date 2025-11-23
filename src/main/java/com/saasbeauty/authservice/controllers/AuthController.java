package com.saasbeauty.authservice.controllers; // Ajusta tu paquete

import com.saasbeauty.authservice.dto.request.LoginRequest; // Ajusta tus paquetes DTO
import com.saasbeauty.authservice.dto.response.LoginResponseDTO;
import com.saasbeauty.authservice.dto.response.UserResponseDTO; // Necesario para /me simplificado
import com.saasbeauty.authservice.entities.RefreshToken;
import com.saasbeauty.authservice.entities.User;
import com.saasbeauty.authservice.exceptions.ResourceNotFoundException;
import com.saasbeauty.authservice.mappers.UserMapper; // Ajusta tu paquete mappers
import com.saasbeauty.authservice.repositories.UserRepository; // Ajusta tu paquete repositorios
import com.saasbeauty.authservice.services.AuthService;
import com.saasbeauty.authservice.services.RefreshTokenService;
import com.saasbeauty.authservice.components.JwtUtil; // Necesario para /refresh

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties; // Para /apiV
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors; // Para mapear roles en /me

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil; // Necesario para /refresh
    private final UserRepository userRepository; // Necesario para /me
    private final UserMapper userMapper; // Necesario para /me
    private final BuildProperties buildProperties; // Para /apiV (asegúrate que el bean exista)

    @Autowired // Buena práctica en constructor
    public AuthController(AuthService authService,
                          RefreshTokenService refreshTokenService,
                          JwtUtil jwtUtil,
                          UserRepository userRepository,
                          UserMapper userMapper,
                          BuildProperties buildProperties) {
        this.authService = authService;
        this.refreshTokenService = refreshTokenService;
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.buildProperties = buildProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        // La lógica compleja (validar, llamar a thirdparty-service, generar tokens y cookie)
        // ahora está DENTRO de authService.login()
        LoginResponseDTO responseDTO = authService.login(loginRequest.getUsernameOrEmail(), loginRequest.getPassword(), response);
        return ResponseEntity.ok(responseDTO);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        // Esta lógica es correcta: busca la cookie, valida el refresh token y emite un nuevo access token
        if (request.getCookies() == null) {
            return ResponseEntity.status(401).body(Map.of("message", "No se encontró la cookie de refresco"));
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> "refreshToken".equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue)
                .flatMap(refreshTokenService::findByToken) // Busca el token en BD
                .map(refreshTokenService::verifyExpiration) // Verifica si ha expirado
                .map(RefreshToken::getUser) // Obtiene el usuario asociado
                .map(user -> {
                    // Genera SÓLO el nuevo AccessToken
                    String newAccessToken = jwtUtil.generateToken(user.getUsername(), user.getRoles()); // Asume que generateToken acepta roles
                    return ResponseEntity.ok(Map.of("accessToken", newAccessToken)); // Devuelve el nuevo token
                })
                .orElse(ResponseEntity.status(401).body(Map.of("message", "Token de refresco inválido o expirado")));
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@AuthenticationPrincipal UserDetails userDetails) {
        // Devuelve información básica del usuario autenticado (sin llamar a thirdparty-service por simplicidad)
        if (userDetails == null) {
            return ResponseEntity.status(401).build(); // No autenticado
        }

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado para el token principal: " + userDetails.getUsername()));

        // Construimos una respuesta simple aquí o usamos un DTO específico para /me
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("cellular", user.getCellular());
        userInfo.put("attachment", user.getAttachment()); // Devuelve la URL si existe
        if (user.getRoles() != null) {
            userInfo.put("roles", user.getRoles().stream()
                    .map(role -> role.getRole().getCode())
                    .collect(Collectors.toList()));
        } else {
            userInfo.put("roles", java.util.Collections.emptyList());
        }


        return ResponseEntity.ok(userInfo);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        // 1. Intenta invalidar el refresh token en la BD si existe la cookie
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .filter(cookie -> "refreshToken".equals(cookie.getName()))
                    .findFirst()
                    .ifPresent(cookie -> refreshTokenService.deleteByToken(cookie.getValue()));
        }

        // 2. Expira la cookie en el navegador
        Cookie cookie = new Cookie("refreshToken", null); // Crea una cookie con el mismo nombre
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // Le dice al navegador que la borre inmediatamente
        // cookie.setSecure(true); // Debería ser true en producción (HTTPS)
        response.addCookie(cookie);

        return ResponseEntity.ok(Map.of("message", "Logout exitoso"));
    }

    @GetMapping("/apiV")
    public Map<String, String> getApiVersion() {
        Map<String, String> response = new HashMap<>();
        // El bean BuildProperties es opcional, si no lo configuras, esto podría fallar
        response.put("version", buildProperties != null ? buildProperties.getVersion() : "N/A");
        return response;
    }
}