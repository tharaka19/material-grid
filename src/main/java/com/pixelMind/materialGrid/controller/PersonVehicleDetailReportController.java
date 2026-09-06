package com.pixelMind.materialGrid.controller;

import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceipt;
import com.pixelMind.materialGrid.service.PersonVehicleDetailExcelService;
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
 * MODIFIED: added Excel preview/download endpoints alongside the existing
 * PDF pair. Both formats call the SAME
 * PersonVehicleDetailReportService.generateReceipt(...) - there is no
 * separate Excel data path, per this feature's explicit reuse requirement.
 * Still deliberately thin - no business logic, no database access, no
 * POI/PDF-library code here.
 */
@Tag(name = "Person Vehicle Detail Reports", description = "Read-only PDF/Excel receipt for a single person's vehicle assignments across a date range")
@RestController
@RequestMapping("/api/v1/person-vehicle-details/report")
@RequiredArgsConstructor
public class PersonVehicleDetailReportController {

    private static final MediaType XLSX_MEDIA_TYPE =
            MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

    private final PersonVehicleDetailReportService personVehicleDetailReportService;
    private final PersonVehicleDetailPdfService personVehicleDetailPdfService;
    private final PersonVehicleDetailExcelService personVehicleDetailExcelService;

    @Operation(summary = "Preview the Person Vehicle Details receipt PDF inline (personId + startDate + endDate)")
    @GetMapping("/preview")
    public ResponseEntity<byte[]> previewPdf(
            @RequestParam Long personId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PersonVehicleDetailReceipt receipt = personVehicleDetailReportService.generateReceipt(personId, startDate, endDate);
        byte[] pdfBytes = personVehicleDetailPdfService.generatePdf(receipt);
        String fileName = PdfFileNameUtil.buildPersonVehicleDetailFileName(
                receipt.getPersonCode(), receipt.getStartDate(), receipt.getEndDate());
        return buildResponse(pdfBytes, MediaType.APPLICATION_PDF, fileName, ContentDisposition.inline());
    }

    @Operation(summary = "Download the Person Vehicle Details receipt PDF as an attachment (personId + startDate + endDate)")
    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadPdf(
            @RequestParam Long personId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PersonVehicleDetailReceipt receipt = personVehicleDetailReportService.generateReceipt(personId, startDate, endDate);
        byte[] pdfBytes = personVehicleDetailPdfService.generatePdf(receipt);
        String fileName = PdfFileNameUtil.buildPersonVehicleDetailFileName(
                receipt.getPersonCode(), receipt.getStartDate(), receipt.getEndDate());
        return buildResponse(pdfBytes, MediaType.APPLICATION_PDF, fileName, ContentDisposition.attachment());
    }

    @Operation(summary = "Preview the Person Vehicle Details receipt as Excel (personId + startDate + endDate). Same data/validation/consolidation as the PDF.")
    @GetMapping("/excel/preview")
    public ResponseEntity<byte[]> previewExcel(
            @RequestParam Long personId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PersonVehicleDetailReceipt receipt = personVehicleDetailReportService.generateReceipt(personId, startDate, endDate);
        byte[] excelBytes = personVehicleDetailExcelService.generateExcel(receipt);
        String fileName = PdfFileNameUtil.buildPersonVehicleDetailFileName(
                receipt.getPersonCode(), receipt.getStartDate(), receipt.getEndDate(), "xlsx");
        return buildResponse(excelBytes, XLSX_MEDIA_TYPE, fileName, ContentDisposition.inline());
    }

    @Operation(summary = "Download the Person Vehicle Details receipt as an Excel attachment (personId + startDate + endDate). Same data/validation/consolidation as the PDF.")
    @GetMapping("/excel/download")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam Long personId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        PersonVehicleDetailReceipt receipt = personVehicleDetailReportService.generateReceipt(personId, startDate, endDate);
        byte[] excelBytes = personVehicleDetailExcelService.generateExcel(receipt);
        String fileName = PdfFileNameUtil.buildPersonVehicleDetailFileName(
                receipt.getPersonCode(), receipt.getStartDate(), receipt.getEndDate(), "xlsx");
        return buildResponse(excelBytes, XLSX_MEDIA_TYPE, fileName, ContentDisposition.attachment());
    }

    private ResponseEntity<byte[]> buildResponse(byte[] bytes, MediaType mediaType, String fileName,
                                                 ContentDisposition.Builder dispositionBuilder) {
        ContentDisposition disposition = dispositionBuilder.filename(fileName).build();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(disposition);
        headers.setContentType(mediaType);
        return ResponseEntity.ok().headers(headers).body(bytes);
    }
}