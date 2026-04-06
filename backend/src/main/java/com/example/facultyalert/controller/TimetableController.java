package com.example.facultyalert.controller;

import com.example.facultyalert.model.Timetable;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/timetable")
@RequiredArgsConstructor
public class TimetableController {

    private final com.example.facultyalert.service.TimetableService timetableService;
    private final com.example.facultyalert.service.BulkUploadService bulkUploadService;

    @GetMapping
    public ResponseEntity<List<Timetable>> getAllTimetables() {
        return ResponseEntity.ok(timetableService.getAllTimetables());
    }

    @PostMapping("/upload")
    public ResponseEntity<?> bulkUpload(@RequestParam("file") org.springframework.web.multipart.MultipartFile file,
                                        @RequestParam("startDate") java.time.LocalDate startDate,
                                        @RequestParam("endDate") java.time.LocalDate endDate,
                                        @RequestParam(value = "facultyId", required = false) Long facultyId) {
        try {
            int count = bulkUploadService.processUpload(file, startDate, endDate, facultyId);
            return ResponseEntity.ok("Successfully generated " + count + " classes.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> addTimetable(@RequestBody Timetable timetable) {
        try {
            if (timetable.getStatus() == null) {
                timetable.setStatus(com.example.facultyalert.model.TimetableStatus.SCHEDULED);
            }
            return ResponseEntity.ok(timetableService.saveTimetable(timetable));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error saving timetable: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTimetable(@PathVariable Long id) {
        try {
            timetableService.deleteTimetable(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error deleting timetable");
        }
    }

    @DeleteMapping("/all")
    public ResponseEntity<?> deleteAllTimetables() {
        try {
            timetableService.deleteAll();
            return ResponseEntity.ok("All timetable records wiped successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error clearing timetable: " + e.getMessage());
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateTimetableStatus(@PathVariable Long id, @RequestBody java.util.Map<String, String> payload) {
        try {
            com.example.facultyalert.model.TimetableStatus status = com.example.facultyalert.model.TimetableStatus.valueOf(payload.get("status"));
            timetableService.updateStatus(id, status);
            return ResponseEntity.ok("Status updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error updating status: " + e.getMessage());
        }
    }

    @PostMapping("/mark-absent")
    public ResponseEntity<?> markFacultyAbsent(@RequestBody AbsentRequest request) {
        try {
            timetableService.markFacultyAbsent(request.getFacultyId(), request.getDate());
            return ResponseEntity.ok("Successfully marked absent and re-allocated " + 
                    "substitutes where possible.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @Data
    public static class AbsentRequest {
        private Long facultyId;
        private LocalDate date;
    }
}
