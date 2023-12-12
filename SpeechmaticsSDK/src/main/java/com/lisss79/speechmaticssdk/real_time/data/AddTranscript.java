package com.lisss79.speechmaticssdk.real_time.data;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ALTERNATIVES;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.CONTENT;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.END_TIME;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.LANGUAGE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.METADATA;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.RESULTS;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.START_TIME;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.TRANSCRIPT;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.TYPE;

import androidx.annotation.NonNull;

import com.lisss79.speechmaticssdk.common.Language;
import com.lisss79.speechmaticssdk.real_time.SpeechmaticsRealTimeSDK;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

/**
 * Ответ с расшифровкой. Только чтение.
 */
public class AddTranscript {

    private float startTime;
    private float endTime;
    private String transcript;
    private final ArrayList<Results> results;

    private AddTranscript() {
        startTime = 0;
        endTime = 0;
        transcript = "";
        results = new ArrayList<>();
    }

    public AddTranscript(JSONObject json) {
        this();
        try {
            parseJSON(json);
        } catch (JSONException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void parseJSON(JSONObject json) throws JSONException, NumberFormatException {
        JSONObject jsonMetaData = new JSONObject();
        if (json.has(METADATA)) jsonMetaData = json.getJSONObject(METADATA);
        if (jsonMetaData.has(START_TIME)) startTime = (float) jsonMetaData.getDouble(START_TIME);
        if (jsonMetaData.has(END_TIME)) endTime = (float) jsonMetaData.getDouble(END_TIME);
        if (jsonMetaData.has(TRANSCRIPT)) transcript = jsonMetaData.getString(TRANSCRIPT);

        JSONArray jsonResults = new JSONArray();
        if (json.has(RESULTS)) jsonResults = json.getJSONArray(RESULTS);
        for(int i = 0; i < jsonResults.length(); i++) {
            Results result = new Results(jsonResults.getJSONObject(i));
            results.add(result);
        }
    }

    public float getStartTime() {
        return startTime;
    }

    public float getEndTime() {
        return endTime;
    }

    @NonNull
    public String getTranscript() {
        return transcript;
    }

    public ArrayList<Results> getResults() {
        return results;
    }

    @NonNull
    @Override
    public String toString() {
        return "AddTranscript: " +
                "startTime= " + startTime +
                ", endTime= " + endTime +
                ", transcript= '" + transcript + '\'';
    }

    /**
     * Детали расшифровки. Только чтение. Часть AddTranscript
     */
    public static class Results {
        private Type type;
        private Alternatives alternatives;

        private Results() {}

        public Results(JSONObject json) {
            this();
            try {
                parseJSON(json);
            } catch (JSONException | NumberFormatException e) {
                e.printStackTrace();
            }
        }

        private void parseJSON(JSONObject json) throws JSONException {
            if (json.has(TYPE)) type = Type.getTypeByCode(json.getString(TYPE));
            if (json.has(ALTERNATIVES)) alternatives =
                    new Alternatives(json.getJSONArray(ALTERNATIVES).getJSONObject(0));
            else alternatives = new Alternatives();
        }

        public Type getType() {
            return type;
        }

        public Alternatives getAlternatives() {
            return alternatives;
        }

        /**
         * Тип элемента расшифровки
         */
        public enum Type {

            WORD("word"),
            PUNCTUATION("punctuation"),
            SPEAKER_CHANGE("speaker_change");

            private final String code;

            Type(String code) {
                this.code = code;
            }

            public String getCode() {
                return code;
            }

            public static Type getTypeByCode(String code) {
                Type t = Type.WORD;
                for(Type tempType: Type.values()) {
                    if(tempType.getCode().equals(code)) {
                        t = tempType;
                        break;
                    }
                }
                return t;
            }
        }

        /**
         * Опции для слова/символа (включая значение)
         */
        public static class Alternatives {
            private String content;
            private Language language;

            private Alternatives() {
                content = "";
                language = SpeechmaticsRealTimeSDK.defLanguage;
            }

            public Alternatives(JSONObject json) {
                this();
                try {
                    parseJSON(json);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            private void parseJSON(JSONObject json) throws JSONException {
                if (json.has(CONTENT)) content = json.getString(CONTENT);
                if (json.has(LANGUAGE)) language = Language.getLanguage(json.getString(LANGUAGE));
            }

            public String getContent() {
                return content;
            }

            public Language getLanguage() {
                return language;
            }
        }
    }
}
