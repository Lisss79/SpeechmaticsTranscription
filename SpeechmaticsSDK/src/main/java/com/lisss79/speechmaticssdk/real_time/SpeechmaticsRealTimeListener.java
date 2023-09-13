package com.lisss79.speechmaticssdk.real_time;

/**
 * Интерфейс с callback'ами, вызываемыми по завершении обращений к серверу
 */
public interface SpeechmaticsRealTimeListener {

    /**
     * Callback после получения временного ключа
     * @param responseCode ответ сервера, 200 - успешно
     * @param tempKey полученный временный ключ
     */
    void onGetTempKeyFinished(int responseCode, String tempKey);

    /**
     * Callback после установки соединения
     */
    void onConnectionOpen();

    /**
     * Callback после разрыва соединения
     */
    void onConnectionClosed();

    /**
     * Callback как только сервер готов принимать raw аудио
     * @param iD идентификатор созданного подключения
     */
    void onRecognitionStarted(String iD);

    /**
     * Callback когда добавлено аудио
     * @param seqNo номер полученного файла в последовательности
     */
    void onAudioAdded(int seqNo);

    /**
     * Callback при сообщении об ошибке,
     * пришедшем от сервера в ответном сообщении
     * @param errorText текст ошибки
     */
    void onError(String errorText);

    /**
     * Callback по окончании расшифровки
     */
    void onEndOfTranscript();

    /**
     * Callback по получении расшифровки
     * @param transcript текст расшифровки
     */
    void onTranscriptAdded(String transcript);

    /**
     * Callback по получении исключения в соединении WebSocket
     */
    void onWebSocketException(String errorText);
}
