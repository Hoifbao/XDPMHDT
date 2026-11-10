package com.smartheritage.analytics.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartheritage.analytics.dto.StatisticsResponse;
import com.smartheritage.analytics.repository.StatisticsRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Xuất báo cáo thống kê ra Excel và PDF (Tuần 6, Tuấn).
 * Số liệu lấy đúng từ StatisticsService.getOverview (cùng nguồn fact_* của NiFi với Dashboard),
 * nên số trong file khớp Dashboard với cùng bộ lọc. Không dùng bảng rt_* để tránh cộng chồng.
 * Cả 2 định dạng dựng từ cùng một danh sách ReportSheet nên nội dung giống nhau.
 */
@Service
@RequiredArgsConstructor
public class ReportExportService {

    /** Số hiện vật trong bảng top của báo cáo (Dashboard hiện 5, báo cáo hiện nhiều hơn) */
    static final int REPORT_TOP = 10;
    private static final String FONT_PATH = "/fonts/NotoSans-Regular.ttf";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Map<String, String> STATUS_LABEL =
            Map.of("PENDING", "Chờ duyệt", "APPROVED", "Đã duyệt", "REJECTED", "Từ chối");

    private final StatisticsService statisticsService;
    private final StatisticsRepository statisticsRepository;

    /** Một bảng trong báo cáo; rows chứa String hoặc Number (Excel giữ kiểu số để cộng, lọc được). */
    record ReportTable(String title, List<String> headers, List<List<Object>> rows) {
    }

    /** Một sheet Excel = một phần trong PDF */
    record ReportSheet(String name, List<ReportTable> tables) {
    }

    /** Thông tin đầu báo cáo */
    record ReportHeader(String period, String site, String exportedAt) {
    }

    public byte[] exportExcel(Long siteId, LocalDate from, LocalDate to) {
        StatisticsResponse data = statisticsService.getOverview(REPORT_TOP, siteId, from, to);
        return toExcel(header(siteId, from, to), buildSheets(data));
    }

    public byte[] exportPdf(Long siteId, LocalDate from, LocalDate to) {
        StatisticsResponse data = statisticsService.getOverview(REPORT_TOP, siteId, from, to);
        return toPdf(header(siteId, from, to), buildSheets(data));
    }

    private ReportHeader header(Long siteId, LocalDate from, LocalDate to) {
        String period = from == null && to == null ? "Toàn bộ thời gian"
                : (from == null ? "…" : from.format(DATE)) + " – " + (to == null ? "…" : to.format(DATE));
        String site = "Tất cả di tích";
        if (siteId != null) {
            String name = statisticsRepository.findSiteName(siteId);
            site = name != null ? name : "Di tích #" + siteId;
        }
        return new ReportHeader(period, site, LocalDateTime.now().format(DATE_TIME));
    }

    // 4 sheet theo task tuần 6: Tổng quan, Top hiện vật, Ngôn ngữ, Đánh giá
    static List<ReportSheet> buildSheets(StatisticsResponse d) {
        List<List<Object>> kpis = List.of(
                row("Tổng khách", d.getTotalVisitors()),
                row("Lượt tương tác", d.getTotalViews()),
                row("Thời gian dừng TB (giây)", orDash(d.getAverageDurationSeconds())),
                row("Điểm đánh giá TB (đã duyệt)", orDash(d.getAverageRatingApproved())),
                row("Số phản hồi", d.getTotalFeedbackCount()));
        List<List<Object>> daily = new ArrayList<>();
        d.getDailyViews().forEach(v -> daily.add(row(v.getDate().format(DATE), v.getViews())));

        List<List<Object>> top = new ArrayList<>();
        List<StatisticsResponse.TopArtifact> artifacts = d.getTopArtifacts();
        for (int i = 0; i < artifacts.size(); i++) {
            StatisticsResponse.TopArtifact a = artifacts.get(i);
            top.add(row(i + 1, orDash(a.getArtifactCode()), orDash(a.getArtifactName()), orDash(a.getSiteName()),
                    a.getViews()));
        }

        List<List<Object>> languages = new ArrayList<>();
        d.getLanguageDistribution().forEach(l -> languages.add(row(l.getLanguageCode(), l.getVisitors())));

        List<List<Object>> ratingSummary = List.of(
                row("Điểm TB (đã duyệt)", orDash(d.getAverageRatingApproved())),
                row("Số phản hồi đã duyệt", d.getApprovedFeedbackCount()),
                row("Điểm TB (mọi trạng thái)", orDash(d.getAverageRatingAll())),
                row("Tổng số phản hồi", d.getTotalFeedbackCount()));
        List<List<Object>> recent = new ArrayList<>();
        d.getRecentFeedbacks().forEach(f -> recent.add(row(
                f.getCreatedAt() == null ? "—" : f.getCreatedAt().format(DATE_TIME),
                orDash(f.getArtifactName()), f.getRating(),
                STATUS_LABEL.getOrDefault(f.getStatus(), String.valueOf(f.getStatus())))));

        return List.of(
                new ReportSheet("Tổng quan", List.of(
                        new ReportTable("Chỉ số chính", List.of("Chỉ số", "Giá trị"), kpis),
                        new ReportTable("Lượt tương tác theo ngày (tối đa 31 ngày có dữ liệu)",
                                List.of("Ngày", "Lượt"), daily))),
                new ReportSheet("Top hiện vật", List.of(new ReportTable("Hiện vật được tương tác nhiều nhất",
                        List.of("Hạng", "Mã", "Tên hiện vật", "Di tích", "Lượt"), top))),
                new ReportSheet("Ngôn ngữ", List.of(new ReportTable("Số khách theo ngôn ngữ",
                        List.of("Ngôn ngữ", "Số khách"), languages))),
                new ReportSheet("Đánh giá", List.of(
                        new ReportTable("Tổng hợp đánh giá", List.of("Chỉ số", "Giá trị"), ratingSummary),
                        new ReportTable("Phản hồi gần đây", List.of("Ngày", "Hiện vật", "Điểm", "Trạng thái"),
                                recent))));
    }

