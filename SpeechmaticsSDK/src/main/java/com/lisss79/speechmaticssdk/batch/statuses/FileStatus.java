package com.lisss79.speechmaticssdk.batch.statuses;

import java.util.Locale;

/**
 * Допустимые значения статуса файла для обработки
 */
public enum FileStatus {

    NOT_SELECTED("", "не выбран", "not selected"),
    SELECTED("selected", "выбран", "selected"),
    LOADING_ERROR("loading_error", "ошибка загрузки", "loading error"),
    SENT("sent", "отправлен", "sent"),
    SENDING_ERROR("sending_error", "ошибка отправки", "sending error"),
    WRONG_FILE_TYPE("wrong_file_type", "неверный тип/размер файла", "wrong file type/size");

    private final String name;
    private final String code;
    FileStatus(String code, String nameRu, String nameEn) {
        this.code = code;
        boolean langRu = Locale.getDefault().getLanguage().equals("ru");
        if(langRu) this.name = nameRu;
        else this.name = nameEn;
    }

    public String getName() {
        return name;
    }

    public String getCode() {
        return code;
    }

    public static FileStatus getFileStatus(String code) {
        FileStatus stat = FileStatus.NOT_SELECTED;
        for(FileStatus fileStatus: FileStatus.values()) {
            if(fileStatus.getCode().equals(code)) {
                stat = fileStatus;
                break;
            }
        }
        return stat;
    }

}
