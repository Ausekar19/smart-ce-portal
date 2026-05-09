package com.fergusson.ceportal.service;

import com.fergusson.ceportal.model.ExamTest;
import com.fergusson.ceportal.model.TestAttempt;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelExportService {

    private final ExamService examService;

    public byte[] exportTestReport(ExamTest test) throws IOException {
        List<TestAttempt> attempts = examService.getAttemptsForTest(test);

        try (Workbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Results");

            // ── Header style ───────────────────────────────────────────────
            CellStyle headerStyle = wb.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = wb.createFont();
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            // ── Header row ─────────────────────────────────────────────────
            Row header = sheet.createRow(0);
            String[] cols = {"#", "Student Name", "PRN", "Score",
                    "Correct", "Wrong", "Unattempted",
                    "Tab Switches", "Auto Submitted", "Start Time", "End Time"};
            for (int i = 0; i < cols.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(cols[i]);
                cell.setCellStyle(headerStyle);
            }

            // ── Data rows ──────────────────────────────────────────────────
            int rowNum = 1;
            for (TestAttempt a : attempts) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rowNum - 1);
                row.createCell(1).setCellValue(a.getStudent().getFullName());
                row.createCell(2).setCellValue(a.getStudent().getUsername());
                row.createCell(3).setCellValue(a.getScore());
                row.createCell(4).setCellValue(a.getCorrectCount());
                row.createCell(5).setCellValue(a.getWrongCount());
                row.createCell(6).setCellValue(a.getUnattemptedCount());
                row.createCell(7).setCellValue(a.getTabSwitchCount());
                row.createCell(8).setCellValue(a.isAutoSubmitted() ? "YES" : "NO");
                row.createCell(9).setCellValue(a.getStartTime() != null ? a.getStartTime().toString() : "");
                row.createCell(10).setCellValue(a.getEndTime() != null ? a.getEndTime().toString() : "");
            }

            for (int i = 0; i < cols.length; i++) sheet.autoSizeColumn(i);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
