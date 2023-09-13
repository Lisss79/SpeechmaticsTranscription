package com.lisss79.speechmaticssdk.common;

import java.util.Locale;

/**
 * Допустимые значения языков. Чтение и запись.
 * Часть AlignmentConfig и TranscriptionConfig
 */
public enum Language {

    EN("en", "английский", "English"),
    FR("fr", "французский", "French"),
    DE("de", "немецкий", "German"),
    HI("hi", "хинди", "Hindi"),
    KO("ko", "корейский", "Korean"),
    RU("ru", "русский", "Russian");

    private final String name;
    private final String code;
    Language(String code, String nameRu, String nameEn) {
        this.code = code;
        boolean langRu = Locale.getDefault().getLanguage().equals("ru");
        if(langRu) this.name = nameRu;
        else this.name = nameEn;
    }

    public String getName() {
        return name;
    }

    public static String[] getAllNames() {
        int length = Language.values().length;
        String[] names = new String[length];
        for(int i = 0; i < length; i++) {
            names[i] = Language.values()[i].getName();
        }
        return names;
    }

    public String getCode() {
        return code;
    }

    public static String[] getAllCodes() {
        int length = Language.values().length;
        String[] codes = new String[length];
        for(int i = 0; i < length; i++) {
            codes[i] = Language.values()[i].getCode();
        }
        return codes;
    }

    public static Language getLanguage(String code) {
        Language lang = Language.RU;
        for(Language language: Language.values()) {
            if(language.getCode().equals(code)) {
                lang = language;
                break;
            }
        }
        return lang;
    }
}
