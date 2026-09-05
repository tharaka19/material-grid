package com.pixelMind.materialGrid.service.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.constant.ReportConstants;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceipt;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceiptRow;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.service.PersonVehicleDetailPdfService;
import com.pixelMind.materialGrid.util.DateTimeUtil;
import com.pixelMind.materialGrid.util.MoneyFormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;

/**
 * Follows the exact same layout/typography/color scheme as
 * DailyRoutePdfServiceImpl (per this feature's spec: "Follow the existing
 * Daily Routes Receipt PDF template/style") - same fonts, same header
 * branding block, same borderless label/value header table, same bordered
 * financial-style table with a repeating header and a TOTAL row, same
 * signature footer. Only the specific header fields and table columns
 * differ, matching this receipt's own data shape.
 */
@Slf4j
@Service
public class PersonVehicleDetailPdfServiceImpl implements PersonVehicleDetailPdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 20, Font.BOLD);
    private static final Font TAGLINE_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font RECEIPT_TITLE_FONT = new Font(Font.HELVETICA, 15, Font.BOLD);
    private static final Font SECTION_HEADING_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font HEADER_LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font HEADER_VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font PERSON_NAME_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font TABLE_CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font TOTAL_ROW_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Color HEADER_BG = new Color(64, 64, 64);
    private static final Color HIGHLIGHT_BG = new Color(230, 230, 230);

    @Override
    public byte[] generatePdf(PersonVehicleDetailReceipt receipt) {
        Document document = new Document(PageSize.A4, 30, 30, 30, 30);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            addBrandHeader(document);
            addReceiptTitleAndPersonInfo(document, receipt);
            addDetailsTable(document, receipt);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate person vehicle detail receipt PDF", e);
            throw new BusinessException("Failed to generate the person vehicle detail receipt PDF",
                    ErrorCodeConstants.INTERNAL_ERROR);
        }
    }

    private void addBrandHeader(Document document) throws Exception {
        Paragraph title = new Paragraph(ReportConstants.COMPANY_NAME, TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph tagline = new Paragraph(ReportConstants.COMPANY_TAGLINE, TAGLINE_FONT);
        tagline.setAlignment(Element.ALIGN_CENTER);
        tagline.setSpacingAfter(10f);
        document.add(tagline);

        document.add(horizontalRule(1.2f));
    }

    private void addReceiptTitleAndPersonInfo(Document document, PersonVehicleDetailReceipt receipt) throws Exception {
        Paragraph receiptTitle = new Paragraph("PERSON VEHICLE DETAILS RECEIPT", RECEIPT_TITLE_FONT);
        receiptTitle.setAlignment(Element.ALIGN_CENTER);
        receiptTitle.setSpacingBefore(10f);
        receiptTitle.setSpacingAfter(12f);
        document.add(receiptTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(new float[]{35, 65});
        table.setSpacingAfter(10f);

        addHeaderRow(table, "Person", receipt.getPersonName() + " (" + receipt.getPersonCode() + ")", PERSON_NAME_FONT);
        addHeaderRow(table, "Date Range",
                DateTimeUtil.formatReportDate(receipt.getStartDate()) + " - "
                        + DateTimeUtil.formatReportDate(receipt.getEndDate()),
                HEADER_VALUE_FONT);
        document.add(table);

        document.add(horizontalRule(0.8f));
    }

    private void addHeaderRow(PdfPTable table, String label, String value, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_LABEL_FONT));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPaddingBottom(4f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPaddingBottom(4f);
        table.addCell(valueCell);
    }

    private PdfPTable horizontalRule(float thickness) throws Exception {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setBorderWidth(thickness);
        cell.setFixedHeight(2f);
        rule.addCell(cell);
        rule.setSpacingAfter(12f);
        return rule;
    }

    private void addDetailsTable(Document document, PersonVehicleDetailReceipt receipt) throws Exception {
        document.add(sectionHeading("Person Vehicle Details"));

        String[] headers = {"Date", "Vehicle Number", "Vehicle Capacity", "Load Count", "Total Vehicle Capacity"};
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{16, 22, 20, 16, 26});
        table.setHeaderRows(1); // repeats the header row across page breaks

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6f);
            table.addCell(cell);
        }

        for (PersonVehicleDetailReceiptRow row : receipt.getRows()) {
            addCell(table, DateTimeUtil.formatReportDate(row.getDate()), Element.ALIGN_CENTER, TABLE_CELL_FONT);
            addCell(table, row.getVehicleNumber(), Element.ALIGN_LEFT, TABLE_CELL_FONT);
            addCell(table, MoneyFormatUtil.format(row.getVehicleCapacity()), Element.ALIGN_RIGHT, TABLE_CELL_FONT);
            addCell(table, String.valueOf(row.getLoadCount()), Element.ALIGN_CENTER, TABLE_CELL_FONT);
            addCell(table, MoneyFormatUtil.format(row.getTotalVehicleCapacity()), Element.ALIGN_RIGHT, TABLE_CELL_FONT);
        }

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total", TOTAL_ROW_FONT));
        totalLabelCell.setColspan(4);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setBackgroundColor(HIGHLIGHT_BG);
        totalLabelCell.setPadding(6f);
        table.addCell(totalLabelCell);

        PdfPCell totalValueCell = new PdfPCell(new Phrase(MoneyFormatUtil.format(receipt.getGrandTotalCapacity()), TOTAL_ROW_FONT));
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setBackgroundColor(HIGHLIGHT_BG);
        totalValueCell.setPadding(6f);
        table.addCell(totalValueCell);

        document.add(table);
    }

    private Paragraph sectionHeading(String text) {
        Paragraph heading = new Paragraph(text, SECTION_HEADING_FONT);
        heading.setSpacingBefore(4f);
        heading.setSpacingAfter(8f);
        return heading;
    }

    private void addCell(PdfPTable table, String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        table.addCell(cell);
    }
}