package com.example.facultyalert.controller;

import com.example.facultyalert.model.Admin;
import com.example.facultyalert.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Admin admin) {
        try {
            Admin registered = authService.register(admin);
            return ResponseEntity.ok(registered);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Admin admin) {
        Optional<Admin> loggedIn = authService.login(admin.getUsername(), admin.getPassword());
        if (loggedIn.isPresent()) {
            return ResponseEntity.ok(loggedIn.get());
        }
        return ResponseEntity.status(401).body("Invalid credentials");
    }

    @PostMapping("/faculty-login")
    public ResponseEntity<?> facultyLogin(@RequestBody java.util.Map<String, String> payload) {
        String email = payload.get("email");
        String password = payload.get("password");
        Optional<com.example.facultyalert.model.Faculty> loggedIn = authService.loginFaculty(email, password);
        if (loggedIn.isPresent()) {
            return ResponseEntity.ok(loggedIn.get());
        }
        return ResponseEntity.status(401).body("Invalid faculty credentials");
    }
}
