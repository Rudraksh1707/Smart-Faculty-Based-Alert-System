package com.example.facultyalert.service;

import com.example.facultyalert.model.Admin;
import com.example.facultyalert.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import com.example.facultyalert.repository.FacultyRepository;
import com.example.facultyalert.model.Faculty;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminRepository adminRepository;
    private final FacultyRepository facultyRepository;

    public Admin register(Admin admin) {
        if (adminRepository.findByUsername(admin.getUsername()).isPresent()) {
            throw new RuntimeException("Username '" + admin.getUsername() + "' is already taken.");
        }
        if (adminRepository.findByEmail(admin.getEmail()).isPresent()) {
            throw new RuntimeException("Email '" + admin.getEmail() + "' is already registered.");
        }

        String pwd = admin.getPassword();
        if (pwd == null || pwd.length() < 8 || !pwd.matches(".*[A-Z].*") || !pwd.matches(".*[a-z].*") || !pwd.matches(".*\\d.*")) {
            throw new RuntimeException("Password must contain at least 8 characters, including a number, an uppercase letter, and a lowercase letter.");
        }

        return adminRepository.save(admin);
    }

    public Optional<Admin> login(String username, String password) {
        return adminRepository.findByUsername(username)
                .filter(admin -> admin.getPassword().equals(password));
    }
    
    public Optional<Faculty> loginFaculty(String email, String password) {
        return facultyRepository.findByEmail(email)
                .filter(faculty -> faculty.getPassword().equals(password));
    }
}
