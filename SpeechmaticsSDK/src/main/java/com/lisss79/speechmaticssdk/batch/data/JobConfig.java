package com.lisss79.speechmaticssdk.batch.data;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ADDITIONAL_VOCAB;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ALIGNMENT_CONFIG;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.DIARIZATION;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.FETCH_DATA;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.JOB_TYPE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.LANGUAGE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.OPERATING_POINT;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.TRANSCRIPTION_CONFIG;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.URL;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.common.AdditionalVocab;
import com.lisss79.speechmaticssdk.common.Language;
import com.lisss79.speechmaticssdk.common.OperatingPoint;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;

/**
 * Конфигурация работы. Чтение и запись. При чтении - часть JobDetails.
 * При записи - главный элемент
 */
public class JobConfig {

    private JobType jobType;
    private TranscriptionConfig transcriptionConfig;
    private AlignmentConfig alignmentConfig;
    private FetchData fetchData;
    private final String CR = System.lineSeparator();

    public JobConfig() {
        this.jobType = SpeechmaticsBatchSDK.defJobType;
        this.transcriptionConfig = new TranscriptionConfig();
        this.alignmentConfig = new AlignmentConfig();
        this.fetchData = new FetchData();
    }

    public JobConfig(JSONObject json) {
        this();
        try {
            if (json.has(JOB_TYPE)) jobType = JobType.getJobType(json.getString(JOB_TYPE));
            if (json.has(TRANSCRIPTION_CONFIG)) {
                JSONObject jsonTranscriptionConfig = json.getJSONObject(TRANSCRIPTION_CONFIG);
                String stringTranscriptionConfig = jsonTranscriptionConfig.toString();
                transcriptionConfig = new TranscriptionConfig(stringTranscriptionConfig);
            }
            if (json.has(ALIGNMENT_CONFIG)) {
                JSONObject jsonAC = json.getJSONObject(ALIGNMENT_CONFIG);
                alignmentConfig = new AlignmentConfig(jsonAC);
            }
            if (json.has(FETCH_DATA)) {
                JSONObject jsonFD = json.getJSONObject(FETCH_DATA);
                fetchData = new FetchData(jsonFD);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JobType getJobType() {
        return jobType;
    }

    public void setJobType(JobType jobType) {
        this.jobType = jobType;
    }

    public TranscriptionConfig getTranscriptionConfig() {
        return transcriptionConfig;
    }
    public AlignmentConfig getAlignmentConfig() {
        return alignmentConfig;
    }

    public FetchData getFetchData() {
        return fetchData;
    }

    public void setTranscriptionConfig(TranscriptionConfig transcriptionConfig) {
        this.transcriptionConfig = transcriptionConfig;
    }

    public void setAlignmentConfig(AlignmentConfig alignmentConfig) {
        this.alignmentConfig = alignmentConfig;
    }

    public void setFetchData(FetchData fetchData) {
        this.fetchData = fetchData;
    }

    /**
     * Возвращает строку для отображения
     * @return строка, понятная пользователю
     */
    @NonNull
    @Override
    public String toString() {
        return  JOB_TYPE + ": " + jobType.getName() + ", " + CR +
                TRANSCRIPTION_CONFIG + ": " + transcriptionConfig.toString() + ", " + CR +
                ALIGNMENT_CONFIG + ": " + alignmentConfig.toString() + ", " + CR +
                FETCH_DATA + ": " + fetchData.toString();
    }

    /**
     * Возвращает строку для отправки на сервер
     * @return строка, понятная серверу
     */
    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put(JOB_TYPE, jobType.getCode());
            json.put(TRANSCRIPTION_CONFIG, transcriptionConfig.toJson());
            json.put(ALIGNMENT_CONFIG, alignmentConfig.toJson());
            if(!fetchData.getUrl().isEmpty()) json.put(FETCH_DATA, fetchData.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString().replace("\\/", "/");
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(JOB_TYPE, jobType.getCode());
            json.put(TRANSCRIPTION_CONFIG, transcriptionConfig.toJson());
            json.put(ALIGNMENT_CONFIG, alignmentConfig.toJson());
            if(!fetchData.getUrl().isEmpty()) json.put(FETCH_DATA, fetchData.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }


    /**
     * Билдер для конфигурации
     */
    public static class Builder {

        private final JobConfig jc;
        private final TranscriptionConfig tc;
        private final AlignmentConfig ac;
        private final FetchData fd;
        public Builder() {
            jc = new JobConfig();
            tc = jc.getTranscriptionConfig();
            ac = jc.getAlignmentConfig();
            fd = jc.getFetchData();
        }

        public Builder jobType(JobType jt) {
            jc.setJobType(jt);
            return this;
        }
        public Builder language(Language l) {
            tc.setLanguage(l);
            jc.setTranscriptionConfig(tc);
            ac.setLanguage(l);
            jc.setAlignmentConfig(ac);
            return this;
        }
        public Builder diarization(TranscriptionConfig.Diarization d) {
            tc.setDiarization(d);
            jc.setTranscriptionConfig(tc);
            return this;
        }
        public Builder operatingPoint(OperatingPoint op) {
            tc.setOperatingPoint(op);
            jc.setTranscriptionConfig(tc);
            return this;
        }
        public Builder url(String url) {
            fd.setUrl(url);
            jc.setFetchData(fd);
            return this;
        }
        public Builder additionalVocab(@Nullable AdditionalVocab[] addVoc) {
            if(addVoc != null) {
                tc.setAdditionalVocab(addVoc);
                jc.setTranscriptionConfig(tc);
            }
            return this;
        }
        public JobConfig build() {
            return jc;
        }
    }

    /**
     * Конфигурация для выравнивания. Для чтения и записи.
     * Часть JobConfig
     */
    public static class AlignmentConfig {

        private Language language;

        public AlignmentConfig() {
            language = SpeechmaticsBatchSDK.defLanguage;
        }

        public AlignmentConfig(JSONObject json) {
            this();
            try {
                parseJSON(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public AlignmentConfig(String response) {
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
            if (json.has(LANGUAGE)) language = Language.getLanguage(json.getString(LANGUAGE));
        }

        public Language getLanguage() {
            return language;
        }

        public void setLanguage(Language language) {
            this.language = language;
        }

        @NonNull
        @Override
        public String toString() {
            return LANGUAGE + ": " + language.getName();
        }

        public String toJsonString() {
            JSONObject json = new JSONObject();
            try {
                json.put(LANGUAGE, language.getCode());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json.toString();
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put(LANGUAGE, language.getCode());
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

    }

    /**
     * Конфигурация расшифровки. Чтение и запись.
     * Часть JobConfig
     */
    public static class TranscriptionConfig {

        private Language language;
        private Diarization diarization;
        private OperatingPoint operatingPoint;
        private AdditionalVocab[] additionalVocab;
        private final String CR = System.lineSeparator();

        public TranscriptionConfig() {
            language = SpeechmaticsBatchSDK.defLanguage;
            diarization = SpeechmaticsBatchSDK.defDiarization;
            operatingPoint = SpeechmaticsBatchSDK.defOperatingPoint;
            additionalVocab = null;
        }

        public TranscriptionConfig(String jsonString) {
            this();
            try {
                JSONObject json = new JSONObject(jsonString);
                if (json.has(LANGUAGE)) language = Language.getLanguage(json.getString(LANGUAGE));
                if (json.has(DIARIZATION)) diarization = Diarization.getDiarization(json.getString(DIARIZATION));
                if (json.has(OPERATING_POINT)) operatingPoint =
                        OperatingPoint.getOperationPoint(json.getString(OPERATING_POINT));
                if (json.has(ADDITIONAL_VOCAB)) additionalVocab =
                        AdditionalVocab.fromString(json.getString(ADDITIONAL_VOCAB));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public Language getLanguage() {
            return language;
        }

        public void setLanguage(Language language) {
            this.language = language;
        }

        public Diarization getDiarization() {
            return diarization;
        }

        public void setDiarization(Diarization diarization) {
            this.diarization = diarization;
        }

        public OperatingPoint getOperatingPoint() {
            return operatingPoint;
        }

        public void setOperatingPoint(OperatingPoint operatingPoint) {
            this.operatingPoint = operatingPoint;
        }

        public AdditionalVocab[] getAdditionalVocab() {
            return additionalVocab;
        }

        public void setAdditionalVocab(AdditionalVocab[] additionalVocab) {
            this.additionalVocab = additionalVocab;
        }

        @NonNull
        @Override
        public String toString() {
            return  LANGUAGE + ": " + language.getName() + ", " + CR +
                    DIARIZATION + ": " + diarization.getName() + ", " + CR +
                    OPERATING_POINT + ": " + operatingPoint.getName() + ", " + CR +
                    ADDITIONAL_VOCAB + ": " + Arrays.toString(additionalVocab);
        }

        public String toJsonString() {
            JSONObject json = new JSONObject();
            try {
                json.put(LANGUAGE, language.getCode());
                json.put(DIARIZATION, diarization.getCode());
                json.put(OPERATING_POINT, operatingPoint.getCode());
                if(additionalVocab != null)
                    json.put(ADDITIONAL_VOCAB, AdditionalVocab.toJsonArray(additionalVocab));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json.toString();
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put(LANGUAGE, language.getCode());
                json.put(DIARIZATION, diarization.getCode());
                json.put(OPERATING_POINT, operatingPoint.getCode());
                if(additionalVocab != null)
                    json.put(ADDITIONAL_VOCAB, AdditionalVocab.toJsonArray(additionalVocab));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        /**
         * Допустимые значения диаризации. Чтение и запись.
         * Часть TranscriptionConfig
         */
        public enum Diarization {
            NONE("none", "нет", "none"),
            SPEAKER("speaker", "спикеры", "speakers");

            private final String name;
            private final String code;
            Diarization(String code, String nameRu, String nameEn) {
                this.code = code;
                boolean langRu = Locale.getDefault().getLanguage().equals("ru");
                if(langRu) this.name = nameRu;
                else this.name = nameEn;
            }

            public String getName() {
                return name;
            }

            public static String[] getAllNames() {
                int length = Diarization.values().length;
                String[] names = new String[length];
                for(int i = 0; i < length; i++) {
                    names[i] = Diarization.values()[i].getName();
                }
                return names;
            }

            public String getCode() {
                return code;
            }

            public static String[] getAllCodes() {
                int length = Diarization.values().length;
                String[] codes = new String[length];
                for(int i = 0; i < length; i++) {
                    codes[i] = Diarization.values()[i].getCode();
                }
                return codes;
            }

            public static Diarization getDiarization(String code) {
                Diarization diar = Diarization.NONE;
                for(Diarization diarization: Diarization.values()) {
                    if(diarization.getCode().equals(code)) {
                        diar = diarization;
                        break;
                    }
                }
                return diar;
            }

        }
    }

    /**
     * Допустимые значения типа работы. Чтение и запись.
     * Часть JobConfig
     */
    public enum JobType {

        //ALIGNMENT("alignment", "выравнивание", "alignment),
        TRANSCRIPTION("transcription", "расшифровка", "transcription");

        private final String name;
        private final String code;
        JobType(String code, String nameRu, String nameEn) {
            this.code = code;
            boolean langRu = Locale.getDefault().getLanguage().equals("ru");
            if(langRu) this.name = nameRu;
            else this.name = nameEn;
        }

        public String getName() {
            return name;
        }

        public static String[] getAllNames() {
            int length = JobType.values().length;
            String[] names = new String[length];
            for(int i = 0; i < length; i++) {
                names[i] = JobType.values()[i].getName();
            }
            return names;
        }

        public String getCode() {
            return code;
        }

        public static String[] getAllCodes() {
            int length = JobType.values().length;
            String[] codes = new String[length];
            for(int i = 0; i < length; i++) {
                codes[i] = JobType.values()[i].getCode();
            }
            return codes;
        }

        public static JobType getJobType(String code) {
            JobType jt = JobType.TRANSCRIPTION;
            for(JobType jobType: JobType.values()) {
                if(jobType.getCode().equals(code)) {
                    jt = jobType;
                    break;
                }
            }
            return jt;
        }

    }

    /**
     * Раздел fetch_data в конфигурации работы со ссылкой на файл. Чтение и запись.
     * Часть JobConfig
     */
    public static class FetchData {

        private String url;

        public FetchData() {
            url = "";
        }

        public FetchData(JSONObject json) {
            this();
            try {
                parseJSON(json);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        public FetchData(String response) {
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
            if (json.has((URL))) url = json.getString((URL));
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        @NonNull
        @Override
        public String toString() {
            String urlStr = url.isEmpty() ? "нет" : url;
            return URL + ": " + urlStr;
        }

        public String toJsonString() {
            JSONObject json = new JSONObject();
            try {
                json.put(URL, url);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json.toString().replace("\\/", "/");
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put(URL, url);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

    }
}
