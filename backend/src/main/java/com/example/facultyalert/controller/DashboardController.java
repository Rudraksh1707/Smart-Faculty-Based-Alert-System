package com.example.facultyalert.controller;

import com.example.facultyalert.model.TimetableStatus;
import com.example.facultyalert.repository.FacultyRepository;
import com.example.facultyalert.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final FacultyRepository facultyRepository;
    private final TimetableRepository timetableRepository;

    @GetMapping("/stats")
    public ResponseEntity<?> getLiveStats() {
        LocalDate today = LocalDate.now();
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalFaculty", facultyRepository.count());
        stats.put("totalClassesToday", timetableRepository.countByDate(today));
        stats.put("totalClasses", timetableRepository.count());
        stats.put("totalSubstitutedToday", timetableRepository.countByDateAndStatus(today, TimetableStatus.SUBSTITUTED));
        stats.put("todayTimeline", timetableRepository.findByDateOrderByStartTimeAsc(today));
        
        return ResponseEntity.ok(stats);
    }
}
