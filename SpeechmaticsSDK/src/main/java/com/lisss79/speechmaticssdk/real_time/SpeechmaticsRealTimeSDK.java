package com.lisss79.speechmaticssdk.real_time;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.*;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lisss79.speechmaticssdk.common.Language;
import com.lisss79.speechmaticssdk.common.OperatingPoint;
import com.lisss79.speechmaticssdk.real_time.data.AddTranscript;
import com.lisss79.speechmaticssdk.real_time.data.StartRecognition;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tech.gusavila92.websocketclient.WebSocketClient;

public class SpeechmaticsRealTimeSDK {

    public final static int NO_DATA = -1;
    private final String ENDPOINT_URI = "eu2.rt.speechmatics.com";

    // Идентификаторы задач для handler'а
    private final int GET_TEMP_KEY = 10;
    private final int OPEN_CONNECTION = 11;
    private final int EXCEPTION_CONNECTION = 12;
    private final int CLOSE_CONNECTION = 13;
    private final int ERROR_MESSAGE = 14;
    private final int RECOGNITION_STARTED_MESSAGE = 15;
    private final int AUDIO_ADDED_MESSAGE = 16;
    private final int END_OF_TRANSCRIPT_MESSAGE = 17;
    private final int ADD_TRANSCRIPT_MESSAGE = 18;

    // Основные глобальные приватные переменные для работы SDK
    private int seqNo = 0;
    private boolean firstSymbol = true;                 // Первый ли символ сейчас
    private final StartRecognition startRecognition;
    private String AUTH_TOKEN = "";
    private final Context context;
    private final ExecutorService service;
    private HttpURLConnection connection;
    private WebSocketClient client;
    private final SpeechmaticsRealTimeListener listener;
    private final Handler uiHandler;

    // Значения по умолчанию для параметров, публичные
    public final static Language defLanguage = Language.RU;
    public final static StartRecognition.TranscriptionConfigRT.DiarizationRT defDiarizationRT = StartRecognition.TranscriptionConfigRT.DiarizationRT.NONE;
    public final static OperatingPoint defOperatingPoint = OperatingPoint.ENHANCED;
    public final static boolean defEnableEntities = false;
    public final static int defSampleRate = 44100;
    public final static StartRecognition.AudioFormat.Encoding defEncoding = StartRecognition.AudioFormat.Encoding.PCM_16;

