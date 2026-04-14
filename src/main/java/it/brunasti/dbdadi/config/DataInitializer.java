package it.brunasti.dbdadi.config;

import it.brunasti.dbdadi.dto.UserDto;
import it.brunasti.dbdadi.model.enums.UserRole;
import it.brunasti.dbdadi.repository.UserRepository;
import it.brunasti.dbdadi.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.count() == 0) {
            log.info("No users found — creating default admin user (admin / admin)");
            userService.create(UserDto.builder()
                    .username("admin")
                    .password("admin")
                    .role(UserRole.ADMIN)
                    .enabled(true)
                    .build());
            log.warn("Default admin created with password 'admin' — please change it immediately!");
        }
    }
}
