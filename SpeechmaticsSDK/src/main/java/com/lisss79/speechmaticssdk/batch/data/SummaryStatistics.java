package com.lisss79.speechmaticssdk.batch.data;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.COUNT;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.DURATION_HOURS;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.JOB_MODE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.JOB_TYPE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.OPERATING_POINT;

import androidx.annotation.NonNull;

import com.lisss79.speechmaticssdk.common.OperatingPoint;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Общая статистика
 */
public class SummaryStatistics {

    private String mode;
    private JobConfig.JobType type;
    private int count;
    private float duration_hrs;
    private OperatingPoint operatingPoint;

    public SummaryStatistics() {
        mode = "";
        type = JobConfig.JobType.TRANSCRIPTION;
        operatingPoint = OperatingPoint.ENHANCED;
        count = 0;
        duration_hrs = 0;
    }

    public SummaryStatistics(JSONObject json) {
        this();
        try {
            parseJSON(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void parseJSON(JSONObject json) throws JSONException {
        if (json.has(JOB_MODE)) mode = json.getString(JOB_MODE);
        if (json.has(JOB_TYPE)) type = JobConfig.JobType.getJobType(json.getString(JOB_TYPE));
        if (json.has(OPERATING_POINT)) operatingPoint =
                OperatingPoint.getOperationPoint(json.getString(OPERATING_POINT));

        // Получаем число работ и длительность
        if (json.has(COUNT)) count = json.getInt(COUNT);
        if (json.has(DURATION_HOURS)) duration_hrs = (float) json.getDouble(DURATION_HOURS);
    }

    public String getMode() {
        return mode;
    }

    public JobConfig.JobType getType() {
        return type;
    }

    public int getCount() {
        return count;
    }

    public float getDuration_hrs() {
        return duration_hrs;
    }

    public OperatingPoint getOperatingPoint() {
        return operatingPoint;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public void setType(JobConfig.JobType type) {
        this.type = type;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public void setDuration_hrs(float duration_hrs) {
        this.duration_hrs = duration_hrs;
    }

    public void setOperatingPoint(OperatingPoint operatingPoint) {
        this.operatingPoint = operatingPoint;
    }

    @NonNull
    @Override
    public String toString() {
        return "SummaryStatistics{" +
                "mode='" + mode + '\'' +
                ", type=" + type.getName() +
                ", accuracy=" + operatingPoint.getName() +
                ", count=" + count +
                ", duration_hrs=" + duration_hrs +
                '}';
    }
}
