package com.example.facultyalert.service;

import com.example.facultyalert.model.Faculty;
import com.example.facultyalert.repository.FacultyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FacultyService {

    private final FacultyRepository facultyRepository;
    private final EmailService emailService;

    public List<Faculty> getAllFaculty() {
        return facultyRepository.findAll();
    }

    public Faculty getFacultyById(Long id) {
        return facultyRepository.findById(id).orElseThrow(() -> new RuntimeException("Faculty not found"));
    }

    public Faculty saveFaculty(Faculty faculty) {
        if (faculty.getPassword() == null || faculty.getPassword().trim().isEmpty()) {
            String randomPass = "Fac@" + (int)(Math.random() * 9000 + 1000) + "!";
            faculty.setPassword(randomPass);
            System.out.println("Generated password for " + faculty.getEmail() + ": " + randomPass);
            // Send email
            String body = "Hello " + faculty.getName() + ",\n\n" +
                          "Your HOD has added you to the Smart Faculty Alert System.\n\n" +
                          "Your auto-generated login credentials are:\n" +
                          "Email: " + faculty.getEmail() + "\n" +
                          "Password: " + randomPass + "\n\n" +
                          "Please log into the Faculty Portal and change your password immediately.";
            emailService.sendEmail(faculty.getEmail(), "Welcome to the Smart Faculty Portal", body);
        }
        return facultyRepository.save(faculty);
    }

    public void changePassword(Long facultyId, String newPassword) {
        Faculty faculty = getFacultyById(facultyId);
        faculty.setPassword(newPassword);
        facultyRepository.save(faculty);
    }
    
    public Faculty updateFaculty(Long id, Faculty updatedData) {
        Faculty existing = getFacultyById(id);
        existing.setName(updatedData.getName());
        existing.setEmail(updatedData.getEmail());
        existing.setMobile(updatedData.getMobile());
        existing.setDepartment(updatedData.getDepartment());
        existing.setSubject(updatedData.getSubject());
        return facultyRepository.save(existing);
    }

    public void deleteFaculty(Long id) {
        facultyRepository.deleteById(id);
    }
}
