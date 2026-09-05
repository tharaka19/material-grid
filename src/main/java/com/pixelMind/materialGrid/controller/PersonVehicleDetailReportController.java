package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceipt;
import com.pixelMind.materialGrid.service.PersonVehicleDetailPdfService;
import com.pixelMind.materialGrid.service.PersonVehicleDetailReportService;
import com.pixelMind.materialGrid.util.PdfFileNameUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Deliberately thin, mirroring DailyRouteReportController exactly: request
 * in, delegate to the report service for data + consolidation, delegate to
 * the PDF service for rendering, attach the right Content-Disposition,
 * return bytes.
 */
@Tag(name = "Person Vehicle Detail Reports", description = "Read-only PDF receipt for a single person's vehicle assignments across a date range")
@RestController
@RequestMapping("/api/v1/person-vehicle-details/report")
@RequiredArgsConstructor
public class PersonVehicleDetailReportController {

    private final PersonVehicleDetailReportService personVehicleDetailReportService;
    private final PersonVehicleDetailPdfService personVehicleDetailPdfService;

    @Operation(summary = "Preview the Person Vehicle Details receipt PDF inline (personId + startDate + endDate)")
    @GetMapping("/preview")
    public ResponseEntity<byte[]> preview(
            @RequestParam Long personId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return buildPdfResponse(personId, startDate, endDate, ContentDisposition.inline());
    }

    @Operation(summary = "Download the Person Vehicle Details receipt PDF as an attachment (personId + startDate + endDate)")
    @GetMapping("/download")
    public ResponseEntity<byte[]> download(
            @RequestParam Long personId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return buildPdfResponse(personId, startDate, endDate, ContentDisposition.attachment());
    }

    private ResponseEntity<byte[]> buildPdfResponse(Long personId, LocalDate startDate, LocalDate endDate,
                                                    ContentDisposition.Builder dispositionBuilder) {
        PersonVehicleDetailReceipt receipt = personVehicleDetailReportService.generateReceipt(personId, startDate, endDate);
        byte[] pdfBytes = personVehicleDetailPdfService.generatePdf(receipt);

        String fileName = PdfFileNameUtil.buildPersonVehicleDetailFileName(
                receipt.getPersonCode(), receipt.getStartDate(), receipt.getEndDate());
        ContentDisposition disposition = dispositionBuilder.filename(fileName).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentType(MediaType.APPLICATION_PDF);

        return ResponseEntity.ok().headers(headers).body(pdfBytes);
    }
}