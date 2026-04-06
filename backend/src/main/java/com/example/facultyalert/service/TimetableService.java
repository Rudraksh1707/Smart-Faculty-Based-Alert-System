package com.example.facultyalert.service;

import com.example.facultyalert.model.*;
import com.example.facultyalert.repository.AbsenceLogRepository;
import com.example.facultyalert.repository.FacultyRepository;
import com.example.facultyalert.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableRepository timetableRepository;
    private final FacultyRepository facultyRepository;
    private final AbsenceLogRepository absenceLogRepository;
    private final EmailService emailService;

    public List<Timetable> getAllTimetables() {
        return timetableRepository.findAll();
    }

    public Timetable saveTimetable(Timetable timetable) {
        if (timetable.getFaculty() != null && timetable.getDate() != null && timetable.getStartTime() != null) {
            List<Timetable> existing = timetableRepository.findByFacultyIdAndDate(timetable.getFaculty().getId(), timetable.getDate());
            for (Timetable existingT : existing) {
                if (existingT.getStatus() != TimetableStatus.CANCELLED && 
                    timetable.getStartTime().equals(existingT.getStartTime()) &&
                    (timetable.getId() == null || !existingT.getId().equals(timetable.getId()))) {
                    throw new RuntimeException("Conflict: " + timetable.getFaculty().getName() + " is already teaching another class at " + timetable.getStartTime() + "!");
                }
            }
        }
        return timetableRepository.save(timetable);
    }

    public void saveAll(List<Timetable> timetables) {
        timetableRepository.saveAll(timetables);
    }
    
    public void deleteTimetable(Long id) {
        timetableRepository.deleteById(id);
    }

    @Transactional
    public void deleteAll() {
        timetableRepository.deleteAll();
    }

    public void updateStatus(Long id, com.example.facultyalert.model.TimetableStatus status) {
        Timetable t = timetableRepository.findById(id).orElseThrow(() -> new RuntimeException("Timetable not found"));
        t.setStatus(status);
        timetableRepository.save(t);
    }

    @Transactional
    public void markFacultyAbsent(Long facultyId, LocalDate date) {
        Faculty absentFaculty = facultyRepository.findById(facultyId)
                .orElseThrow(() -> new RuntimeException("Faculty not found"));

        if (absenceLogRepository.existsByFacultyIdAndAbsentDate(facultyId, date)) {
            throw new RuntimeException("Faculty is already marked absent for this date");
        }

        // Log the absence
        AbsenceLog log = new AbsenceLog();
        log.setFaculty(absentFaculty);
        log.setAbsentDate(date);
        absenceLogRepository.save(log);

        // Find all scheduled classes for this faculty on this date
        List<Timetable> todayClasses = timetableRepository.findByFacultyIdAndDate(facultyId, date);

        for (Timetable t : todayClasses) {
            if (t.getStatus() == TimetableStatus.SCHEDULED) {
                // Find a substitute
                List<Faculty> available = facultyRepository.findAvailableFaculty(
                        absentFaculty.getDepartment(), date, t.getStartTime());
                
                if (!available.isEmpty()) {
                    Faculty substitute = available.get(0); // Pick the first available
                    
                    t.setOriginalFaculty(absentFaculty);
                    t.setFaculty(substitute);
                    t.setStatus(TimetableStatus.SUBSTITUTED);
                    timetableRepository.save(t);
                    
                    // Alert the substitute
                    emailService.sendEmail(
                            substitute.getEmail(),
                            "Emergency Substitution Alert",
                            "Hello " + substitute.getName() + ",\n\n" +
                            "You have been assigned as a substitute for an upcoming class due to the absence of " + absentFaculty.getName() + ".\n\n" +
                            "Subject: " + t.getSubject() + "\n" +
                            "Date: " + t.getDate() + "\n" +
                            "Time: " + t.getStartTime() + " to " + t.getEndTime() + "\n\n" +
                            "Please make sure to attend the lecture."
                    );
                } else {
                    t.setStatus(TimetableStatus.CANCELLED);
                    timetableRepository.save(t);
                    // Could notify admin that no substitute was found
                }
            }
        }
    }
}
