package com.booking.service.service;

import com.booking.service.dto.UserCreateRequest;
import com.booking.service.dto.UserDTO;
import com.booking.service.dto.UserUpdateRequest;
import com.booking.service.entity.User;
import com.booking.service.exception.ResourceAlreadyExistsException;
import com.booking.service.exception.ResourceNotFoundException;
import com.booking.service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private UserCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .password("encodedPassword")
                .role(User.Role.USER)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = UserCreateRequest.builder()
                .username("newuser")
                .password("password123")
                .role(User.Role.USER)
                .build();
    }

    @Test
    void createUser_WithValidRequest_ShouldReturnUserDTO() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User user = i.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(LocalDateTime.now());
            return user;
        });

        // Act
        UserDTO result = userService.createUser(createRequest);

        // Assert
        assertNotNull(result);
        assertEquals("newuser", result.getUsername());
        assertEquals(User.Role.USER, result.getRole());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void createUser_WithExistingUsername_ShouldThrowException() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        ResourceAlreadyExistsException exception = assertThrows(
                ResourceAlreadyExistsException.class,
                () -> userService.createUser(createRequest)
        );
        assertTrue(exception.getMessage().contains("already exists"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_WithValidRequest_ShouldUpdateUser() {
        // Arrange
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .id(1L)
                .username("updateduser")
                .password("newpassword")
                .role(User.Role.ADMIN)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("updateduser")).thenReturn(false);
        when(passwordEncoder.encode("newpassword")).thenReturn("newEncodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        UserDTO result = userService.updateUser(updateRequest);

        // Assert
        assertNotNull(result);
        assertEquals("updateduser", result.getUsername());
        assertEquals(User.Role.ADMIN, result.getRole());
        verify(userRepository).save(testUser);
    }

    @Test
    void updateUser_WithNonExistentId_ShouldThrowException() {
        // Arrange
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .id(999L)
                .build();

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> userService.updateUser(updateRequest)
        );
    }

    @Test
    void updateUser_WithExistingUsername_ShouldThrowException() {
        // Arrange
        UserUpdateRequest updateRequest = UserUpdateRequest.builder()
                .id(1L)
                .username("existinguser")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // Act & Assert
        assertThrows(ResourceAlreadyExistsException.class,
                () -> userService.updateUser(updateRequest)
        );
    }

    @Test
    void deleteUser_WithValidId_ShouldDeleteUser() {
        // Arrange
        when(userRepository.existsById(1L)).thenReturn(true);

        // Act
        userService.deleteUser(1L);

        // Assert
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteUser_WithNonExistentId_ShouldThrowException() {
        // Arrange
        when(userRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> userService.deleteUser(999L)
        );
        verify(userRepository, never()).deleteById(any());
    }

    @Test
    void getUserByUsername_WithValidUsername_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.getUserByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void getUserByUsername_WithNonExistentUsername_ShouldThrowException() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> userService.getUserByUsername("nonexistent")
        );
    }
}
