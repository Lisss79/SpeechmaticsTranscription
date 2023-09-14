package com.lisss79.speechmaticssdk.real_time.data;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ADDITIONAL_VOCAB;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.AUDIO_FORMAT;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.AUDIO_TYPE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.DIARIZATION;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ENABLE_ENTITIES;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ENCODING;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.LANGUAGE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.MESSAGE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.OPERATING_POINT;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.SAMPLE_RATE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.TRANSCRIPTION_CONFIG;

import androidx.annotation.NonNull;

import com.lisss79.speechmaticssdk.common.AdditionalVocab;
import com.lisss79.speechmaticssdk.common.Language;
import com.lisss79.speechmaticssdk.common.OperatingPoint;
import com.lisss79.speechmaticssdk.real_time.SpeechmaticsRealTimeSDK;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Locale;

/**
 * Общая конфигурация для запуска распознавания в реальном времени.
 * Только отправка.
 */
public class StartRecognition {

    private String message;
    private AudioFormat audioFormat;
    private TranscriptionConfigRT config;
    private final String CR = System.lineSeparator();

    public StartRecognition() {
        this.message = "StartRecognition";
        this.audioFormat = new AudioFormat();
        this.config = new TranscriptionConfigRT();
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setAudioFormat(AudioFormat audioFormat) {
        this.audioFormat = audioFormat;
    }

    public void setTranscriptionConfigRT(TranscriptionConfigRT config) {
        this.config = config;
    }

    public String getMessage() {
        return message;
    }

    public AudioFormat getAudioFormat() {
        return audioFormat;
    }

    public TranscriptionConfigRT getTranscriptionConfigRT() {
        return config;
    }

    @NonNull
    @Override
    public String toString() {
        return  MESSAGE + ": " + message + ", " + CR +
                AUDIO_FORMAT + ": " + audioFormat.toString() + ", " + CR +
                TRANSCRIPTION_CONFIG + ": " + config.toString();
    }

    /**
     * Возвращает строку для отправки на сервер
     * @return строка, понятная серверу
     */
    public String toJsonString() {
        JSONObject json = new JSONObject();
        try {
            json.put(MESSAGE, message);
            json.put(AUDIO_FORMAT, audioFormat.toJson());
            json.put(TRANSCRIPTION_CONFIG, config.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json.toString().replace("\\/", "/");
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put(MESSAGE, message);
            json.put(AUDIO_FORMAT, audioFormat.toJson());
            json.put(TRANSCRIPTION_CONFIG, config.toJson());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    /**
     * Билдер для конфигурации
     */
    public static class Builder {

        private final StartRecognition sr;
        private final String msg;
        private final AudioFormat af;
        private final TranscriptionConfigRT tc;

        public Builder() {
            sr = new StartRecognition();
            msg = sr.getMessage();
            af = sr.getAudioFormat();
            tc = sr.getTranscriptionConfigRT();
        }

        public Builder language(Language l) {
            tc.setLanguage(l);
            sr.setTranscriptionConfigRT(tc);
            return this;
        }

        public Builder additionalVocab(AdditionalVocab[] av) {
            if (av != null && av.length > 0) {
                tc.setAdditionalVocab(av);
                sr.setTranscriptionConfigRT(tc);
            }
            return this;
        }

        public Builder diarization(TranscriptionConfigRT.DiarizationRT d) {
            tc.setDiarization(d);
            sr.setTranscriptionConfigRT(tc);
            return this;
        }

        public Builder operatingPoint(OperatingPoint op) {
            tc.setOperatingPoint(op);
            sr.setTranscriptionConfigRT(tc);
            return this;
        }

        public Builder entities(boolean ee) {
            tc.setEntities(ee);
            sr.setTranscriptionConfigRT(tc);
            return this;
        }

        public Builder encoding(AudioFormat.Encoding e) {
            af.setEncoding(e);
            sr.setAudioFormat(af);
            return this;
        }

        public Builder sampleRate(int s) {
            af.setSampleRate(s);
            sr.setAudioFormat(af);
            return this;
        }

        public StartRecognition build() {
            return sr;
        }

    }

    /**
     * Конфигурация аудиоформата для реального времени. Только отправка.
     * Часть сообщения StartRecognition
     */
    public static class AudioFormat {

        private String audioType;
        private Encoding encoding;
        private int sampleRate;
        private final String CR = System.lineSeparator();

        public AudioFormat() {
            audioType = "raw";
            encoding = SpeechmaticsRealTimeSDK.defEncoding;
            sampleRate = SpeechmaticsRealTimeSDK.defSampleRate;
        }

        public void setAudioType(String audioType) {
            this.audioType = audioType;
        }
        public void setEncoding(Encoding encoding) {
            this.encoding = encoding;
        }
        public void setSampleRate(int sampleRate) {
            this.sampleRate = sampleRate;
        }

        public String getAudioType() {
            return audioType;
        }

        public Encoding getEncoding() {
            return encoding;
        }

        public int getSampleRate() {
            return sampleRate;
        }

        @NonNull
        @Override
        public String toString() {
            return  AUDIO_TYPE + ": " + audioType + ", " + CR +
                    ENCODING + ": " + encoding.getName() + ", " + CR +
                    SAMPLE_RATE + ": " + sampleRate;
        }

        public String toJsonString() {
            JSONObject json = new JSONObject();
            try {
                json.put(AUDIO_TYPE, audioType);
                json.put(ENCODING, encoding.getCode());
                json.put(SAMPLE_RATE, sampleRate);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json.toString();
        }

        public JSONObject toJson() {
            JSONObject json = new JSONObject();
            try {
                json.put(AUDIO_TYPE, audioType);
                json.put(ENCODING, encoding.getCode());
                json.put(SAMPLE_RATE, sampleRate);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return json;
        }

        public enum Encoding {

            PCM_32("pcm_f32le", "32 бит", "32 bit"),
            PCM_16("pcm_s16le", "16 бит", "16 bit"),
            MULAW("mulaw", "8 бит", "8 bit");

            private final String name;
            private final String code;

            Encoding(String code, String nameRu, String nameEn) {
                this.code = code;
                boolean langRu = Locale.getDefault().getLanguage().equals("ru");
                if(langRu) this.name = nameRu;
                else this.name = nameEn;
            }

            public String getName() {
                return name;
            }

            public static String[] getAllNames() {
                int length = Encoding.values().length;
                String[] names = new String[length];
                for(int i = 0; i < length; i++) {
                    names[i] = Encoding.values()[i].getName();
                }
                return names;
            }

            public String getCode() {
                return code;
            }

            public static String[] getAllCodes() {
                int length = Encoding.values().length;
                String[] codes = new String[length];
                for(int i = 0; i < length; i++) {
                    codes[i] = Encoding.values()[i].getCode();
                }
                return codes;
            }

            public static Encoding getEncoding(String code) {
                Encoding en = Encoding.PCM_32;
                for(Encoding encoding: Encoding.values()) {
                    if(encoding.getCode().equals(code)) {
                        en = encoding;
                        break;
                    }
                }
                return en;
            }
        }
    }

    /**
     * Конфигурация расшифровки для реального времени. Только отправка.
     * Часть сообщения StartRecognition
     */
    public static class TranscriptionConfigRT {

        private Language language;
        private DiarizationRT diarization;
        private OperatingPoint operatingPoint;
        private AdditionalVocab[] additionalVocab;
        private boolean enableEntities;
        private final String CR = System.lineSeparator();

        public TranscriptionConfigRT() {
            language = SpeechmaticsRealTimeSDK.defLanguage;
            diarization = SpeechmaticsRealTimeSDK.defDiarizationRT;
            additionalVocab = null;
            operatingPoint = SpeechmaticsRealTimeSDK.defOperatingPoint;
            enableEntities = SpeechmaticsRealTimeSDK.defEnableEntities;
        }

        public void setLanguage(Language language) {
            this.language = language;
        }

        public Language getLanguage() {
            return language;
        }

        public void setDiarization(DiarizationRT diarization) {
            this.diarization = diarization;
        }

        public DiarizationRT getDiarization() {
            return diarization;
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

        public void setEntities(boolean enableEntities) {
            this.enableEntities = enableEntities;
        }

        @NonNull
        @Override
        public String toString() {
            return  LANGUAGE + ": " + language.getName() + ", " + CR +
                    DIARIZATION + ": " + diarization.getName() + ", " + CR +
                    OPERATING_POINT + ": " + operatingPoint.getName() + ", " + CR +
                    ENABLE_ENTITIES + ": " + enableEntities + ", " + CR +
                    ADDITIONAL_VOCAB + ": " + Arrays.toString(additionalVocab);
        }

        public String toJsonString() {
            JSONObject json = new JSONObject();
            try {
                json.put(LANGUAGE, language.getCode());
                json.put(DIARIZATION, diarization.getCode());
                json.put(OPERATING_POINT, operatingPoint.getCode());
                json.put(ENABLE_ENTITIES, enableEntities);
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
                json.put(ENABLE_ENTITIES, enableEntities);
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
        public enum DiarizationRT {
            NONE("none", "нет", "none"),
            SPEAKER("speaker_change", "спикеры", "speakers");

            private final String name;
            private final String code;

            DiarizationRT(String code, String nameRu, String nameEn) {
                this.code = code;
                boolean langRu = Locale.getDefault().getLanguage().equals("ru");
                if(langRu) this.name = nameRu;
                else this.name = nameEn;
            }

            public String getName() {
                return name;
            }

            public static String[] getAllNames() {
                int length = DiarizationRT.values().length;
                String[] names = new String[length];
                for(int i = 0; i < length; i++) {
                    names[i] = DiarizationRT.values()[i].getName();
                }
                return names;
            }

            public String getCode() {
                return code;
            }

            public static String[] getAllCodes() {
                int length = DiarizationRT.values().length;
                String[] codes = new String[length];
                for(int i = 0; i < length; i++) {
                    codes[i] = DiarizationRT.values()[i].getCode();
                }
                return codes;
            }

            public static DiarizationRT getDiarization(String code) {
                DiarizationRT diar = DiarizationRT.NONE;
                for(DiarizationRT diarization: DiarizationRT.values()) {
                    if(diarization.getCode().equals(code)) {
                        diar = diarization;
                        break;
                    }
                }
                return diar;
            }

        }
    }
}
