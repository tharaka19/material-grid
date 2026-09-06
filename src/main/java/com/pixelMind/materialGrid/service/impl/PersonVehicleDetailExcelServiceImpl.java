package com.pixelMind.materialGrid.service.impl;

import com.pixelMind.materialGrid.constant.ErrorCodeConstants;
import com.pixelMind.materialGrid.constant.ReportConstants;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceipt;
import com.pixelMind.materialGrid.dto.response.PersonVehicleDetailReceiptRow;
import com.pixelMind.materialGrid.exception.BusinessException;
import com.pixelMind.materialGrid.service.PersonVehicleDetailExcelService;
import com.pixelMind.materialGrid.util.DateTimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

/**
 * Excel-native equivalent of PersonVehicleDetailPdfServiceImpl - same
 * section order (brand header -> receipt title -> person/date-range info ->
 * details table -> total row), consuming the identical
 * PersonVehicleDetailReceipt DTO. Dates are written as pre-formatted
 * dd/MM/yyyy strings (matching the PDF's display exactly); capacity/load
 * count/total columns are real numeric cells with Excel number formats,
 * which is the correct Excel-native equivalent of the PDF's necessarily
 * text-only rendering.
 */
@Slf4j
@Service
public class PersonVehicleDetailExcelServiceImpl implements PersonVehicleDetailExcelService {

    private static final int TABLE_COLUMN_COUNT = 5;

    @Override
    public byte[] generateExcel(PersonVehicleDetailReceipt receipt) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Receipt");
            setColumnWidths(sheet);

            Styles styles = new Styles(workbook);

