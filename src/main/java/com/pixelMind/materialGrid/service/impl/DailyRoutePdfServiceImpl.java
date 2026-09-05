package com.pixelMind.materialGrid.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.constant.ReportConstants;
import com.pixelMind.materialGrid.dto.response.DailyExpensesPaymentReceiptRow;
import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceipt;
import com.pixelMind.materialGrid.dto.response.DailyRoutePaymentReceiptRow;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.service.DailyRoutePdfService;
import com.pixelMind.materialGrid.util.DateTimeUtil;
import com.pixelMind.materialGrid.util.MoneyFormatUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;

/**
 * MODIFIED: Daily Route Details table gains "Route Code" and "KM" columns
 * (Date | Route Code | KM | Load Count | Price Rate | Total Amount | Paid
 * Amount), and Financial Summary gains a "Total KM" line alongside "Total
 * Load Count". Route Code is a comma-joined list of distinct routes run
 * that day - a date can legitimately span multiple routes, so it's shown
 * as a list rather than forced into one value (unlike Price Rate, which
 * IS validated as single-valued per date - see the service layer).
 */
@Slf4j
@Service
public class DailyRoutePdfServiceImpl implements DailyRoutePdfService {

    private static final Font TITLE_FONT = new Font(Font.HELVETICA, 20, Font.BOLD);
    private static final Font TAGLINE_FONT = new Font(Font.HELVETICA, 11, Font.NORMAL);
    private static final Font RECEIPT_TITLE_FONT = new Font(Font.HELVETICA, 15, Font.BOLD);
    private static final Font SECTION_HEADING_FONT = new Font(Font.HELVETICA, 12, Font.BOLD);
    private static final Font HEADER_LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font HEADER_VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font VEHICLE_NUMBER_FONT = new Font(Font.HELVETICA, 13, Font.BOLD);
    private static final Font SUMMARY_LABEL_FONT = new Font(Font.HELVETICA, 10, Font.BOLD);
    private static final Font SUMMARY_VALUE_FONT = new Font(Font.HELVETICA, 10, Font.NORMAL);
    private static final Font SUMMARY_HIGHLIGHT_FONT = new Font(Font.HELVETICA, 11, Font.BOLD);
    private static final Font TABLE_HEADER_FONT = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
    private static final Font TABLE_CELL_FONT = new Font(Font.HELVETICA, 9, Font.NORMAL);
    private static final Font TOTAL_ROW_FONT = new Font(Font.HELVETICA, 9, Font.BOLD);
    private static final Color HEADER_BG = new Color(64, 64, 64);
    private static final Color HIGHLIGHT_BG = new Color(230, 230, 230);

