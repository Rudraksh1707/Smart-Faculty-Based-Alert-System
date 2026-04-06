package com.example.facultyalert.controller;

import com.example.facultyalert.model.Faculty;
import com.example.facultyalert.service.FacultyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faculty")
@RequiredArgsConstructor
public class FacultyController {
    
    private final FacultyService facultyService;
    private final com.example.facultyalert.repository.FacultyRepository facultyRepository;

    @GetMapping
    public ResponseEntity<List<Faculty>> getAllFaculty() {
        return ResponseEntity.ok(facultyService.getAllFaculty());
    }

    @PostMapping
    public ResponseEntity<?> addFaculty(@RequestBody Faculty faculty) {
        try {
            return ResponseEntity.ok(facultyService.saveFaculty(faculty));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving faculty: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFaculty(@PathVariable Long id) {
        try {
            facultyService.deleteFaculty(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting faculty");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFaculty(@PathVariable Long id, @RequestBody Faculty faculty) {
        try {
            return ResponseEntity.ok(facultyService.updateFaculty(id, faculty));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating faculty: " + e.getMessage());
        }
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody java.util.Map<String, String> payload) {
        try {
            Long facultyId = Long.parseLong(payload.get("facultyId"));
            String newPassword = payload.get("newPassword");
            facultyService.changePassword(facultyId, newPassword);
            return ResponseEntity.ok("Password updated successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error changing password: " + e.getMessage());
        }
    }

    @PostMapping("/link-telegram")
    public ResponseEntity<?> linkTelegram(@RequestBody java.util.Map<String, String> payload) {
        try {
            Long facultyId = Long.parseLong(payload.get("facultyId"));
            String chatId = payload.get("chatId");
            Faculty faculty = facultyRepository.findById(facultyId).orElseThrow();
            faculty.setTelegramChatId(chatId);
            facultyRepository.save(faculty);
            return ResponseEntity.ok("Telegram linked successfully");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error linking Telegram: " + e.getMessage());
        }
    }
}
