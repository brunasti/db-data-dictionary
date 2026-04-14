package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.UserDto;
import it.brunasti.dbdadi.exception.DuplicateResourceException;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.User;
import it.brunasti.dbdadi.model.enums.UserRole;
import it.brunasti.dbdadi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository repository;
    private final BCryptPasswordEncoder passwordEncoder;

    public List<UserDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public UserDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public Optional<User> findRawByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Transactional
    public UserDto create(UserDto dto) {
        if (repository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("User already exists: " + dto.getUsername());
        }
        if (dto.getPassword() == null || dto.getPassword().isBlank()) {
            throw new IllegalArgumentException("Password is required when creating a user");
        }
        User user = User.builder()
                .username(dto.getUsername())
                .passwordHash(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .enabled(dto.isEnabled())
                .build();
        return toDto(repository.save(user));
    }

    @Transactional
    public UserDto update(Long id, UserDto dto) {
        User existing = getOrThrow(id);
        // Check username uniqueness if it changed
        if (!existing.getUsername().equals(dto.getUsername())
                && repository.existsByUsername(dto.getUsername())) {
            throw new DuplicateResourceException("Username already taken: " + dto.getUsername());
        }
        existing.setUsername(dto.getUsername());
        existing.setRole(dto.getRole());
        existing.setEnabled(dto.isEnabled());
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            existing.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        }
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    public boolean validateCredentials(String username, String rawPassword) {
        return repository.findByUsername(username)
                .filter(User::isEnabled)
                .map(u -> passwordEncoder.matches(rawPassword, u.getPasswordHash()))
                .orElse(false);
    }

    private User getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private UserDto toDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .username(u.getUsername())
                .role(u.getRole())
                .enabled(u.isEnabled())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
