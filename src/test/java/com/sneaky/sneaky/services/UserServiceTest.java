package com.sneaky.sneaky.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.sneaky.sneaky.dto.CreateUserRequestDTO;
import com.sneaky.sneaky.dto.UserDTO;
import com.sneaky.sneaky.entity.Users;
import com.sneaky.sneaky.repository.UsersRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UsersRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private UserService userService;

    @Test
    void getAllUsersMapsUsersToDtos() {
        UUID userId = UUID.randomUUID();
        Users user = new Users();
        user.setUserId(userId);
        user.setName("Ari");
        user.setEmail("ari@example.com");
        user.setIsGuest(false);

        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserDTO> users = userService.getAllUsers();

        assertThat(users).singleElement().satisfies(dto -> {
            assertThat(dto.getUserId()).isEqualTo(userId);
            assertThat(dto.getName()).isEqualTo("Ari");
            assertThat(dto.getEmail()).isEqualTo("ari@example.com");
            assertThat(dto.getIsGuest()).isFalse();
        });
    }

    @Test
    void createUserEncodesPasswordAndReturnsSavedUser() {
        UUID savedId = UUID.randomUUID();
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setName("Mina");
        request.setEmail("mina@example.com");
        request.setPassword("secret123");
        request.setIsGuest(false);

        when(userRepository.existsByEmail("mina@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded-password");
        when(userRepository.save(any(Users.class))).thenAnswer(invocation -> {
            Users user = invocation.getArgument(0);
            user.setUserId(savedId);
            return user;
        });

        Users created = userService.createUser(request);

        ArgumentCaptor<Users> userCaptor = ArgumentCaptor.forClass(Users.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPassword()).isEqualTo("encoded-password");
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("mina@example.com");
        assertThat(created.getUserId()).isEqualTo(savedId);
        assertThat(created.getName()).isEqualTo("Mina");
        assertThat(created.getEmail()).isEqualTo("mina@example.com");
        assertThat(created.getIsGuest()).isFalse();
    }

    @Test
    void createUserRejectsDuplicateEmailBeforeSaving() {
        CreateUserRequestDTO request = new CreateUserRequestDTO();
        request.setName("Mina");
        request.setEmail("Mina@Example.com");
        request.setPassword("secret123");

        when(userRepository.existsByEmail("mina@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);

        verify(userRepository, never()).save(any(Users.class));
    }
}
