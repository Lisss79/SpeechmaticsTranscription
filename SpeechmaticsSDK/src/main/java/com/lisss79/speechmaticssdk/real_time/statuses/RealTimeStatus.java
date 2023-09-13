package com.lisss79.speechmaticssdk.real_time.statuses;

import android.graphics.Color;

import java.util.Locale;

/**
 * Допустимые значения статуса распознавания в реальном времени
 */
public enum RealTimeStatus {

    NO_STATUS("нет", "no", Color.GRAY),
    READY("готов", "ready", Color.GRAY),
    CONNECTION("подключение", "connection", Color.DKGRAY),
    CONNECTION_ERROR("ошибка подключения", "connection error", Color.RED),
    WORKING("в работе", "working", Color.GREEN),
    RUNTIME_ERROR("ошибка при работе", "runtime error", Color.RED),
    FINISHING("завершение", "finishing", Color.GREEN),
    NO_PERMISSION("нет разрешения", "no permission", Color.RED);

    private final String name;
    private final int color;

    RealTimeStatus(String nameRu, String nameEn, int color) {
        this.color = color;
        boolean langRu = Locale.getDefault().getLanguage().equals("ru");
        if(langRu) this.name = nameRu;
        else this.name = nameEn;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }
}
