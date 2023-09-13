package com.lisss79.speechmaticssdk.batch.data;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.*;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

/**
 * Класс, содержащий данные об ошибке, полученной от сервера при отправке работы.
 * Только чтение. Ответ сервера при кодах 400..500
 */
public class ErrorMessage {
    private int code;
    private String error;
    private String detail;
    private boolean isError;

    public ErrorMessage() {
        code = -1;
        error = "";
        detail = "";
        isError = false;
    }

    public ErrorMessage(JSONObject json) {
        this();
        try {
            parseJSON(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public ErrorMessage(String response) {
        this();
        JSONObject json;
        try {
            json = new JSONObject(response);
            parseJSON(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void parseJSON(JSONObject json) throws JSONException {
            if (json.has((CODE))) code = json.getInt((CODE));
            if (json.has(ERROR)) error = json.getString(ERROR);
            if (json.has(DETAIL)) detail = json.getString(DETAIL);
            if(code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_CREATED) {
                isError = true;
            }
    }

    public int getCode() {
        return code;
    }

    public String getError() {
        return error;
    }

    public String getDetail() {
        return detail;
    }

    public void setCode(int code) {
        this.code = code;
        isError = code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_CREATED;
    }

    public void setError(String error) {
        this.error = error;
    }

    @NonNull
    @Override
    public String toString() {
        if(isError)
            return "There is the error. Code= " + code + ", error: " + error + ", detail: " + detail;
        else return "No errors. Response code= " + code;
    }
}
