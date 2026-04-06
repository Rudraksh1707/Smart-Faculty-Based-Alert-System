package com.example.facultyalert.repository;

import com.example.facultyalert.model.Faculty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends JpaRepository<Faculty, Long> {
    Optional<Faculty> findByEmail(String email);
    Optional<Faculty> findByMobile(String mobile);
    Optional<Faculty> findByName(String name);

    @Query("SELECT f FROM Faculty f WHERE f.department = :department AND f.id NOT IN " +
           "(SELECT t.faculty.id FROM Timetable t WHERE t.date = :date AND " +
           "(t.startTime <= :time AND t.endTime > :time))")
    List<Faculty> findAvailableFaculty(@Param("department") String department, 
                                       @Param("date") LocalDate date, 
                                       @Param("time") LocalTime time);
}
