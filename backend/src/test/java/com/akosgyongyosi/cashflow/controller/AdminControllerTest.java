package com.akosgyongyosi.cashflow.controller;

import com.akosgyongyosi.cashflow.controller.admin.AdminController;
import com.akosgyongyosi.cashflow.entity.User;
import com.akosgyongyosi.cashflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllUsers_shouldReturnAllUsers() {
        User user1 = new User();
        user1.setId(1L);
        user1.setEmail("user1@example.com");

        User user2 = new User();
        user2.setId(2L);
        user2.setEmail("user2@example.com");

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        List<User> result = adminController.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("user1@example.com");
        assertThat(result.get(1).getEmail()).isEqualTo("user2@example.com");
        verify(userRepository).findAll();
    }

    @Test
    void getAllUsers_shouldReturnEmptyListWhenNoUsers() {
        when(userRepository.findAll()).thenReturn(Arrays.asList());

        List<User> result = adminController.getAllUsers();

        assertThat(result).isEmpty();
        verify(userRepository).findAll();
    }
}
