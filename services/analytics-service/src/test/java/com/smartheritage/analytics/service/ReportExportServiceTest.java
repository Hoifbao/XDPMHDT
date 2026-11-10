package com.smartheritage.analytics.service;

import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.smartheritage.analytics.dto.StatisticsResponse;
import com.smartheritage.analytics.service.ReportExportService.ReportHeader;
import com.smartheritage.analytics.service.ReportExportService.ReportSheet;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportExportServiceTest {

    private static final ReportHeader HEADER =
            new ReportHeader("01/11/2026 – 10/11/2026", "Văn Miếu – Quốc Tử Giám", "10/11/2026 20:00");

    private static StatisticsResponse sample() {
        return StatisticsResponse.builder()
                .totalViews(12).totalVisitors(4)
                .averageDurationSeconds(185.5)
                .averageRatingApproved(4.5).approvedFeedbackCount(2)
                .averageRatingAll(4.0).totalFeedbackCount(3)
                .topArtifacts(List.of(StatisticsResponse.TopArtifact.builder()
                        .artifactId(1L).artifactCode("VM01").artifactName("Bia Tiến sĩ")
                        .siteId(1L).siteName("Văn Miếu").views(7).build()))
                .languageDistribution(List.of(new StatisticsResponse.LanguageCount("vi", 3)))
                .dailyViews(List.of(new StatisticsResponse.DailyViews(LocalDate.of(2026, 11, 10), 12)))
                .recentFeedbacks(List.of(new StatisticsResponse.RecentFeedback(
                        5L, LocalDateTime.of(2026, 11, 10, 9, 30), "Bia Tiến sĩ", 5, null, "APPROVED")))
                .build();
    }

    @Test
    void excel_co4SheetVaSoLieuGiuKieuSo() throws Exception {
        List<ReportSheet> sheets = ReportExportService.buildSheets(sample());
        byte[] file = ReportExportService.toExcel(HEADER, sheets);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(file))) {
            assertEquals(4, workbook.getNumberOfSheets());
            assertEquals("Tổng quan", workbook.getSheetAt(0).getSheetName());
            assertEquals("Top hiện vật", workbook.getSheetAt(1).getSheetName());
            assertEquals("Ngôn ngữ", workbook.getSheetAt(2).getSheetName());
            assertEquals("Đánh giá", workbook.getSheetAt(3).getSheetName());

            // Dòng 0-3 là tiêu đề, 4 trống, 5 tên bảng, 6 tiêu đề cột, 7 = "Tổng khách"
            Sheet overview = workbook.getSheetAt(0);
            assertEquals("Tổng khách", overview.getRow(7).getCell(0).getStringCellValue());
            assertEquals(4, overview.getRow(7).getCell(1).getNumericCellValue());
            assertEquals(7, workbook.getSheetAt(1).getRow(7).getCell(4).getNumericCellValue());
        }
    }

    @Test
    void pdf_hienDungTiengVietCoDau() throws Exception {
        byte[] file = ReportExportService.toPdf(HEADER, ReportExportService.buildSheets(sample()));

        PdfReader reader = new PdfReader(file);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        reader.close();
        assertTrue(text.contains("Báo cáo thống kê Smart Heritage"), text);
        assertTrue(text.contains("Văn Miếu – Quốc Tử Giám"), text);
        assertTrue(text.contains("Bia Tiến sĩ"), text);
        assertTrue(text.contains("Đã duyệt"), text);
    }

    @Test
    void khongCoDuLieu_vanTaoDuocFile() {
        StatisticsResponse empty = StatisticsResponse.builder()
                .topArtifacts(List.of()).languageDistribution(List.of())
                .dailyViews(List.of()).recentFeedbacks(List.of()).build();
        List<ReportSheet> sheets = ReportExportService.buildSheets(empty);

        assertTrue(ReportExportService.toExcel(HEADER, sheets).length > 0);
        assertTrue(ReportExportService.toPdf(HEADER, sheets).length > 0);
    }
}
