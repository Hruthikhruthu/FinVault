package com.finvault.report;

import com.finvault.common.security.SecurityUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private static final MediaType EXCEL = MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<StreamingResponseBody> pdf() {
        byte[] content = reportService.pdf(SecurityUtils.currentUserId());
        return download("finvault-report.pdf", MediaType.APPLICATION_PDF, content);
    }

    @GetMapping(value = "/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<StreamingResponseBody> excel() {
        byte[] content = reportService.excel(SecurityUtils.currentUserId());
        return download("finvault-report.xlsx", EXCEL, content);
    }

    private ResponseEntity<StreamingResponseBody> download(String fileName, MediaType mediaType, byte[] content) {
        StreamingResponseBody body = outputStream -> outputStream.write(content);
        return ResponseEntity.ok()
            .contentType(mediaType)
            .contentLength(content.length)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(fileName).build().toString())
            .body(body);
    }
}

