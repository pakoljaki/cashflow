package com.akosgyongyosi.cashflow;

import com.akosgyongyosi.cashflow.entity.Role;
import com.akosgyongyosi.cashflow.entity.User;
import com.akosgyongyosi.cashflow.repository.UserRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
public class CashflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashflowApplication.class, args);
    }

	/**
     * On application startup, if there are no users in the database,
     * create a default admin with email "admin@admin", password "admin",
     * and roles ADMIN, USER.
     */
    @Bean
    public CommandLineRunner initializeAdminUser(UserRepository userRepository,
                                                 PasswordEncoder passwordEncoder) {
        return args -> {
            long count = userRepository.count();
            if (count == 0) {
                User admin = new User();
                admin.setEmail("admin@admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                Set<Role> roles = new HashSet<>(Arrays.asList(
                    Role.ADMIN,
                    Role.USER
                ));
                admin.setRoles(roles);
                userRepository.save(admin);
                log.info("Default admin created: {} with roles {}", admin.getEmail(), roles);
            } else {
                log.info("{} existing users found; skipping default admin creation", count);
            }
        };
    }
	
}