    public SpeechmaticsRealTimeSDK(@NonNull Context context, String auth_token,
                                   @Nullable StartRecognition startRecognition,
                                   @NonNull SpeechmaticsRealTimeListener listener) {
        this.context = context;
        AUTH_TOKEN = auth_token;
        this.startRecognition = startRecognition;
        this.listener = listener;
        service = Executors.newCachedThreadPool();
        uiHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                sendToListener(msg);
            }
        };

    }

    private void sendToListener(Message msg) {
        int task = msg.arg1;
        switch(task) {
            case GET_TEMP_KEY:
                listener.onGetTempKeyFinished(msg.what, (String) msg.obj);
                break;
            case OPEN_CONNECTION:
                listener.onConnectionOpen();
                break;
            case EXCEPTION_CONNECTION:
                listener.onWebSocketException((String) msg.obj);
                break;
            case CLOSE_CONNECTION:
                listener.onConnectionClosed();
                break;
            case ERROR_MESSAGE:
                listener.onError((String) msg.obj);
                break;
            case RECOGNITION_STARTED_MESSAGE:
                listener.onRecognitionStarted((String) msg.obj);
                break;
            case AUDIO_ADDED_MESSAGE:
                listener.onAudioAdded((Integer) msg.obj);
                break;
            case END_OF_TRANSCRIPT_MESSAGE:
                listener.onEndOfTranscript();
                break;
            case ADD_TRANSCRIPT_MESSAGE:
                listener.onTranscriptAdded((String) msg.obj);
                break;
            default:
                break;
        }
    }

    /**
     * Запрашиваем временный ключ для работы в реальном времени.
     * По получении вызывается onGetTempKeyFinished
     */
    public void getTempKey() {
        URL url;
        try {
            String tempKeyUrl = "https://mp.speechmatics.com/v1/api_keys?type=rt";
            url = new URL(tempKeyUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        service.execute(() -> {

            Message msg = new Message();
            msg.arg1 = GET_TEMP_KEY;
            int responseCode = NO_DATA;

            try {
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + AUTH_TOKEN);
                connection.setRequestProperty("Content-Type", "application/json");
                String jsonInputString = "{\"ttl\": 60}";

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                connection.connect();
                responseCode = connection.getResponseCode();
                System.out.println("Code: " + responseCode);

                String response = getServerResponse(responseCode);
                String value = "";
                try {
                    JSONObject js = new JSONObject(response);
                    if(responseCode == HttpURLConnection.HTTP_OK ||
                            responseCode == HttpURLConnection.HTTP_CREATED)
                        value = js.getString(KEY_VALUE);
                    else value = js.getString(ERROR);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                msg.obj = value;

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                connection.disconnect();
            }

            msg.what = responseCode;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Послать сообщение об окончании передачи данных
     */
    public void sendEndOfStream() {
        JSONObject js = new JSONObject();
        try {
            js.put(MESSAGE, END_OF_STREAM);
            js.put(LAST_SEQ_NO, seqNo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        client.send(js.toString());
    }

    /**
     * Послать аудио на сервер для расшифровки
     */
    public void addAudio(byte[] data) {
        client.send(data);
        seqNo++;
    }

    /**
     * Создание соединения Web Socket
     * @param key временный ключ для авторизации
     */
    public void createConnection(String key) {
        String lang = startRecognition.getTranscriptionConfigRT().getLanguage().getCode();
        URI uri;
        try {
            uri = new URI(String.format(("wss://%s/v2/%s"), ENDPOINT_URI, lang));
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        createWebSocketClient(uri, key);
    }

    /**
     * Разорвать соединение Web Socket
     */
    public void closeConnection() {
        client.close();
    }

    /**
     * Создание клиента WebSocket, вспомогательный метод для createConnection
     * @param uri ссылка на endpoint
     * @param key временный ключ для авторизации
     */
    private void createWebSocketClient(URI uri, String key) {

        client = new WebSocketClient(uri) {
            @Override
            public void onOpen() {
                System.out.println("open");
                Message msg = new Message();
                msg.arg1 = OPEN_CONNECTION;
                uiHandler.sendMessage(msg);
            }

            @Override
            public void onTextReceived(String text) {
                System.out.println("received: " + text);
                String message = "";
                String extraInfo = "";
                try {
                    JSONObject js = new JSONObject(text);
                    message = js.getString(MESSAGE);
                    extraInfo = getExtraInfoFromMessage(js, message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                Message msg = new Message();

                if(message.equals(ERROR_MESSAGE_VALUE)) {
                    msg.arg1 = ERROR_MESSAGE;
                    msg.obj = extraInfo;
                    uiHandler.sendMessage(msg);
                }
                if(message.equals(RECOGNITION_STARTED)) {
                    seqNo = 0;
                    msg.arg1 = RECOGNITION_STARTED_MESSAGE;
                    msg.obj = extraInfo;
                    uiHandler.sendMessage(msg);
                }
                if(message.equals(AUDIO_ADDED)) {
                    msg.arg1 = AUDIO_ADDED_MESSAGE;
                    int seqNo = Integer.parseInt(extraInfo);
                    msg.obj = seqNo;
                    if (seqNo == 1) firstSymbol = true;
                    uiHandler.sendMessage(msg);
                }
                if(message.equals(END_OF_TRANSCRIPT)) {
                    msg.arg1 = END_OF_TRANSCRIPT_MESSAGE;
                    uiHandler.sendMessage(msg);
                }
                if(message.equals(ADD_TRANSCRIPT)) {
                    msg.arg1 = ADD_TRANSCRIPT_MESSAGE;
                    msg.obj = extraInfo;
                    uiHandler.sendMessage(msg);
                }
            }

            @Override
            public void onBinaryReceived(byte[] data) {

            }

            @Override
            public void onPingReceived(byte[] data) {

            }

            @Override
            public void onPongReceived(byte[] data) {

            }

            @Override
            public void onException(Exception e) {
                e.printStackTrace();
                //close();
                Message msg = new Message();
                msg.arg1 = EXCEPTION_CONNECTION;
                msg.obj = e.toString();
                uiHandler.sendMessage(msg);
            }

            @Override
            public void onCloseReceived() {
                System.out.println("close");
                Message msg = new Message();
                msg.arg1 = CLOSE_CONNECTION;
                uiHandler.sendMessage(msg);
            }
        };
        client.addHeader("Authorization", "Bearer " + key);
        client.setConnectTimeout(10000);
        client.setReadTimeout(60000);
        client.enableAutomaticReconnection(5000);
        client.connect();

    }

    /**
     * Получает дополнительную информацию из ответного сообщения, важную для пользователя
     * @param js сообщение сервера
     * @param message расшифрованный тип сообщения
     * @return информация для пользователя
     */
    private String getExtraInfoFromMessage(JSONObject js, final String message) throws JSONException {
        String response = "";
        switch(message) {
            case ERROR_MESSAGE_VALUE:
                response = js.getString(TYPE) + " (" + js.getString(REASON) + ")";
                break;
            case RECOGNITION_STARTED:
                response = js.getString(ID);
                break;
            case AUDIO_ADDED:
                response = js.getString(SEQ_NO);
                break;
            case ADD_TRANSCRIPT:
                AddTranscript as = new AddTranscript(js);
                response = getTranscriptWithSpeakers(as);
                // response = as.getTranscript();
                break;
        }
        return response;
    }

    /**
     * Возвращает расшифровку с обозначением смены спикера (если включена функция)
     * @param as AddTranscript, из которого извлекается текст
     * @return расшифровка
     */
    private String getTranscriptWithSpeakers(AddTranscript as) {
        boolean firstInLine = true;
        boolean spaceNeed = false;
        StringBuilder textBuilder = new StringBuilder();
        if(startRecognition.getTranscriptionConfigRT().getDiarization() ==
                StartRecognition.TranscriptionConfigRT.DiarizationRT.NONE)
            textBuilder.append(as.getTranscript());
        else {
            ArrayList<AddTranscript.Results> results = as.getResults();
            for(AddTranscript.Results result : results) {
                switch (result.getType()) {
                    case WORD:
                        if((spaceNeed || firstInLine) && !firstSymbol) textBuilder.append(" ");
                        spaceNeed = true;
                        firstSymbol = false;
                        textBuilder.append(result.getAlternatives().getContent());
                        break;
                    case PUNCTUATION:
                        spaceNeed = true;
                        firstSymbol = false;
                        textBuilder.append(result.getAlternatives().getContent());
                        break;
                    case SPEAKER_CHANGE:
                        spaceNeed = false;
                        textBuilder.append(CR).append("Speaker change").append(CR);
                        firstSymbol = true;
                        break;
                }
                firstInLine = false;
            }
        }
        return textBuilder.toString();
    }

    /**
     * Отправить сообщение о начале StartRecognition
     * @return успешно ли отправлено
     */
    public boolean sendStartRecognition() {
        if(client == null) return false;
        client.send(startRecognition.toJsonString());
        return true;
    }

    /**
     * Возвращает ответ сервера
     * @param responseCode код ответа от сервера
     * @return ответ сервера
     */
    @NonNull
    private String getServerResponse(int responseCode) {
        String CR = System.lineSeparator();
        String response = "";

        // Если код ответ сервера CREATED (для POST) или OK (для остальных), получить ответ
        if (responseCode == HttpURLConnection.HTTP_OK ||
                responseCode == HttpURLConnection.HTTP_CREATED) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder responseSB = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseSB.append(responseLine.trim()).append(CR);
                }
                if (responseSB.length() != 0) {
                    response = responseSB.toString();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Если другой код ответ сервера - получить текст ошибки
        else if (responseCode >= 400 && responseCode <= 500 && connection != null) {
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                StringBuilder responseSB = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    responseSB.append(responseLine.trim()).append(CR);
                }
                if (responseSB.length() != 0) {
                    response = responseSB.toString();
                } else {

                }
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        }

        // Иначе - ответа от сервера не было
        else {

        }
        return response;
    }

}
