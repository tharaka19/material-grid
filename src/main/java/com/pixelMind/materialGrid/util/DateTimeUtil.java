package com.pixelMind.materialGrid.util;

import com.pixelMind.materialGrid.constant.ReportConstants;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DateTimeUtil {

    private DateTimeUtil() {
    }

    public static final ZoneId SRI_LANKA_ZONE = ZoneId.of("Asia/Colombo");
    private static final Clock CLOCK = Clock.system(SRI_LANKA_ZONE);

    private static final DateTimeFormatter REPORT_DATE_FORMATTER =
            DateTimeFormatter.ofPattern(ReportConstants.REPORT_DATE_PATTERN);

    public static LocalDateTime now() {
        return LocalDateTime.now(CLOCK);
    }

    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(CLOCK);
    }

    /**
     * Formats a date exactly as shown in the provided sample PDF ("2026.08.11").
     */
    public static String formatReportDate(LocalDate date) {
        return date == null ? "" : date.format(REPORT_DATE_FORMATTER);
    }
}