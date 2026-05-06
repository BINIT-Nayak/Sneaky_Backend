package com.sneaky.sneaky.controllers;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import com.sneaky.sneaky.dto.auth.LoginRequestDTO;
import com.sneaky.sneaky.dto.auth.LoginResponseDTO;
import com.sneaky.sneaky.dto.user.*;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.AuthService;
import com.sneaky.sneaky.services.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AuthService authService;
    private final CurrentUser currentUser;

    @GetMapping
    public List<UserDTO> getUsers() {
        return userService.getAllUsers();
    }

    @PostMapping
    public LoginResponseDTO createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        userService.createUser(request);

        LoginRequestDTO loginRequest = new LoginRequestDTO();
        loginRequest.setEmail(request.getEmail());
        loginRequest.setPassword(request.getPassword());

        return authService.authenticate(loginRequest);
    }

    @GetMapping("/me")
    public UserDTO getCurrentUser() {
        return userService.getUserById(currentUser.getUserId());
    }

    @PutMapping("/me")
    public UserDTO updateCurrentUser(@Valid @RequestBody UpdateUserRequestDTO request) {
        return userService.updateUserById(currentUser.getUserId(), request);
    }

    @PatchMapping("/me")
    public UserDTO patchCurrentUser(@RequestBody UpdateUserRequestDTO request) {
        return userService.patchUserById(currentUser.getUserId(), request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCurrentUser() {
        userService.deleteUserById(currentUser.getUserId());
    }
}
