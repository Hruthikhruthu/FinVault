package com.finvault.report;

import com.finvault.common.domain.FinancialTransaction;
import com.finvault.common.domain.SchedulerAudit;
import com.finvault.common.domain.SchedulerStatus;
import com.finvault.common.repository.SchedulerAuditRepository;
import com.finvault.common.repository.TransactionRepository;
import com.finvault.common.repository.UserRepository;
import com.lowagie.text.Document;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SchedulerAuditRepository schedulerAuditRepository;

    public ReportService(TransactionRepository transactionRepository,
                         UserRepository userRepository,
                         SchedulerAuditRepository schedulerAuditRepository) {
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.schedulerAuditRepository = schedulerAuditRepository;
    }

    @Transactional(readOnly = true)
    public byte[] pdf(Long userId) {
        List<FinancialTransaction> transactions = transactionRepository.findByUserIdOrderByOccurredAtDesc(userId);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("FinVault Personal Finance Report", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK)));
            document.add(new Paragraph("Generated " + DateTimeFormatter.ISO_INSTANT.format(Instant.now())));
            document.add(new Paragraph(" "));
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            addHeader(table, "Date");
            addHeader(table, "Type");
            addHeader(table, "Merchant");
            addHeader(table, "Category");
            addHeader(table, "Amount");
            for (FinancialTransaction transaction : transactions) {
                table.addCell(transaction.getOccurredAt().toLocalDate().toString());
                table.addCell(transaction.getType().name());
                table.addCell(transaction.getMerchant());
                table.addCell(transaction.getCategory());
                table.addCell(transaction.getAmount().toPlainString());
            }
            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("FinVault - Real-Time Personal Finance Intelligence Platform"));
            document.close();
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate PDF report", ex);
        }
    }

    @Transactional(readOnly = true)
    public byte[] excel(Long userId) {
        List<FinancialTransaction> transactions = transactionRepository.findByUserIdOrderByOccurredAtDesc(userId);
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("FinVault Report");
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFont(headerFont);
            Row title = sheet.createRow(0);
            title.createCell(0).setCellValue("FinVault Personal Finance Report");
            Row header = sheet.createRow(2);
            String[] headers = {"Date", "Type", "Merchant", "Category", "Description", "Amount"};
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
                header.getCell(i).setCellStyle(headerStyle);
            }
            int rowIndex = 3;
            for (FinancialTransaction transaction : transactions) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(transaction.getOccurredAt().toString());
                row.createCell(1).setCellValue(transaction.getType().name());
                row.createCell(2).setCellValue(transaction.getMerchant());
                row.createCell(3).setCellValue(transaction.getCategory());
                row.createCell(4).setCellValue(transaction.getDescription());
                row.createCell(5).setCellValue(transaction.getAmount().doubleValue());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate Excel report", ex);
        }
    }

    @Scheduled(cron = "${app.reports.weekly-cron:0 0 8 ? * MON}")
    @Transactional
    public void scheduledWeeklyReports() {
        Instant started = Instant.now();
        SchedulerAudit audit = new SchedulerAudit();
        audit.setJobName("weekly-report-generation");
        audit.setStartedAt(started);
        try {
            long users = userRepository.count();
            audit.setStatus(SchedulerStatus.SUCCESS);
            audit.setMessage("Prepared weekly report job for " + users + " users");
        } catch (RuntimeException ex) {
            audit.setStatus(SchedulerStatus.FAILED);
            audit.setMessage(ex.getMessage());
        }
        audit.setCompletedAt(Instant.now());
        schedulerAuditRepository.save(audit);
    }

    private void addHeader(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
        cell.setBackgroundColor(new Color(225, 232, 240));
        table.addCell(cell);
    }
}

