package com.sneaky.sneaky.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sneaky.sneaky.dto.CreateUserRequestDTO;
import com.sneaky.sneaky.dto.LoginRequestDTO;
import com.sneaky.sneaky.dto.LoginResponseDTO;
import com.sneaky.sneaky.dto.UpdateUserRequestDTO;
import com.sneaky.sneaky.dto.UserDTO;
import com.sneaky.sneaky.security.CurrentUser;
import com.sneaky.sneaky.services.AuthService;
import com.sneaky.sneaky.services.UserService;

class UserControllerTest {

        private final UserService userService = org.mockito.Mockito.mock(UserService.class);
        private final AuthService authService = org.mockito.Mockito.mock(AuthService.class);
        private final CurrentUser currentUser = org.mockito.Mockito.mock(CurrentUser.class);
        private final ObjectMapper objectMapper = new ObjectMapper();
        private MockMvc mockMvc;

        @BeforeEach
        void setUp() {
                LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
                validator.afterPropertiesSet();
                mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService, authService, currentUser))
                                .setValidator(validator)
                                .build();
        }

        @Test
        void getUsersReturnsUsers() throws Exception {
                UUID userId = UUID.randomUUID();
                when(userService.getAllUsers())
                                .thenReturn(List.of(new UserDTO(userId, "Ari", "ari@example.com", false)));

                mockMvc.perform(get("/api/users"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].userId").value(userId.toString()))
                                .andExpect(jsonPath("$[0].name").value("Ari"))
                                .andExpect(jsonPath("$[0].email").value("ari@example.com"))
                                .andExpect(jsonPath("$[0].isGuest").value(false));
        }

        @Test
        void createUserCreatesAndLogsInUser() throws Exception {
                CreateUserRequestDTO request = new CreateUserRequestDTO();
                request.setName("Ari");
                request.setEmail("ari@example.com");
                request.setPassword("secret123");
                request.setIsGuest(false);

                when(authService.authenticate(any(LoginRequestDTO.class)))
                                .thenReturn(new LoginResponseDTO("access", "refresh"));

                mockMvc.perform(post("/api/users")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access"))
                                .andExpect(jsonPath("$.refreshToken").value("refresh"));

                verify(userService).createUser(any(CreateUserRequestDTO.class));
                verify(authService).authenticate(any(LoginRequestDTO.class));
        }

        @Test
        void createUserRejectsInvalidPayloadBeforeServiceCalls() throws Exception {
                CreateUserRequestDTO request = new CreateUserRequestDTO();
                request.setName("");
                request.setEmail("bad-email");
                request.setPassword("123");

                mockMvc.perform(post("/api/users")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isBadRequest());

                verifyNoInteractions(userService, authService);
        }

        @Test
        void getCurrentUserUsesCurrentUserId() throws Exception {
                UUID userId = UUID.randomUUID();
                when(currentUser.getUserId()).thenReturn(userId);
                when(userService.getUserById(userId))
                                .thenReturn(new UserDTO(userId, "Ari", "ari@example.com", false));

                mockMvc.perform(get("/api/users/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.email").value("ari@example.com"));
        }

        @Test
        void putPatchAndDeleteCurrentUserUseCurrentUserId() throws Exception {
                UUID userId = UUID.randomUUID();
                UpdateUserRequestDTO request = new UpdateUserRequestDTO();
                request.setName("Ari Updated");

                when(currentUser.getUserId()).thenReturn(userId);
                when(userService.updateUserById(eq(userId), any(UpdateUserRequestDTO.class)))
                                .thenReturn(new UserDTO(userId, "Ari Updated", "ari@example.com", false));
                when(userService.patchUserById(eq(userId), any(UpdateUserRequestDTO.class)))
                                .thenReturn(new UserDTO(userId, "Ari Updated", "ari@example.com", false));

                mockMvc.perform(put("/api/users/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Ari Updated"));

                mockMvc.perform(patch("/api/users/me")
                                .contentType("application/json")
                                .content(objectMapper.writeValueAsString(request)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.name").value("Ari Updated"));

                mockMvc.perform(delete("/api/users/me"))
                                .andExpect(status().isNoContent());

                verify(userService).deleteUserById(userId);
        }
}
