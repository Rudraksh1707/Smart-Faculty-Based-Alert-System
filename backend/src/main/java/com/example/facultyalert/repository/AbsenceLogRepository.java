package com.example.facultyalert.repository;

import com.example.facultyalert.model.AbsenceLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceLogRepository extends JpaRepository<AbsenceLog, Long> {
    List<AbsenceLog> findByFacultyId(Long facultyId);
    boolean existsByFacultyIdAndAbsentDate(Long facultyId, LocalDate absentDate);
}