    static byte[] toExcel(ReportHeader header, List<ReportSheet> sheets) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFFont boldFont = workbook.createFont();
            boldFont.setBold(true);
            CellStyle bold = workbook.createCellStyle();
            bold.setFont(boldFont);

            for (ReportSheet reportSheet : sheets) {
                Sheet sheet = workbook.createSheet(reportSheet.name());
                int r = 0;
                Row title = sheet.createRow(r++);
                title.createCell(0).setCellValue("Báo cáo thống kê Smart Heritage – " + reportSheet.name());
                title.getCell(0).setCellStyle(bold);
                sheet.createRow(r++).createCell(0).setCellValue("Khoảng ngày: " + header.period());
                sheet.createRow(r++).createCell(0).setCellValue("Di tích: " + header.site());
                sheet.createRow(r++).createCell(0).setCellValue("Xuất lúc: " + header.exportedAt());
                int columns = 1;
                for (ReportTable table : reportSheet.tables()) {
                    r++;
                    Row tableTitle = sheet.createRow(r++);
                    tableTitle.createCell(0).setCellValue(table.title());
                    tableTitle.getCell(0).setCellStyle(bold);
                    Row head = sheet.createRow(r++);
                    for (int c = 0; c < table.headers().size(); c++) {
                        head.createCell(c).setCellValue(table.headers().get(c));
                        head.getCell(c).setCellStyle(bold);
                    }
                    if (table.rows().isEmpty()) {
                        sheet.createRow(r++).createCell(0).setCellValue("Không có dữ liệu");
                    }
                    for (List<Object> values : table.rows()) {
                        Row row = sheet.createRow(r++);
                        for (int c = 0; c < values.size(); c++) {
                            Object v = values.get(c);
                            if (v instanceof Number n) {
                                row.createCell(c).setCellValue(n.doubleValue());
                            } else {
                                row.createCell(c).setCellValue(String.valueOf(v));
                            }
                        }
                    }
                    columns = Math.max(columns, table.headers().size());
                }
                for (int c = 0; c < columns; c++) {
                    sheet.autoSizeColumn(c);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Không tạo được file Excel", e);
        }
    }

    static byte[] toPdf(ReportHeader header, List<ReportSheet> sheets) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(document, out);
            BaseFont base = loadVietnameseFont();
            Font titleFont = new Font(base, 16, Font.BOLD);
            Font sectionFont = new Font(base, 13, Font.BOLD);
            Font textFont = new Font(base, 10);
            Font headFont = new Font(base, 10, Font.BOLD);

            document.open();
            document.add(new Paragraph("Báo cáo thống kê Smart Heritage", titleFont));
            document.add(new Paragraph("Khoảng ngày: " + header.period(), textFont));
            document.add(new Paragraph("Di tích: " + header.site(), textFont));
            document.add(new Paragraph("Xuất lúc: " + header.exportedAt(), textFont));

            for (ReportSheet sheet : sheets) {
                Paragraph section = new Paragraph(sheet.name(), sectionFont);
                section.setSpacingBefore(14);
                document.add(section);
                for (ReportTable table : sheet.tables()) {
                    Paragraph tableTitle = new Paragraph(table.title(), headFont);
                    tableTitle.setSpacingBefore(6);
                    tableTitle.setSpacingAfter(4);
                    document.add(tableTitle);
                    document.add(pdfTable(table, headFont, textFont));
                }
            }
            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new IllegalStateException("Không tạo được file PDF", e);
        }
    }

    private static PdfPTable pdfTable(ReportTable table, Font headFont, Font textFont) {
        PdfPTable pdf = new PdfPTable(table.headers().size());
        pdf.setWidthPercentage(100);
        pdf.setHeaderRows(1);
        table.headers().forEach(h -> {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setGrayFill(0.9f);
            pdf.addCell(cell);
        });
        if (table.rows().isEmpty()) {
            PdfPCell empty = new PdfPCell(new Phrase("Không có dữ liệu", textFont));
            empty.setColspan(table.headers().size());
            pdf.addCell(empty);
        }
        for (List<Object> values : table.rows()) {
            for (Object v : values) {
                PdfPCell cell = new PdfPCell(new Phrase(formatPdf(v), textFont));
                if (v instanceof Number) {
                    cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
                }
                pdf.addCell(cell);
            }
        }
        return pdf;
    }

    // Font Noto Sans nhúng vào PDF (IDENTITY_H) để hiện đúng tiếng Việt có dấu trên mọi máy
    private static BaseFont loadVietnameseFont() {
        try (InputStream in = ReportExportService.class.getResourceAsStream(FONT_PATH)) {
            if (in == null) {
                throw new IllegalStateException("Thiếu font " + FONT_PATH + " để xuất PDF");
            }
            return BaseFont.createFont("NotoSans-Regular.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED,
                    true, in.readAllBytes(), null);
        } catch (IOException | DocumentException e) {
            throw new IllegalStateException("Không đọc được font " + FONT_PATH, e);
        }
    }

    // 4.0 -> "4", 4.33 -> "4.33"
    private static String formatPdf(Object v) {
        if (v instanceof Double d && d == Math.floor(d)) {
            return String.valueOf(d.longValue());
        }
        return String.valueOf(v);
    }

    private static Object orDash(Object value) {
        return value == null ? "—" : value;
    }

    private static List<Object> row(Object... values) {
        return List.of(values);
    }
}
