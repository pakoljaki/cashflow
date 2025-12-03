package com.akosgyongyosi.cashflow;

import com.akosgyongyosi.cashflow.entity.Role;
import com.akosgyongyosi.cashflow.entity.User;
import com.akosgyongyosi.cashflow.repository.UserRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class CashflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashflowApplication.class, args);
    }


    @Bean
    public CommandLineRunner initializeAdminUser(UserRepository userRepository,
                                                 PasswordEncoder passwordEncoder) {
        return args -> {
            long count = userRepository.count();
            if (count == 0) {
                User admin = new User();
                admin.setEmail("admin@admin");
                admin.setPassword(passwordEncoder.encode("admin"));
                admin.setRole(Role.ADMIN);
                userRepository.save(admin);
                log.info("Default admin created: {} with role {}", admin.getEmail(), Role.ADMIN);
            } else {
                log.info("{} existing users found; skipping default admin creation", count);
            }
        };
    }
	
}
