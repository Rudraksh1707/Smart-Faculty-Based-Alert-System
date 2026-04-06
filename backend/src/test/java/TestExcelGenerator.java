import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.FileOutputStream;

public class TestExcelGenerator {
    public static void main(String[] args) throws Exception {
        Workbook wb = new XSSFWorkbook();
        Sheet ws = wb.createSheet("Timetable");
        
        Row header = ws.createRow(0);
        header.createCell(0).setCellValue("Day");
        header.createCell(1).setCellValue("09:00 To 10:00");
        header.createCell(2).setCellValue("10:00 To 11:00");
        
        Row row1 = ws.createRow(1);
        row1.createCell(0).setCellValue("MON");
        row1.createCell(1).setCellValue("Java Basics (Dr. Jane)");
        row1.createCell(2).setCellValue("Physics (Other)");
        
        Row row2 = ws.createRow(2);
        row2.createCell(0).setCellValue("TUE");
        row2.createCell(1).setCellValue("Data Structures (Dr. Jane)");
        row2.createCell(2).setCellValue("");

        try (FileOutputStream out = new FileOutputStream("test_timetable.xlsx")) {
            wb.write(out);
        }
        wb.close();
        System.out.println("Excel file created!");
    }
}
