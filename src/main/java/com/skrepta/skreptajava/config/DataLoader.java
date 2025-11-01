package com.skrepta.skreptajava.config;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataLoader implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        final String adminEmail = "SkreptaAdmin@skrepta.com";
        final String adminPassword = "SkreptaTopProject123!";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .fio("Главный Администратор Skrepta")
                    .phoneNumber("77777777777")
                    .city("Admin City")
                    .role(User.Role.ADMIN)
                    .createdAt(Instant.now())
                    .build();

            userRepository.save(admin);
            log.info("--- Initial ADMIN user created: {} ---", adminEmail);
        } else {
            log.info("--- Initial ADMIN user already exists: {} ---", adminEmail);
        }
    }
}
