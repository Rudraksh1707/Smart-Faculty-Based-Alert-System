package com.example.facultyalert.service;

import com.example.facultyalert.model.Timetable;
import com.example.facultyalert.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AlertScheduler {

    private final TimetableRepository timetableRepository;
    private final EmailService emailService;
    private final TelegramService telegramService;

    // Run every minute
    @Scheduled(fixedRate = 60000)
    public void sendLectureAlerts() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);
        
        LocalTime inTen = now.plusMinutes(10);
        LocalTime inFive = now.plusMinutes(5);
        
        System.out.println("Scheduler running... Checking precisely for " + inTen + " and " + inFive);

        // 10 MINUTE WARNING
        List<Timetable> tenMinClasses = timetableRepository.findUpcomingClassesExactTime(today, inTen);
        for (Timetable t : tenMinClasses) {
            System.out.println("Sending 10-min alert for " + t.getSubject());
            emailService.sendEmail(
                    t.getFaculty().getEmail(),
                    "⏰ 10-Min Warning: " + t.getSubject(),
                    "Hello " + t.getFaculty().getName() + ",\n\n" +
                    "Your lecture for " + t.getSubject() + " will begin in exactly 10 minutes.\n\n" +
                    "Time: " + t.getStartTime() + " to " + t.getEndTime() + "\n" +
                    "Please prepare to start the class."
            );

            // TELEGRAM DISPATCH
            if (t.getFaculty().getTelegramChatId() != null) {
                telegramService.sendMessage(
                    t.getFaculty().getTelegramChatId(),
                    "*⏰ 10-Min Warning*\n\nYour lecture for *" + t.getSubject() + "* starts in exactly 10 minutes.\n\nTime: `" + t.getStartTime() + " - " + t.getEndTime() + "`"
                );
            }
        }

        // 5 MINUTE WARNING
        List<Timetable> fiveMinClasses = timetableRepository.findUpcomingClassesExactTime(today, inFive);
        for (Timetable t : fiveMinClasses) {
            System.out.println("Sending 5-min alert for " + t.getSubject());
            emailService.sendEmail(
                    t.getFaculty().getEmail(),
                    "🚨 5-Min Urgent Warning: " + t.getSubject(),
                    "Hello " + t.getFaculty().getName() + ",\n\n" +
                    "This is your final 5-minute reminder. Your lecture for " + t.getSubject() + " is starting very soon.\n\n" +
                    "Time: " + t.getStartTime() + " to " + t.getEndTime() + "\n" +
                    "Please proceed to your class."
            );

            // TELEGRAM DISPATCH
            if (t.getFaculty().getTelegramChatId() != null) {
                telegramService.sendMessage(
                    t.getFaculty().getTelegramChatId(),
                    "*🚨 5-Min Urgent Warning*\n\nYour lecture *" + t.getSubject() + "* is starting very soon!\n\nTime: `" + t.getStartTime() + " - " + t.getEndTime() + "`"
                );
            }
        }
    }
}
