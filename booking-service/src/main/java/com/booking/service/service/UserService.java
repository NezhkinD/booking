package com.booking.service.service;

import com.booking.service.dto.UserCreateRequest;
import com.booking.service.dto.UserDTO;
import com.booking.service.dto.UserUpdateRequest;
import com.booking.service.entity.User;
import com.booking.service.exception.ResourceAlreadyExistsException;
import com.booking.service.exception.ResourceNotFoundException;
import com.booking.service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDTO createUser(UserCreateRequest request) {
        log.debug("Creating user: {}", request.getUsername());

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new ResourceAlreadyExistsException("User already exists: " + request.getUsername());
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .build();

        user = userRepository.save(user);
        log.info("User created successfully: {}", user.getUsername());

        return mapToDTO(user);
    }

    @Transactional
    public UserDTO updateUser(UserUpdateRequest request) {
        log.debug("Updating user with ID: {}", request.getId());

        User user = userRepository.findById(request.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + request.getId()));

        if (request.getUsername() != null && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new ResourceAlreadyExistsException("Username already taken: " + request.getUsername());
            }
            user.setUsername(request.getUsername());
        }

        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }

        user = userRepository.save(user);
        log.info("User updated successfully: {}", user.getUsername());

        return mapToDTO(user);
    }

    @Transactional
    public void deleteUser(Long userId) {
        log.debug("Deleting user with ID: {}", userId);

        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with ID: " + userId);
        }

        userRepository.deleteById(userId);
        log.info("User deleted successfully with ID: {}", userId);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    public java.util.List<UserDTO> getAllUsers() {
        log.debug("Retrieving all users");
        java.util.List<User> users = userRepository.findAll();
        log.info("Retrieved {} users", users.size());
        return users.stream()
                .map(this::mapToDTO)
                .collect(java.util.stream.Collectors.toList());
    }

    private UserDTO mapToDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