    @Override
    public byte[] generatePdf(DailyRoutePaymentReceipt receipt) {
        Document document = new Document(PageSize.A4, 20, 20, 20, 20);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfWriter.getInstance(document, out);
            document.open();

            addBrandHeader(document);
            addReceiptTitleAndVehicleInfo(document, receipt);
            addDailyRouteDetails(document, receipt);
            addDailyExpensesDetails(document, receipt);
            addFinancialSummary(document, receipt);
            addSignatureFooter(document);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate vehicle payment receipt PDF", e);
            throw new BusinessException("Failed to generate the vehicle payment receipt PDF",
                    ErrorCodeConstants.INTERNAL_ERROR);
        }
    }

    private void addBrandHeader(Document document) throws Exception {
        Paragraph title = new Paragraph(ReportConstants.COMPANY_NAME, TITLE_FONT);
        title.setAlignment(Element.ALIGN_CENTER);
        document.add(title);

        Paragraph tagline = new Paragraph(ReportConstants.COMPANY_TAGLINE, TAGLINE_FONT);
        tagline.setAlignment(Element.ALIGN_CENTER);
        tagline.setSpacingAfter(6f);
        document.add(tagline);

        document.add(horizontalRule(1.2f));
    }

    private void addReceiptTitleAndVehicleInfo(Document document, DailyRoutePaymentReceipt receipt) throws Exception {
        Paragraph receiptTitle = new Paragraph("VEHICLE PAYMENT RECEIPT", RECEIPT_TITLE_FONT);
        receiptTitle.setAlignment(Element.ALIGN_CENTER);
        receiptTitle.setSpacingBefore(4f);
        receiptTitle.setSpacingAfter(6f);
        document.add(receiptTitle);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(new float[]{35, 65});
        table.setSpacingAfter(6f);

        addHeaderRow(table, "Vehicle Number", receipt.getVehicleNumber(), VEHICLE_NUMBER_FONT);
        addHeaderRow(table, "Vehicle Capacity",
                receipt.getVehicleCapacity() != null ? receipt.getVehicleCapacity().toPlainString() : "-",
                HEADER_VALUE_FONT);
        addHeaderRow(table, "Period",
                DateTimeUtil.formatReportDate(receipt.getStartDate()) + " - "
                        + DateTimeUtil.formatReportDate(receipt.getEndDate()),
                HEADER_VALUE_FONT);
        document.add(table);

        document.add(horizontalRule(0.8f));
    }

    private void addHeaderRow(PdfPTable table, String label, String value, Font valueFont) {
        PdfPCell labelCell = new PdfPCell(new Phrase(label, HEADER_LABEL_FONT));
        labelCell.setBorder(PdfPCell.NO_BORDER);
        labelCell.setPaddingBottom(3f);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.NO_BORDER);
        valueCell.setPaddingBottom(3f);
        table.addCell(valueCell);
    }

    private PdfPTable horizontalRule(float thickness) {
        PdfPTable rule = new PdfPTable(1);
        rule.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(PdfPCell.BOTTOM);
        cell.setBorderWidth(thickness);
        cell.setFixedHeight(2f);
        rule.addCell(cell);
        rule.setSpacingAfter(6f);
        return rule;
    }

    private void addDailyRouteDetails(Document document, DailyRoutePaymentReceipt receipt) {
        document.add(sectionHeading("Daily Routes"));

        String[] headers = {"Date", "Route Code", "KM", "Load Count", "Price Rate (Rs.)", "Total Amount (Rs.)"};
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{12, 16, 10, 10, 16, 18});
        table.setHeaderRows(1); // repeats the header row across page breaks
        table.setSpacingAfter(8f);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4.5f);
            table.addCell(cell);
        }

        for (DailyRoutePaymentReceiptRow row : receipt.getRows()) {
            addCell(table, DateTimeUtil.formatReportDate(row.getDate()), Element.ALIGN_CENTER, TABLE_CELL_FONT);
            addCell(table, row.getRouteCode(), Element.ALIGN_CENTER, TABLE_CELL_FONT);
            addCell(table, MoneyFormatUtil.format(row.getRouteDistance()), Element.ALIGN_RIGHT, TABLE_CELL_FONT);
            addCell(table, String.valueOf(row.getLoadCount()), Element.ALIGN_RIGHT, TABLE_CELL_FONT);
            addCell(table, MoneyFormatUtil.format(row.getPriceRate()), Element.ALIGN_RIGHT, TABLE_CELL_FONT);
            addCell(table, MoneyFormatUtil.format(row.getTotalAmount()), Element.ALIGN_RIGHT, TABLE_CELL_FONT);
        }

        addCell(table, "TOTAL", Element.ALIGN_CENTER, TOTAL_ROW_FONT);
        addCell(table, "-", Element.ALIGN_LEFT, TOTAL_ROW_FONT);
        addCell(table, "-", Element.ALIGN_RIGHT, TOTAL_ROW_FONT);
        addCell(table, String.valueOf(receipt.getTotalLoadCount()), Element.ALIGN_RIGHT, TOTAL_ROW_FONT);
        addCell(table, "-", Element.ALIGN_RIGHT, TOTAL_ROW_FONT);
        addCell(table, MoneyFormatUtil.format(receipt.getTotalAmount()), Element.ALIGN_RIGHT, TOTAL_ROW_FONT);

        document.add(table);
    }

    private void addDailyExpensesDetails(Document document, DailyRoutePaymentReceipt receipt) {
        document.add(sectionHeading("Daily Expenses"));

        String[] headers = {"Date", "Total Paid Amount (Rs.)"};
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(50);
        table.setWidths(new float[]{4, 10});
        table.setHeaderRows(1); // repeats the header row across page breaks
        table.setSpacingAfter(8f);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);

        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, TABLE_HEADER_FONT));
            cell.setBackgroundColor(HEADER_BG);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(4.5f);
            table.addCell(cell);
        }

        for (DailyExpensesPaymentReceiptRow row : receipt.getPaidRows()) {
            addCell(table, DateTimeUtil.formatReportDate(row.getDate()), Element.ALIGN_CENTER, TABLE_CELL_FONT);
            addCell(table, MoneyFormatUtil.format(row.getPaidAmount()), Element.ALIGN_RIGHT, TABLE_CELL_FONT);
        }

        addCell(table, "TOTAL", Element.ALIGN_CENTER, TOTAL_ROW_FONT);
        addCell(table, MoneyFormatUtil.format(receipt.getTotalPaidAmount()), Element.ALIGN_RIGHT, TOTAL_ROW_FONT);

        document.add(table);
    }

    private void addFinancialSummary(Document document, DailyRoutePaymentReceipt receipt) throws Exception {
        document.add(sectionHeading("Financial Summary"));

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(60);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setWidths(new float[]{45, 55});

//        addSummaryRow(table, "Total Load Count", String.valueOf(receipt.getTotalLoadCount()), false);
//        addSummaryRow(table, "Price Rate",
//                receipt.isPriceRateVaries() ? "Varies across period" : MoneyFormatUtil.format(receipt.getPriceRate()),
//                false);
        addSummaryRow(table, "Total Amount", ReportConstants.CURRENCY_PREFIX + MoneyFormatUtil.format(receipt.getTotalAmount()), false);
        addSummaryRow(table, "Total Paid Amount", ReportConstants.CURRENCY_PREFIX + MoneyFormatUtil.format(receipt.getTotalPaidAmount()), false);
        addSummaryRow(table, "Licence Fee", ReportConstants.CURRENCY_PREFIX + MoneyFormatUtil.format(receipt.getLicenceFee()), false);
        addSummaryRow(table, "Balance", ReportConstants.CURRENCY_PREFIX + MoneyFormatUtil.format(receipt.getBalance()), true);

        document.add(table);
    }

    private Paragraph sectionHeading(String text) {
        Paragraph heading = new Paragraph(text, SECTION_HEADING_FONT);
        heading.setSpacingBefore(3f);
        heading.setSpacingAfter(5f);
        return heading;
    }

    private void addSummaryRow(PdfPTable table, String label, String value, boolean highlight) {
        Font labelFont = highlight ? SUMMARY_HIGHLIGHT_FONT : SUMMARY_LABEL_FONT;
        Font valueFont = highlight ? SUMMARY_HIGHLIGHT_FONT : SUMMARY_VALUE_FONT;

        PdfPCell labelCell = new PdfPCell(new Phrase(label, labelFont));
        labelCell.setBorder(PdfPCell.BOX);
        labelCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        labelCell.setPadding(4.5f);
        if (highlight) {
            labelCell.setBackgroundColor(HIGHLIGHT_BG);
        }
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(value, valueFont));
        valueCell.setBorder(PdfPCell.BOX);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        valueCell.setPadding(4.5f);
        if (highlight) {
            valueCell.setBackgroundColor(HIGHLIGHT_BG);
        }
        table.addCell(valueCell);
    }

    private void addCell(PdfPTable table, String text, int alignment, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(4f);
        table.addCell(cell);
    }

    private void addSignatureFooter(Document document) throws Exception {
        PdfPTable footer = new PdfPTable(3);
        footer.setWidthPercentage(100);
        footer.setSpacingBefore(18f);
        footer.setWidths(new float[]{1, 1, 1});

        String[] labels = {"Prepared By", "Checked By", "Authorized Signature"};
        for (String label : labels) {
            PdfPCell cell = new PdfPCell();
            cell.setBorder(PdfPCell.TOP);
            cell.setBorderWidth(0.8f);
            cell.setPaddingTop(4f);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.addElement(new Phrase(label, SUMMARY_VALUE_FONT));
            footer.addCell(cell);
        }
        document.add(footer);
    }
}