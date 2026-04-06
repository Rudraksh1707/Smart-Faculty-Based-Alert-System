package com.example.facultyalert.repository;

import com.example.facultyalert.model.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TimetableRepository extends JpaRepository<Timetable, Long> {
    List<Timetable> findByFacultyId(Long facultyId);
    List<Timetable> findByDate(LocalDate date);
    
    @Query("SELECT t FROM Timetable t WHERE t.date = :date AND t.startTime BETWEEN :start AND :end AND t.status != 'CANCELLED'")
    List<Timetable> findUpcomingClasses(@Param("date") LocalDate date, 
                                        @Param("start") LocalTime start, 
                                        @Param("end") LocalTime end);

    @Query("SELECT t FROM Timetable t WHERE t.date = :date AND t.startTime = :time AND t.status != 'CANCELLED'")
    List<Timetable> findUpcomingClassesExactTime(@Param("date") LocalDate date, 
                                                 @Param("time") LocalTime time);

    List<Timetable> findByFacultyIdAndDate(Long facultyId, LocalDate date);

    long countByDate(LocalDate date);
    long countByDateAndStatus(LocalDate date, com.example.facultyalert.model.TimetableStatus status);
    List<Timetable> findByDateOrderByStartTimeAsc(LocalDate date);

    boolean existsByFacultyIdAndDateAndStartTime(Long facultyId, LocalDate date, java.time.LocalTime startTime);

    void deleteAllByDateBetween(LocalDate start, LocalDate end);
}
