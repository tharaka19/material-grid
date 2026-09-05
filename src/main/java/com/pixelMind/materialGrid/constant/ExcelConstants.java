package com.pixelMind.materialGrid.constant;

import java.util.List;
import java.util.Set;

public final class ExcelConstants {

    private ExcelConstants() {
    }

    public static final Set<String> ALLOWED_EXCEL_EXTENSIONS = Set.of("xlsx", "xls");
    public static final List<String> VEHICLE_HEADERS = List.of("Vehicle Number", "Capacity");
    public static final List<String> VEHICLE_EXPENSE_HEADERS = List.of("Date", "Vehicle Number", "Expense");
    public static final List<String> DAILY_ROUTE_HEADERS = List.of("Date", "Vehicle Number", "Route Code");
    public static final List<String> VEHICLE_LICENSE_HEADERS = List.of("Vehicle Number", "License Code");
    public static final List<String> PERSON_VEHICLE_DETAIL_HEADERS = List.of("Date", "Person Code", "Vehicle Number");
}