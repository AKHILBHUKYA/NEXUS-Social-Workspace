package com.akhil.social.config;

import com.akhil.social.entity.User;
import com.akhil.social.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seed(UserRepository users, PasswordEncoder encoder) {
        return args -> {
            if (!users.existsByUsername("demo")) {
                User u = new User();
                u.setUsername("demo");
                u.setEmail("demo@nexus.local");
                u.setPasswordHash(encoder.encode("demo12345"));
                u.setDisplayName("NEXUS Demo");
                u.setRole("USER");
                users.save(u);
            }
        };
    }
}
