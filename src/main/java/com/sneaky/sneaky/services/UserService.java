package com.sneaky.sneaky.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.sneaky.sneaky.dto.CreateUserRequest;
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

    public UserDTO createUser(CreateUserRequest request) {

        Users user = new Users();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
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
