package com.lisss79.speechmaticssdk.batch.data;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ALIGNMENT_CONFIG;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.CONFIG;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.CR;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.CREATED_AT;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.DATA_NAME;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.DURATION;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ERRORS;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ID;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.JOB;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.JOB_STATUS;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.MESSAGE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.TIMESTAMP;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.TRANSCRIPTION_CONFIG;

import androidx.annotation.NonNull;

import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.batch.statuses.JobStatus;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Детали работы. Только чтение.
 */
public class JobDetails {

    private Date createdAt;
    private String dataName;
    private String id;
    private String duration;
    private JobStatus status;
    private JobConfig jobConfig;
    private JobConfig.TranscriptionConfig transcriptionConfig;
    private JobConfig.AlignmentConfig alignmentConfig;
    private Errors errors;
    private final static String USER_CREATED_PATTERN = "d MMM yy, HH:mm:ss";

    public JobDetails() {
        this.createdAt = new Date();
        this.dataName = "";
        this.id = "";
        this.duration = "0";
        this.status = JobStatus.NONE;
        this.jobConfig = new JobConfig();
        this.transcriptionConfig = new JobConfig.TranscriptionConfig();
        this.alignmentConfig = new JobConfig.AlignmentConfig();
        this.errors = new Errors();
    }

    public JobDetails(JSONObject json) {
        this();
        try {
            parseJSON(json);
        } catch (JSONException | DateTimeParseException e) {
            e.printStackTrace();
        }
    }

    public JobDetails(String response) {
        this();
        JSONObject json;
        try {
            json = new JSONObject(response);
            parseJSON(json);
        } catch (JSONException | DateTimeParseException e) {
            e.printStackTrace();
        }
    }

    private void parseJSON(JSONObject json) throws JSONException, DateTimeParseException {
        String date = "";
        if (json.has((JOB))) json = json.getJSONObject((JOB));
        if (json.has(CREATED_AT)) {
            date = json.getString(CREATED_AT);
            createdAt = Date.from(Instant.parse(date));
        }
        if (json.has(DATA_NAME)) dataName = json.getString(DATA_NAME);
        if (json.has(ID)) id = json.getString(ID);
        if (json.has(DURATION)) duration = json.getString(DURATION);
        if (json.has(JOB_STATUS)) status = JobStatus.getJobStatus(json.getString(JOB_STATUS));
        if (json.has(CONFIG)) {
            JSONObject jsonConfig = json.getJSONObject(CONFIG);
            jobConfig = new JobConfig(jsonConfig);
            if (jsonConfig.has(TRANSCRIPTION_CONFIG)) {
                JSONObject jsonTranscriptionConfig = jsonConfig.getJSONObject(TRANSCRIPTION_CONFIG);
                String stringTranscriptionConfig = jsonTranscriptionConfig.toString();
                transcriptionConfig = new JobConfig.TranscriptionConfig(stringTranscriptionConfig);
            }
            if (jsonConfig.has(ALIGNMENT_CONFIG)) {
                JSONObject jsonAlignmentConfig = jsonConfig.getJSONObject(ALIGNMENT_CONFIG);
                String stringAlignmentConfig = jsonAlignmentConfig.toString();
                alignmentConfig = new JobConfig.AlignmentConfig(stringAlignmentConfig);
            }
        }
        if(json.has(ERRORS)) errors = new Errors(json.getJSONArray(ERRORS));
    }

    @NonNull
    @Override
    public String toString() {
        return  CREATED_AT + ": " + new SimpleDateFormat
                (USER_CREATED_PATTERN, Locale.getDefault()).format(createdAt) + ", " + CR +
                DATA_NAME + ": " + dataName + ", " + CR +
                ID + ": " + id + ", " + CR +
                DURATION + ": " + SpeechmaticsBatchSDK.durationToString(duration) + ", " + CR +
                JOB_STATUS + ": " + status.getName() + ", " + CR +
                CONFIG + ": " + jobConfig.toString() + ", " + CR +
                ERRORS + ": " + errors + ".";
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getDataName() {
        return dataName;
    }

    public String getId() {
        return id;
    }

    public String getDuration() {
        return duration;
    }

    public JobStatus getStatus() {
        return status;
    }

    public JobConfig getJobConfig() {
        return jobConfig;
    }

    public JobConfig.TranscriptionConfig getTranscriptionConfig() {
        return transcriptionConfig;
    }

    public JobConfig.AlignmentConfig getAlignmentConfig() {
        return alignmentConfig;
    }

    public Errors getErrors() {
        return errors;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    /**
     * Текущие ошибки, возникающие при обработке работы. Только чтение.
     * Часть JobDetails
     */
    public static class Errors {

        private final Map<Date, String> errors;
        private final static String USER_CREATED_PATTERN = "d MMM yy, HH:mm:ss";
        private final String CR = System.lineSeparator();

        public Errors() {
            errors = new LinkedHashMap<>();
        }

        public Errors(JSONArray jsonArray) {
            this();
            try {
                parseJSON(jsonArray);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public Errors(String response) {
            this();
            JSONArray json;
            try {
                json = new JSONArray(response);
                parseJSON(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        private void parseJSON(JSONArray jsonArray) throws JSONException {
            errors.clear();
            Date timestamp;
            String message = "";
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject json = (JSONObject) jsonArray.get(i);
                if (json.has(MESSAGE)) message = json.getString(MESSAGE);
                if (json.has((TIMESTAMP))) {
                    Instant instant = Instant.parse(json.getString((TIMESTAMP)));
                    timestamp = Date.from(instant);
                    errors.put(timestamp, message);
                }
            }
        }

        public Map<Date, String> getErrors() {
            return errors;
        }

        @NonNull
        @Override
        public String toString() {
            if(errors.isEmpty()) return "нет данных";
            StringBuilder sb = new StringBuilder();
            errors.forEach((date, s) -> {
                sb.append(new SimpleDateFormat(USER_CREATED_PATTERN, Locale.getDefault()).format(date))
                        .append(": ").append(s).append(". ");
            });
            return sb.toString();
        }
    }
}
