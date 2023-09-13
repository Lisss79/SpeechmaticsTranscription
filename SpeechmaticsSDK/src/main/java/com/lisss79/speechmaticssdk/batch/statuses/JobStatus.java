package com.lisss79.speechmaticssdk.batch.statuses;

import java.util.Locale;

/**
 * Допустимые значения статуса работы
 */
public enum JobStatus {

    NONE("", "не создана", "not created"),
    RUNNING("running", "в обработке", "running"),
    DONE("done", "выполнена", "done"),
    REJECTED("rejected", "отклонена", "rejected"),
    DELETED("deleted","удалена", "deleted"),
    EXPIRED("expired", "истек срок", "expired"),
    UNKNOWN("unknown", "неизвестно", "unknown");

    private final String name;
    private final String code;
    JobStatus(String code, String nameRu, String nameEn) {
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

    public static JobStatus getJobStatus(String code) {
        JobStatus stat = JobStatus.NONE;
        for(JobStatus jobStatus: JobStatus.values()) {
            if(jobStatus.getCode().equals(code)) {
                stat = jobStatus;
                break;
            }
        }
        return stat;
    }

}
