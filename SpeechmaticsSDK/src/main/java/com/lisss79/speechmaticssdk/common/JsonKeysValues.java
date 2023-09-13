package com.lisss79.speechmaticssdk.common;

/**
 * Статические поля с ключами для расшифровки ответов сервера
 */
public class JsonKeysValues {

    // Ключи для обработки ответов и расшифровки аудио
    public final static String TRANSCRIPTION = "transcription";
    public final static String JOB_STATUS = "status";
    public final static String DATA_NAME = "data_name";
    public final static String CREATED_AT = "created_at";
    public final static String JOB = "job";
    public final static String JOBS = "jobs";
    public final static String DURATION = "duration";
    public final static String ID = "id";
    public final static String URL = "url";
    public final static String CODE = "code";
    public final static String ERROR = "error";
    public final static String DETAIL = "detail";
    public final static String ERRORS = "errors";
    public final static String TIMESTAMP = "timestamp";
    public final static String MESSAGE = "message";


    // Типы сообщений для реального времени
    public final static String RECOGNITION_STARTED = "RecognitionStarted";
    public final static String AUDIO_ADDED = "AudioAdded";
    public final static String ADD_TRANSCRIPT = "AddTranscript";
    public final static String END_OF_STREAM = "EndOfStream";
    public final static String END_OF_TRANSCRIPT = "EndOfTranscript";
    public final static String ERROR_MESSAGE_VALUE = "Error";
    public final static String SEQ_NO = "seq_no";
    public final static String LAST_SEQ_NO = "last_seq_no";
    public final static String TYPE = "type";
    public final static String KEY_VALUE = "key_value";
    public final static String REASON = "reason";
    public final static String METADATA = "metadata";
    public final static String RESULTS = "results";
    public final static String CONTENT = "content";
    public final static String ALTERNATIVES = "alternatives";
    public final static String TRANSCRIPT = "transcript";
    public final static String START_TIME = "start_time";
    public final static String END_TIME = "end_time";
    public final static String SOUNDS_LIKE = "sounds_like";



    // Для конфигурации
    public final static String TRANSCRIPTION_CONFIG = "transcription_config";
    public final static String FETCH_DATA = "fetch_data";
    public final static String ALIGNMENT_CONFIG = "alignment_config";
    public final static String ADDITIONAL_VOCAB = "additional_vocab";
    public final static String CONFIG = "config";
    public final static String LANGUAGE = "language";
    public final static String OPERATING_POINT = "operating_point";
    public final static String JOB_TYPE = "type";
    public final static String DIARIZATION = "diarization";
    public final static String ENABLE_ENTITIES = "enable_entities";
    public final static String AUDIO_FORMAT = "audio_format";
    public final static String AUDIO_TYPE = "type";
    public final static String ENCODING = "encoding";
    public final static String SAMPLE_RATE = "sample_rate";

    // Для статистики
    public final static String SUMMARY = "summary";
    public final static String DETAILS = "details";
    public final static String DURATION_HOURS = "duration_hrs";
    public final static String COUNT = "count";
    public final static String SINCE = "since";
    public final static String UNTIL = "until";
    public final static String JOB_MODE = "mode";

    // Для форматирования времени
    public final static String USER_CREATED_PATTERN = "d MMM yy, HH:mm:ss";
    public final static String CR = System.lineSeparator();

}
