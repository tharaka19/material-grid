package com.pixelMind.materialGrid.constant;

public final class ReportConstants {

    private ReportConstants() {
    }

    public static final String COMPANY_NAME = "MALSHI SUPPLIERS";
    public static final String COMPANY_TAGLINE = "BUILDING MATERIALS SUPPLIERS & TRANSPORT SERVICES";

    public static final String REPORT_DATE_PATTERN = "dd/MM/yyyy";

    public static final String CURRENCY_PREFIX = "Rs. ";

    /** MODIFIED: was "daily-route-report" - the report is now explicitly a
     * payment receipt, per this enhancement's example filename. */
    public static final String PDF_FILENAME_PREFIX = "vehicle-payment-receipt";
    public static final String PERSON_VEHICLE_DETAIL_PDF_FILENAME_PREFIX = "person-vehicle-details";

}