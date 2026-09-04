package com.pixelMind.materialGrid.constant;

public final class CodeSequenceConstants {

    private CodeSequenceConstants() {
    }

    public static final String ROUTE_CODE_SEQUENCE = "ROUTE_CODE";
    public static final String ROUTE_CODE_PREFIX = "RT";
    public static final int ROUTE_CODE_PAD_LENGTH = 6;

    public static final String LICENSE_CODE_SEQUENCE = "LICENSE_CODE";
    public static final String LICENSE_CODE_PREFIX = "LIC";
    public static final int LICENSE_CODE_PAD_LENGTH = 6;

    // New for the Person module - reuses the existing CodeGeneratorService
    // mechanism unchanged, same as Route/License.
    public static final String PERSON_CODE_SEQUENCE = "PERSON_CODE";
    public static final String PERSON_CODE_PREFIX = "PER";
    public static final int PERSON_CODE_PAD_LENGTH = 6;
}