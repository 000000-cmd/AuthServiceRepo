package com.saasbeauty.authservice.controllers;

import com.saasbeauty.authservice.dto.request.user.EmbeddedUserRequestDTO;
import com.saasbeauty.authservice.dto.response.UserResponseDTO;
import com.saasbeauty.authservice.services.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody EmbeddedUserRequestDTO userRequestDTO) {
        // Permite crear un usuario de forma independiente
        UserResponseDTO createdUser = userService.createUser(userRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }
}

