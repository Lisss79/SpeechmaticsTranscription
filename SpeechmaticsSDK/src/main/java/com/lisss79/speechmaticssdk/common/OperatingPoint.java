package com.lisss79.speechmaticssdk.common;

import java.util.Locale;

/**
 * Допустимые значения точности расшифровки.  Чтение и запись.
 * Часть TranscriptionConfig
 */
public enum OperatingPoint {
    STANDARD("standard", "стандартное", "standard"),
    ENHANCED("enhanced", "улучшенное", "enhanced");

    private final String name;
    private final String code;
    OperatingPoint(String code, String nameRu, String nameEn) {
        this.code = code;
        boolean langRu = Locale.getDefault().getLanguage().equals("ru");
        if(langRu) this.name = nameRu;
        else this.name = nameEn;
    }

    public String getName() {
        return name;
    }

    public static String[] getAllNames() {
        int length = OperatingPoint.values().length;
        String[] names = new String[length];
        for(int i = 0; i < length; i++) {
            names[i] = OperatingPoint.values()[i].getName();
        }
        return names;
    }

    public String getCode() {
        return code;
    }

    public static String[] getAllCodes() {
        int length = OperatingPoint.values().length;
        String[] codes = new String[length];
        for(int i = 0; i < length; i++) {
            codes[i] = OperatingPoint.values()[i].getCode();
        }
        return codes;
    }

    public static OperatingPoint getOperationPoint(String code) {
        OperatingPoint op = OperatingPoint.ENHANCED;
        for(OperatingPoint operatingPoint: OperatingPoint.values()) {
            if(operatingPoint.getCode().equals(code)) {
                op = operatingPoint;
                break;
            }
        }
        return op;
    }
}
