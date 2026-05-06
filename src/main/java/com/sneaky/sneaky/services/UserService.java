package com.sneaky.sneaky.services;

import java.util.List;
import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.CreateUserRequestDTO;
import com.sneaky.sneaky.dto.UpdateUserRequestDTO;
import com.sneaky.sneaky.dto.UserDTO;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.UsersRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService {
    private final UsersRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public UserDTO toDTO(Users user) {
        return new UserDTO(
                user.getUserId(),
                user.getName(),
                user.getEmail(),
                user.getIsGuest());
    }

    public List<UserDTO> getAllUsers() {
        List<Users> users = userRepository.findAll();

        return users.stream()
                .map(this::toDTO)
                .toList();
    }

    public Users createUser(CreateUserRequestDTO request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);

        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User already exists");
        }

        Users user = new Users();
        user.setName(request.getName().trim());
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setIsGuest(request.getIsGuest());

        return userRepository.save(user);
    }

    public Users getCurrentUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    public UserDTO getUserById(UUID userId) {
        Users user = getCurrentUser(userId);
        return toDTO(user);
    }

    public UserDTO updateUserById(UUID userId, UpdateUserRequestDTO request) {

        Users user = getCurrentUser(userId);

        user.setName(request.getName());
        if (request.getEmail() != null) {
            String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            }
            user.setEmail(normalizedEmail);
        }
        user.setIsGuest(request.getIsGuest());

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toDTO(userRepository.save(user));
    }

    public UserDTO patchUserById(UUID userId, UpdateUserRequestDTO request) {

        Users user = getCurrentUser(userId);

        if (request.getName() != null)
            user.setName(request.getName());
        if (request.getEmail() != null) {
            String normalizedEmail = request.getEmail().trim().toLowerCase(Locale.ROOT);
            if (!normalizedEmail.equals(user.getEmail()) && userRepository.existsByEmail(normalizedEmail)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
            }
            user.setEmail(normalizedEmail);
        }
        if (request.getIsGuest() != null)
            user.setIsGuest(request.getIsGuest());

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return toDTO(userRepository.save(user));
    }

    public void deleteUserById(UUID userId) {
        redisTemplate.opsForValue().set(
                "auth:logout:" + userId,
                String.valueOf(System.currentTimeMillis()),
                Duration.ofDays(7));

        Users user = getCurrentUser(userId);
        userRepository.delete(user);
    }
}
