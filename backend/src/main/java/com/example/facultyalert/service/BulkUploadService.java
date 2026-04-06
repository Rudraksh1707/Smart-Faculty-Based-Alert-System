package com.example.facultyalert.service;

import com.example.facultyalert.model.Faculty;
import com.example.facultyalert.model.Timetable;
import com.example.facultyalert.model.TimetableStatus;
import com.example.facultyalert.repository.FacultyRepository;
import com.example.facultyalert.repository.TimetableRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class BulkUploadService {

    private final TimetableRepository timetableRepository;
    private final FacultyRepository facultyRepository;
    private final FacultyService facultyService;

    // Pattern for subject codes like CSIT-601, CSE-102, etc.
    private static final Pattern SUBJECT_CODE_PATTERN = Pattern.compile("(?i)[A-Z]{2,4}-\\d{3,4}");
    
    // Pattern for faculty titles
    private static final Pattern FACULTY_TITLE_PATTERN = Pattern.compile("(?i)^(prof\\.|dr\\.|mr\\.|mrs\\.?)\\s*");

    @Transactional
    public int processUpload(MultipartFile file, LocalDate startDate, LocalDate endDate, Long specificFacultyId) throws Exception {
        if (file.getOriginalFilename() == null || !file.getOriginalFilename().endsWith(".xlsx")) {
            throw new RuntimeException("Only .xlsx Excel files are supported.");
        }

        List<Faculty> allFacultyList = facultyRepository.findAll();
        Faculty specificFacMatched = (specificFacultyId != null) ? facultyRepository.findById(specificFacultyId).orElse(null) : null;
        List<Timetable> results = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            // 1. Map Merged Regions for accurate cell reading
            Map<String, String> mergedContentMap = new HashMap<>();
            for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
                CellRangeAddress region = sheet.getMergedRegion(i);
                Cell firstCell = sheet.getRow(region.getFirstRow()).getCell(region.getFirstColumn());
                String val = getCellString(firstCell);
                for (int r = region.getFirstRow(); r <= region.getLastRow(); r++) {
                    for (int c = region.getFirstColumn(); c <= region.getLastColumn(); c++) {
                        mergedContentMap.put(r + "," + c, val);
                    }
                }
            }

            // 2. Identify Header (Day/Time)
            Row headRow = null;
            int headIdx = -1;
            for (int i = 0; i <= Math.min(30, sheet.getLastRowNum()); i++) {
                Row r = sheet.getRow(i);
                if (r != null && r.getCell(0) != null) {
                    String val = getCellString(r.getCell(0)).toLowerCase();
                    if (val.contains("day") || val.contains("time") || val.contains("period")) {
                        headRow = r;
                        headIdx = i;
                        break;
                    }
                }
            }

            if (headRow == null) throw new RuntimeException("Header row (Day/Time) not found in first 30 rows.");

            // 3. Parse Time Slots
            List<TimeSlot> slotsList = new ArrayList<>();
            Pattern p = Pattern.compile("(\\d{1,2}:\\d{2})\\s*(?:to|[-—])\\s*(\\d{1,2}:\\d{2})", Pattern.CASE_INSENSITIVE);
            for (int i = 1; i < headRow.getLastCellNum(); i++) {
                String cellVal = getCellString(headRow.getCell(i));
                Matcher m = p.matcher(cellVal);
                if (m.find()) {
                    slotsList.add(new TimeSlot(parseTime(m.group(1)), parseTime(m.group(2))));
                } else {
                    slotsList.add(null);
                }
            }

            // 4. Grid Processing with Multi-Row support
            DayOfWeek currentDay = null;
            Map<Integer, List<String>> colBuffers = new HashMap<>();

            for (int r = headIdx + 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String dayText = getCellString(row.getCell(0)).trim().toUpperCase();
                DayOfWeek rowDay = parseDay(dayText);

                if (rowDay != null) {
                    if (currentDay != null && currentDay != rowDay) {
                        flushGrid(results, colBuffers, currentDay, slotsList, allFacultyList, specificFacMatched, startDate, endDate);
                        colBuffers.clear();
                    }
                    currentDay = rowDay;
                }

                if (currentDay == null) continue;

                // Accumulate row columns into the current day buffer
                for (int c = 1; c <= slotsList.size(); c++) {
                    String val = mergedContentMap.getOrDefault(r + "," + c, getCellString(row.getCell(c)));
                    if (isSignificant(val)) {
                        colBuffers.computeIfAbsent(c - 1, k -> new ArrayList<>()).add(val);
                    }
                }
            }
            // Final flush for the last day in the sheet
            flushGrid(results, colBuffers, currentDay, slotsList, allFacultyList, specificFacMatched, startDate, endDate);

            if (results.isEmpty()) {
                throw new RuntimeException("0 classes generated. Ensure Column A has Day names and your headers match the '9:00 - 10:00' format.");
            }
        }

        timetableRepository.saveAll(results);
        return results.size();
    }

    private void flushGrid(List<Timetable> results, Map<Integer, List<String>> buffers, DayOfWeek day, 
                           List<TimeSlot> slots, List<Faculty> allFac, Faculty specFac, 
                           LocalDate start, LocalDate end) {
        if (day == null || buffers.isEmpty()) return;

        for (Map.Entry<Integer, List<String>> entry : buffers.entrySet()) {
            int idx = entry.getKey();
            if (idx >= slots.size() || slots.get(idx) == null) continue;

            // Deduplicate lines in multi-row/merged cells
            List<String> rawLines = entry.getValue();
            Set<String> uniqueLines = new LinkedHashSet<>(rawLines);
            String content = String.join("\n", uniqueLines).trim();
            
            if (isBreak(content)) continue;

            Faculty matched = null;
            String subjectFinal = "";

            if (specFac != null) {
                matched = specFac;
                subjectFinal = cleanSubject(content, null);
            } else {
                // --- Advanced Subject/Faculty Separation ---
                String[] lines = content.split("\n");
                String detectedName = null;
                List<String> subjectParts = new ArrayList<>();

                for (String line : lines) {
                    String t = line.trim();
                    if (t.isEmpty()) continue;

                    // 1. Rule: Subject Codes (CSIT-601) are NEVER faculty names
                    if (SUBJECT_CODE_PATTERN.matcher(t).find()) {
                        subjectParts.add(t);
                        continue;
                    }

                    // 2. Rule: Check if line starts with Prof./Dr./etc.
                    if (FACULTY_TITLE_PATTERN.matcher(t).find()) {
                        if (detectedName == null) detectedName = t.replaceFirst("(?i)^(prof\\.|dr\\.|mr\\.|mrs\\.?)\\s*", "").trim();
                        continue;
                    }

                    // 3. Rule: Check if line exactly matches a known Faculty name
                    final String finalT = t;
                    Faculty inDb = allFac.stream()
                        .filter(f -> f.getName().equalsIgnoreCase(finalT) || f.getName().toLowerCase().contains(finalT.toLowerCase()))
                        .findFirst().orElse(null);
                    
                    if (inDb != null && detectedName == null) {
                        detectedName = t;
                        continue;
                    }

                    // 4. Rule: Heuristic - if line has (Name) in parentheses
                    if (t.contains("(") && t.contains(")")) {
                        Matcher m = Pattern.compile("\\(([^)]+)\\)").matcher(t);
                        if (m.find() && detectedName == null) {
                            detectedName = m.group(1).trim();
                            continue;
                        }
                    }

                    // Otherwise, it's part of the subject
                    subjectParts.add(t);
                }

                subjectFinal = String.join(" ", subjectParts).trim();
                
                // Final Guess: If name still null and we have a very short line left at the end
                if (detectedName == null && lines.length > 1) {
                    String last = lines[lines.length - 1].trim();
                    if (last.length() < 25 && !SUBJECT_CODE_PATTERN.matcher(last).find()) {
                        detectedName = last;
                        // remove from subject if it was added
                        subjectFinal = subjectFinal.replace(last, "").trim();
                    }
                }

                if (detectedName != null) {
                    final String finalN = detectedName;
                    matched = allFac.stream()
                        .filter(f -> f.getName().toLowerCase().contains(finalN.toLowerCase()))
                        .findFirst().orElse(null);
                        
                    if (matched == null) {
                        Faculty nf = new Faculty(); 
                        nf.setName(detectedName); nf.setDepartment("CSIT");
                        nf.setEmail(detectedName.replaceAll("[^a-zA-Z]", "").toLowerCase() + "@college.edu");
                        nf.setSubject(subjectFinal); // New faculty gets the subject from Excel
                        matched = facultyService.saveFaculty(nf);
                        allFac.add(matched);
                    } else if (matched.getSubject() != null && !matched.getSubject().isEmpty()) {
                        // CRITICAL: Overwrite Excel subject with the OFFICIAL one from DB
                        subjectFinal = matched.getSubject();
                    }
                }
            }

            if (subjectFinal.isEmpty()) subjectFinal = "Class";

            // Failsafe for Faculty
            if (matched == null) {
                String subForName = subjectFinal.replaceAll("[^a-zA-Z0-9]", "");
                final String tbdName = "Unassigned_" + (subForName.length() > 10 ? subForName.substring(0, 10) : subForName);
                matched = facultyRepository.findByName(tbdName).orElseGet(() -> {
                    Faculty nf = new Faculty(); nf.setName(tbdName); nf.setDepartment("TBD");
                    nf.setEmail(tbdName.toLowerCase() + "@college.edu");
                    Faculty saved = facultyService.saveFaculty(nf);
                    allFac.add(saved);
                    return saved;
                });
            }

            // Create Instances
            LocalDate currDate = start;
            while (!currDate.isAfter(end)) {
                if (currDate.getDayOfWeek() == day) {
                    if (!timetableRepository.existsByFacultyIdAndDateAndStartTime(matched.getId(), currDate, slots.get(idx).start)) {
                        Timetable t = new Timetable();
                        t.setFaculty(matched); t.setSubject(subjectFinal); t.setDate(currDate);
                        t.setStartTime(slots.get(idx).start); t.setEndTime(slots.get(idx).end);
                        t.setStatus(TimetableStatus.SCHEDULED);
                        results.add(t);
                    }
                }
                currDate = currDate.plusDays(1);
            }
        }
    }

    private String cleanSubject(String content, String nameToRemove) {
        if (nameToRemove == null) return content.replaceAll("\\n", " ").trim();
        return content.replace(nameToRemove, "").replaceAll("\\n", " ").trim();
    }

    private boolean isSignificant(String s) {
        if (s == null || s.trim().isEmpty()) return false;
        String low = s.toLowerCase().trim();
        return !low.equals("-") && !low.equals(".") && !isBreak(s);
    }

    private boolean isBreak(String s) {
        if (s == null) return false;
        String low = s.toLowerCase().trim();
        return low.contains("lunch") || low.contains("break") || low.contains("recess") || 
               low.contains("gap") || low.contains("tea") || low.equals("off") || low.equals("free");
    }

    private String getCellString(Cell c) {
        if (c == null) return "";
        if (c.getCellType() == CellType.STRING) return c.getStringCellValue().trim();
        return new DataFormatter().formatCellValue(c).trim();
    }

    private LocalTime parseTime(String t) {
        t = t.trim().replaceAll("\\s+", "");
        String[] p = t.split("[:.]");
        int h = Integer.parseInt(p[0]);
        int m = p.length > 1 ? Integer.parseInt(p[1].substring(0, 2)) : 0;
        
        // Simple 12 to 24h conversion (heuristic for college hours)
        if (h >= 1 && h <= 7) h += 12; 
        return LocalTime.of(h, m);
    }

    private DayOfWeek parseDay(String d) {
        if (d.startsWith("MON")) return DayOfWeek.MONDAY;
        if (d.startsWith("TUE")) return DayOfWeek.TUESDAY;
        if (d.startsWith("WED")) return DayOfWeek.WEDNESDAY;
        if (d.startsWith("THU")) return DayOfWeek.THURSDAY;
        if (d.startsWith("FRI")) return DayOfWeek.FRIDAY;
        if (d.startsWith("SAT")) return DayOfWeek.SATURDAY;
        return null;
    }

    private static class TimeSlot {
        LocalTime start, end;
        TimeSlot(LocalTime s, LocalTime e) { this.start = s; this.end = e; }
    }
}
