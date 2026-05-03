package com.sneaky.sneaky.services;

import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.CreateUserRequestDTO;
import com.sneaky.sneaky.dto.UserDTO;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.UsersRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
     private final UsersRepository userRepository;
     private final PasswordEncoder passwordEncoder;

    public List<UserDTO> getAllUsers() {
        List<Users> users = userRepository.findAll();

        return users.stream()
                .map(user -> new UserDTO(
                        user.getUserId(),
                        user.getName(),
                        user.getEmail(),
                        user.getIsGuest()
                ))
                .toList();
    }

    public UserDTO createUser(CreateUserRequestDTO request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        Users user = new Users();
        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsGuest(request.getIsGuest());

        Users saved = userRepository.save(user);

        return new UserDTO(
                saved.getUserId(),
                saved.getName(),
                saved.getEmail(),
                saved.getIsGuest()
        );
    }
}