            int rowIndex = 0;
            rowIndex = writeBrandHeader(sheet, styles, rowIndex);
            rowIndex = writeReceiptTitleAndPersonInfo(sheet, styles, receipt, rowIndex);
            rowIndex = writeDetailsTable(sheet, styles, receipt, rowIndex);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate person vehicle detail receipt Excel", e);
            throw new BusinessException("Failed to generate the person vehicle detail receipt Excel file",
                    ErrorCodeConstants.INTERNAL_ERROR);
        }
    }

    private int writeBrandHeader(Sheet sheet, Styles styles, int rowIndex) {
        Row companyRow = sheet.createRow(rowIndex);
        Cell companyCell = companyRow.createCell(0);
        companyCell.setCellValue(ReportConstants.COMPANY_NAME);
        companyCell.setCellStyle(styles.companyName);
        mergeRowAcrossTable(sheet, rowIndex);
        rowIndex++;

        Row taglineRow = sheet.createRow(rowIndex);
        Cell taglineCell = taglineRow.createCell(0);
        taglineCell.setCellValue(ReportConstants.COMPANY_TAGLINE);
        taglineCell.setCellStyle(styles.tagline);
        mergeRowAcrossTable(sheet, rowIndex);
        rowIndex++;

        return rowIndex + 1; // blank spacer row
    }

    private int writeReceiptTitleAndPersonInfo(Sheet sheet, Styles styles, PersonVehicleDetailReceipt receipt, int rowIndex) {
        Row titleRow = sheet.createRow(rowIndex);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("PERSON VEHICLE DETAILS RECEIPT");
        titleCell.setCellStyle(styles.receiptTitle);
        mergeRowAcrossTable(sheet, rowIndex);
        rowIndex += 2; // spacer

        rowIndex = writeLabelValueRow(sheet, styles, rowIndex, "Person",
                receipt.getPersonName() + " (" + receipt.getPersonCode() + ")");
        rowIndex = writeLabelValueRow(sheet, styles, rowIndex, "Date Range",
                DateTimeUtil.formatReportDate(receipt.getStartDate()) + " - "
                        + DateTimeUtil.formatReportDate(receipt.getEndDate()));

        return rowIndex + 1; // blank spacer row before the table
    }

    private int writeLabelValueRow(Sheet sheet, Styles styles, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label + ":");
        labelCell.setCellStyle(styles.label);

        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value);
        valueCell.setCellStyle(styles.value);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 1, TABLE_COLUMN_COUNT - 1));

        return rowIndex + 1;
    }

    private int writeDetailsTable(Sheet sheet, Styles styles, PersonVehicleDetailReceipt receipt, int rowIndex) {
        String[] headers = {"Date", "Vehicle Number", "Vehicle Capacity", "Load Count", "Total Vehicle Capacity"};
        Row headerRow = sheet.createRow(rowIndex);
        for (int c = 0; c < headers.length; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(styles.tableHeader);
        }
        rowIndex++;

        for (PersonVehicleDetailReceiptRow row : receipt.getRows()) {
            Row dataRow = sheet.createRow(rowIndex);

            Cell dateCell = dataRow.createCell(0);
            dateCell.setCellValue(DateTimeUtil.formatReportDate(row.getDate()));
            dateCell.setCellStyle(styles.textCell);

            Cell vehicleCell = dataRow.createCell(1);
            vehicleCell.setCellValue(row.getVehicleNumber());
            vehicleCell.setCellStyle(styles.textCell);

            Cell capacityCell = dataRow.createCell(2);
            capacityCell.setCellValue(row.getVehicleCapacity().doubleValue());
            capacityCell.setCellStyle(styles.numberCell);

            Cell loadCountCell = dataRow.createCell(3);
            loadCountCell.setCellValue(row.getLoadCount());
            loadCountCell.setCellStyle(styles.numberCell);

            Cell totalCell = dataRow.createCell(4);
            totalCell.setCellValue(row.getTotalVehicleCapacity().doubleValue());
            totalCell.setCellStyle(styles.numberCell);

            rowIndex++;
        }

        Row totalRow = sheet.createRow(rowIndex);
        Cell totalLabelCell = totalRow.createCell(0);
        totalLabelCell.setCellValue("Total");
        totalLabelCell.setCellStyle(styles.totalLabel);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 2));
        // Merged cells in POI only carry the style of the top-left cell for
        // rendering, but every covered cell should still get a matching
        // style/border so the merge doesn't show a jagged border edge.
        for (int c = 1; c <= 2; c++) {
            Cell filler = totalRow.createCell(c);
            filler.setCellStyle(styles.totalLabel);
        }

        Cell totalLoadCell = totalRow.createCell(3);
        int LoadTotal = receipt.getGrandTotalLoadCount();
        totalLoadCell.setCellValue(LoadTotal);
        totalLoadCell.setCellStyle(styles.totalValue);

        Cell totalValueCell = totalRow.createCell(4);
        BigDecimal grandTotal = receipt.getGrandTotalCapacity() != null ? receipt.getGrandTotalCapacity() : BigDecimal.ZERO;
        totalValueCell.setCellValue(grandTotal.doubleValue());
        totalValueCell.setCellStyle(styles.totalValue);

        return rowIndex + 1;
    }

    private void mergeRowAcrossTable(Sheet sheet, int rowIndex) {
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, TABLE_COLUMN_COUNT - 1));
    }

    private void setColumnWidths(Sheet sheet) {
        sheet.setColumnWidth(0, 16 * 256);
        sheet.setColumnWidth(1, 22 * 256);
        sheet.setColumnWidth(2, 20 * 256);
        sheet.setColumnWidth(3, 14 * 256);
        sheet.setColumnWidth(4, 24 * 256);
    }

    /** All cell styles created once per workbook, mirroring the font/color
     * choices already used in DailyRoutePdfServiceImpl/PersonVehicleDetailPdfServiceImpl
     * (same dark-grey header background, same bold/highlight conventions). */
    private static final class Styles {
        final CellStyle companyName;
        final CellStyle tagline;
        final CellStyle receiptTitle;
        final CellStyle label;
        final CellStyle value;
        final CellStyle tableHeader;
        final CellStyle textCell;
        final CellStyle numberCell;
        final CellStyle integerCell;
        final CellStyle totalLabel;
        final CellStyle totalValue;

        Styles(Workbook workbook) {
            DataFormat dataFormat = workbook.createDataFormat();

            Font companyFont = boldFont(workbook, 16);
            companyName = workbook.createCellStyle();
            companyName.setFont(companyFont);
            companyName.setAlignment(HorizontalAlignment.CENTER);

            Font taglineFont = plainFont(workbook, 10);
            tagline = workbook.createCellStyle();
            tagline.setFont(taglineFont);
            tagline.setAlignment(HorizontalAlignment.CENTER);

            Font titleFont = boldFont(workbook, 13);
            receiptTitle = workbook.createCellStyle();
            receiptTitle.setFont(titleFont);
            receiptTitle.setAlignment(HorizontalAlignment.CENTER);

            Font labelFont = boldFont(workbook, 10);
            label = workbook.createCellStyle();
            label.setFont(labelFont);

            Font valueFont = plainFont(workbook, 10);
            value = workbook.createCellStyle();
            value.setFont(valueFont);

            Font headerFont = boldFont(workbook, 9);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            tableHeader = workbook.createCellStyle();
            tableHeader.setFont(headerFont);
            tableHeader.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
            tableHeader.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            tableHeader.setAlignment(HorizontalAlignment.CENTER);
            applyThinBorder(tableHeader);

            Font cellFont = plainFont(workbook, 9);
            textCell = workbook.createCellStyle();
            textCell.setFont(cellFont);
            applyThinBorder(textCell);

            numberCell = workbook.createCellStyle();
            numberCell.setFont(cellFont);
            numberCell.setDataFormat(dataFormat.getFormat("#,##0.00"));
            numberCell.setAlignment(HorizontalAlignment.RIGHT);
            applyThinBorder(numberCell);

            integerCell = workbook.createCellStyle();
            integerCell.setFont(cellFont);
            integerCell.setDataFormat(dataFormat.getFormat("#,##0"));
            integerCell.setAlignment(HorizontalAlignment.RIGHT);
            applyThinBorder(integerCell);

            Font totalFont = boldFont(workbook, 9);
            totalLabel = workbook.createCellStyle();
            totalLabel.setFont(totalFont);
            totalLabel.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalLabel.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            totalLabel.setAlignment(HorizontalAlignment.RIGHT);
            applyThinBorder(totalLabel);

            totalValue = workbook.createCellStyle();
            totalValue.setFont(totalFont);
            totalValue.setDataFormat(dataFormat.getFormat("#,##0.00"));
            totalValue.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            totalValue.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            totalValue.setAlignment(HorizontalAlignment.RIGHT);
            applyThinBorder(totalValue);
        }

        private Font boldFont(Workbook workbook, int size) {
            Font font = workbook.createFont();
            font.setBold(true);
            font.setFontHeightInPoints((short) size);
            return font;
        }

        private Font plainFont(Workbook workbook, int size) {
            Font font = workbook.createFont();
            font.setFontHeightInPoints((short) size);
            return font;
        }

        private void applyThinBorder(CellStyle style) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
    }
}