package com.pixelMind.materialGrid.util;

import com.pixelMind.materialGrid.constant.ReportConstants;

import java.time.LocalDate;

/**
 * MODIFIED: buildFileName now takes a date RANGE instead of a single date,
 * matching the report's new startDate/endDate parameters. Filename dates
 * use ISO (LocalDate.toString(), e.g. "2026-08-01") rather than the PDF's
 * on-page dd/MM/yyyy display format - filenames should stay
 * machine-sortable/parseable, which dd/MM/yyyy is not (see spec section 28:
 * ISO for API/machine contexts, dd/MM/yyyy only for human-facing PDF
 * display).
 */
public final class PdfFileNameUtil {

    private PdfFileNameUtil() {
    }

    public static String buildFileName(String vehicleNumber, LocalDate startDate, LocalDate endDate) {
        String safeVehicleNumber = vehicleNumber == null
                ? "unknown"
                : vehicleNumber.replaceAll("[^A-Za-z0-9-]", "");
        return ReportConstants.PDF_FILENAME_PREFIX + "-" + safeVehicleNumber
                + "-" + startDate + "-to-" + endDate + ".pdf";
    }

    /**
     * NEW: for the Person Vehicle Details receipt - same sanitization
     * approach as buildFileName above, keyed by personCode instead of
     * vehicleNumber. Produces e.g.
     * "person-vehicle-details-PER000001-2026-09-01-to-2026-09-30.pdf".
     */
    public static String buildPersonVehicleDetailFileName(String personCode, LocalDate startDate, LocalDate endDate) {
        String safePersonCode = personCode == null
                ? "unknown"
                : personCode.replaceAll("[^A-Za-z0-9-]", "");
        return ReportConstants.PERSON_VEHICLE_DETAIL_PDF_FILENAME_PREFIX + "-" + safePersonCode
                + "-" + startDate + "-to-" + endDate + ".pdf";
    }
}