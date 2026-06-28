package com.skrepta.skreptajava.config;

import com.skrepta.skreptajava.auth.entity.User;
import com.skrepta.skreptajava.auth.repository.UserRepository;
import com.skrepta.skreptajava.location.entity.City;
import com.skrepta.skreptajava.location.repository.CityRepository;
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
    private final CityRepository cityRepository;

    @Override
    public void run(String... args) throws Exception {
        final String adminEmail = "SkreptaAdmin@skrepta.com";
        final String adminPassword = "SkreptaTopProject123!";

        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            City defaultCity = resolveDefaultCity();

            User admin = User.builder()
                    .email(adminEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .fio("Главный Администратор Skrepta")
                    .phoneNumber("77777777777")
                    .city(defaultCity)
                    .role(User.Role.ADMIN)
                    .createdAt(Instant.now())
                    .build();

            userRepository.save(admin);
            log.info("--- Initial ADMIN user created: {} ---", adminEmail);
        } else {
            log.info("--- Initial ADMIN user already exists: {} ---", adminEmail);
        }
    }

    /**
     * Пытаемся найти Алматы как дефолтный город для системных аккаунтов.
     * Если таблица cities пустая (сидинг ещё не прогнан) — берём первый
     * попавшийся город, а если городов вообще нет — оставляем null,
     * чтобы не упасть на старте приложения.
     */
    private City resolveDefaultCity() {
        return cityRepository.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase("Алматы"))
                .findFirst()
                .or(() -> cityRepository.findAll().stream().findFirst())
                .orElseGet(() -> {
                    log.warn("No cities found in DB — admin user will be created without a city. " +
                            "Run city seeding before relying on city-based features.");
                    return null;
                });
    }
}