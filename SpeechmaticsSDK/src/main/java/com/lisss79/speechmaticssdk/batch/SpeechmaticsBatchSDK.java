package com.lisss79.speechmaticssdk.batch;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.DETAILS;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ID;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.JOBS;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.lisss79.speechmaticssdk.ContentUriRequestBody;
import com.lisss79.speechmaticssdk.batch.data.ErrorMessage;
import com.lisss79.speechmaticssdk.batch.data.JobConfig;
import com.lisss79.speechmaticssdk.batch.data.JobDetails;
import com.lisss79.speechmaticssdk.batch.data.SummaryStatistics;
import com.lisss79.speechmaticssdk.batch.statuses.FileStatus;
import com.lisss79.speechmaticssdk.batch.statuses.JobStatus;
import com.lisss79.speechmaticssdk.common.Language;
import com.lisss79.speechmaticssdk.common.OperatingPoint;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Основной класс для взаимодействия с API сервера Speechmatics,
 * работа с файлами
 */
public class SpeechmaticsBatchSDK {

    // Идентификаторы задач для handler'а
    private final int CHECK_AUTHORIZATION = 10;
    private final int GET_ALL_JOBS_DETAILS = 11;
    private final int GET_JOB_DETAILS = 12;
    private final int DELETE_JOB = 13;
    private static final int GET_STATISTICS = 14;
    private static final int GET_THE_TRANSCRIPT = 15;
    private static final int SUBMIT_JOB = 17;
    private static final int JOB_SUBMITTING = 18;

    // Константы для http запросов
    private final String ENDPOINT_URI = "asr.api.speechmatics.com";
    public final static int NO_DATA = -1;
    private final String TWO_HYPHENS = "--";
    private final String CR = System.lineSeparator();
    private static String AUTH_TOKEN = "";
    private final static String GET = "GET";
    private final static String DEL = "DELETE";
    private final static int TIMEOUT = 8000;

    // Url для запросов
    private String baseUrl;
    private String statUrl;

    // Значения по умолчанию для параметров, публичные
    public final static Language defLanguage = Language.RU;
    public final static JobConfig.TranscriptionConfig.Diarization defDiarization = JobConfig.TranscriptionConfig.Diarization.NONE;
    public final static OperatingPoint defOperatingPoint = OperatingPoint.ENHANCED;
    public final static JobConfig.JobType defJobType = JobConfig.JobType.TRANSCRIPTION;

    // Ключи для форматирования ответов
    private final static String DATE_ISO8601_PATTERN = "yyyy-MM-dd";
    private static String USER_DURATION_PATTERN_SECOND;
    private static String USER_DURATION_PATTERN_MINUTE;
    private static String USER_DURATION_PATTERN_HOUR;
    private static String USER_DURATION_HOUR_PATTERN_HOUR;
    private static String USER_DURATION_HOUR_PATTERN_DAY;

    // Приватные глобальные переменные для работы sdk
    private HttpURLConnection connection;
    private final ExecutorService service;
    private final Handler uiHandler;
    private final SpeechmaticsBatchListener listener;
    private final Context context;
    private ErrorMessage errorMessage = new ErrorMessage();
    private String extraInfo = "";

    //Публичные глобальные переменные - данные аудиофайла и работы
    public String fileName = "";
    public String fileUrl = "";
    public int fileSize = NO_DATA;
    public String jobId = "";
    public JobConfig.JobType jobType = defJobType;
    public JobStatus jobStatus = JobStatus.NONE;
    public FileStatus fileStatus = FileStatus.NOT_SELECTED;
    public int duration = 0;

    // Поддерживаемые расширения файлов
    private final String[] supportedExtension =
            {"wav", "mp3", "aac", "ogg", "mpeg", "amr", "m4a", "mp3", "flac"};
    // Максимальная длина файла
    private final int maxFileSize = 1024 * 1024 * 1024;

    /**
     * Пустой конструктор для тестов
     */
    public SpeechmaticsBatchSDK() {
        service = null;
        listener = null;
        context = null;
        uiHandler = null;
    }

    /**
     * Конструктор
     *
     * @param auth_token токен для авторизации
     * @param listener   интерфейс для получения ответов
     */
    public SpeechmaticsBatchSDK(@NonNull Context context,
                                String auth_token, @NonNull SpeechmaticsBatchListener listener) {
        this.context = context;
        AUTH_TOKEN = auth_token;
        this.listener = listener;
        baseUrl = String.format(("https://%s/v2/jobs"), ENDPOINT_URI);
        statUrl = String.format(("https://%s/v2/usage"), ENDPOINT_URI);

        service = Executors.newCachedThreadPool();
        uiHandler = new Handler(context.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                sendToListener(msg);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private void sendToListener(@NonNull Message msg) {
        int task = msg.arg1;
        switch (task) {
            case CHECK_AUTHORIZATION:
                listener.onAuthorizationCheckFinished(msg.what);
                break;
            case GET_ALL_JOBS_DETAILS:
                listener.onGetAllJobsDetailsFinished(msg.what, (ArrayList<JobDetails>) msg.obj);
                break;
            case GET_JOB_DETAILS:
                listener.onGetJobDetailsFinished(msg.what, (JobDetails) msg.obj);
                break;
            case DELETE_JOB:
                listener.onDeleteJobFinished(msg.what, (JobDetails) msg.obj);
                break;
            case GET_STATISTICS:
                listener.onGetStatisticsFinished(msg.what, (SummaryStatistics[]) msg.obj, msg.arg2);
                break;
            case GET_THE_TRANSCRIPT:
                listener.onGetTheTranscriptFinished(msg.what, (String) msg.obj, msg.arg2);
                break;
            case SUBMIT_JOB:
                listener.onSubmitJobFinished(msg.what, (String) msg.obj, extraInfo);
                break;
            case JOB_SUBMITTING:
                listener.onJobSubmitting(msg.arg2);
                break;
            default:
                break;
        }

    }

    /**
     * Проверка корректности авторизации.
     * По окончании - вызов onAuthorizationCheckFinished.
     */
    public void checkAuthorization() {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = CHECK_AUTHORIZATION;
            int responseCode = doQuery(baseUrl, GET);
            msg.what = responseCode;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Отправка файла на сервер через HttpUrlConnection для расшифровки:
     * выполнение запроса POST и отправление данных из файла.
     * По окончании - вызов onSubmitJobFinished.
     *
     * @param uri       ссылка на локальный файл с данными,
     *                  игнорируется, если в конфигурации работы есть url
     * @param jobConfig конфигурация работы
     */
    public void submitJob(@Nullable Uri uri, @Nullable JobConfig jobConfig) {

        // Создание конфигурации по умолчанию в случае null или использование заданной
        JobConfig jc;
        if (jobConfig != null) jc = jobConfig;
        else jc = new JobConfig();

        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = SUBMIT_JOB;
            Message submittingMsg;
            int responseCode = NO_DATA;
            extraInfo = "";
            String data;
            InputStream is = null;
            InputStream errorStream = null;
            DataOutputStream os = null;

            // Случайный разделитель разделов тела запроса POST
            String boundary = UUID.randomUUID().toString();

            // Если uri файла корректно или есть url в fetch_data.
            // Приоритет у url. Если есть url - передаем его
            if (setFile(uri) || !jc.getFetchData().getUrl().isEmpty()) {

                try {
                    URL requestURL = new URL(baseUrl);

                    // Начало процесса, 0%
                    submittingMsg = getProgressMessage(0);
                    uiHandler.sendMessage(submittingMsg);

                    // Создание запроса и добавление авторизации
                    connection = (HttpsURLConnection) requestURL.openConnection(Proxy.NO_PROXY);
                    connection.setReadTimeout(TIMEOUT);
                    connection.setConnectTimeout(TIMEOUT);
                    connection.setUseCaches(false);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Authorization", "Bearer " + AUTH_TOKEN);
                    // Указание типа данных и разделителя
                    connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                    // Получение исходящего потока для отправления тела запроса
                    connection.setDoOutput(true);
                    os = new DataOutputStream(connection.getOutputStream());

                    // Если отсутствует url файла, открываем поток из uri
                    if (jc.getFetchData().getUrl().isEmpty()) {

                        // Начало процесса, 0%
                        submittingMsg = getProgressMessage(0);
                        uiHandler.sendMessage(submittingMsg);

                        is = context.getContentResolver().openInputStream(uri);

                        // Начало тела запроса
                        os.writeBytes(TWO_HYPHENS + boundary + CR);
                        // Заголовок раздела с файлом. Сюда вписываем имя файла
                        String con_dis_file = String.format
                                ("Content-Disposition: form-data; name=\"data_file\"; filename=\"%s\"", fileName);
                        String con_type_file = "Content-Type: application/octet-stream";
                        os.writeBytes(con_dis_file + CR);
                        os.writeBytes(con_type_file + CR + CR);

                        // Считываем байт из входящего потока (файл) и записываем в исходящий (тело http запроса)
                        int j = 0;

                        int step = (int) Math.ceil(fileSize / 9f);
                        int numOfBytes;
                        byte[] nextBytes = new byte[step];
                        do {
                            numOfBytes = is.read(nextBytes);
                            if(numOfBytes > 0) os.write(nextBytes, 0, numOfBytes);
                            if(j < 90) j += 10;
                            else j = 99;
                            System.out.println("Step: " + step + ", numOfBytes: " + numOfBytes + ", j: " + j);
                            submittingMsg = getProgressMessage(j);
                            uiHandler.sendMessage(submittingMsg);
                        } while (numOfBytes >= step);

                        os.writeBytes(CR);
                    }

                    // Заголовок раздела конфигурации
                    os.writeBytes(TWO_HYPHENS + boundary + CR);
                    String con_dis_config = "Content-Disposition: form-data; name=\"config\"";
                    String con_config = jc.toJsonString();

                    os.writeBytes(con_dis_config + CR + CR);
                    os.writeBytes(con_config + CR);
                    os.writeBytes(TWO_HYPHENS + boundary + TWO_HYPHENS + CR);
                    // Конец тела запроса
                    os.flush();
                    os.close();

                    // Ожидаем ответ сервера
                    submittingMsg = getProgressMessage(99);
                    uiHandler.sendMessage(submittingMsg);
                    connection.connect();
                    System.out.println("Connection= " + connection);
                    extraInfo = "Connection: " + connection + "." + CR;
                    responseCode = connection.getResponseCode();
                    extraInfo = "Code: " + responseCode + "." + CR;

                    errorStream = connection.getErrorStream();
                    if (errorStream != null) {
                        extraInfo += "Error stream: " + CR;
                        BufferedReader reader = new BufferedReader(new InputStreamReader(errorStream));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            extraInfo += (line + CR);
                        }
                    } else extraInfo += "No error stream." + CR;

                } catch (IOException e) {
                    e.printStackTrace();
                    extraInfo += "Main IOError: " + e.getMessage() + "." + CR;
                } finally {
                    try {
                        if (is != null) is.close();
                        if (os != null) {
                            os.flush();
                            os.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        extraInfo += "Close IOError: " + e.getMessage() + "." + CR;
                    }
                }

                System.out.println("Response Code= " + responseCode);

                String response = getServerResponse(responseCode);
                extraInfo += "Server Response: ";

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    data = "";
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        data = jsonObject.getString(ID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    jobId = data;
                    jobType = jc.getJobType();
                    msg.obj = data;
                    jobStatus = JobStatus.RUNNING;
                    fileStatus = FileStatus.SENT;
                } else {
                    fileStatus = FileStatus.SENDING_ERROR;
                }

                // Окончание процесса, 100%
                submittingMsg = getProgressMessage(100);
                uiHandler.sendMessage(submittingMsg);

            } else fileStatus = FileStatus.SENDING_ERROR;

            msg.what = responseCode;
            uiHandler.sendMessage(msg);
        });
    }


    /**
     * Отправка файла на сервер для расшифровки через okhttp:
     * выполнение запроса POST и отправление данных из файла.
     * По окончании - вызов onSubmitJobFinished.
     *
     * @param uri       ссылка на локальный файл с данными,
     *                  игнорируется, если в конфигурации работы есть url
     * @param jobConfig конфигурация работы
     */
    public void submitJob2(@Nullable Uri uri, @Nullable JobConfig jobConfig) {

        // Создание конфигурации по умолчанию в случае null или использвание заданной
        JobConfig jc;
        if (jobConfig != null) jc = jobConfig;
        else jc = new JobConfig();

        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = SUBMIT_JOB;
            Message submittingMsg;
            int responseCode = NO_DATA;
            String response = "";
            extraInfo = "";
            String data;

            // Если uri файла корректно или есть url в fetch_data.
            // Приоритет у url. Если есть url - передаем его
            if (setFile(uri) || !jc.getFetchData().getUrl().isEmpty()) {

                OkHttpClient client = new OkHttpClient();
                RequestBody body;

                // Начало процесса, 0%
                submittingMsg = getProgressMessage(0);
                uiHandler.sendMessage(submittingMsg);

                // Если отсутствует url файла, добавляем в тело запроса поток из uri
                if (jc.getFetchData().getUrl().isEmpty()) {
                    RequestBody uriRequestBody =
                            new ContentUriRequestBody(context.getContentResolver(), uri, fileSize,
                                    progress -> {
                                        Message submittingMsg1 = getProgressMessage(progress);
                                        uiHandler.sendMessage(submittingMsg1);
                                    });
                    body = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("data_file", fileName, uriRequestBody)
                            .addFormDataPart("config", jc.toJsonString())
                            .build();
                } else {

                    // Если есть url файла, в тело не добавляем данные из файла
                    body = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("config", jc.toJsonString())
                            .build();
                }

                Request request = new Request.Builder()
                        .addHeader("Authorization", "Bearer " + AUTH_TOKEN)
                        .post(body)
                        .url(baseUrl)
                        .build();

                // Отправляем запрос и ожидаем ответа
                try (Response serverResponse = client.newCall(request).execute()) {

                    responseCode = serverResponse.code();
                    if (serverResponse.body() != null) response = serverResponse.body().string();
                    if (!serverResponse.isSuccessful()) {
                        throw new IOException("Запрос к серверу не был успешен (Submit a job):" +
                                " " + responseCode + " " + response);
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                }

                extraInfo = "Code: " + responseCode + "." + CR;
                System.out.println("Response Code= " + responseCode);
                extraInfo += "Server Response: " + response;

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    data = "";
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        data = jsonObject.getString(ID);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    jobId = data;
                    jobType = jc.getJobType();
                    msg.obj = data;
                    jobStatus = JobStatus.RUNNING;
                    fileStatus = FileStatus.SENT;
                } else {
                    fileStatus = FileStatus.SENDING_ERROR;
                }

                // Окончание процесса, 100%
                submittingMsg = getProgressMessage(100);
                uiHandler.sendMessage(submittingMsg);

            } else fileStatus = FileStatus.SENDING_ERROR;

            msg.what = responseCode;
            uiHandler.sendMessage(msg);
        });

    }


    private Message getProgressMessage(int percent) {
        Message message = new Message();
        message.arg1 = JOB_SUBMITTING;
        message.arg2 = percent;
        return message;
    }

    /**
     * Удаление работы с сервера.
     * По окончании - вызов onDeleteJobFinished.
     *
     * @param id идентификатор работы
     */
    public void deleteJob(@NonNull String id) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = DELETE_JOB;
            JobDetails jobDetails;
            String urlWithId = baseUrl + "/" + id;
            int responseCode = doQuery(urlWithId, DEL);
            String response = getServerResponse(responseCode);
            msg.what = responseCode;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                jobDetails = new JobDetails(response);
                msg.obj = jobDetails;
            }
            uiHandler.sendMessage(msg);
        });
    }


    /**
     * Получение деталей работы с сервера.
     * По окончании - вызов onGetJobDetailsFinished.
     *
     * @param id идентификатор работы
     */
    public void getJobDetails(@NonNull String id) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_JOB_DETAILS;
            JobDetails jobDetails;
            int responseCode;
            String response = "";
            if (!id.isEmpty()) {
                String urlWithId = baseUrl + "/" + id;
                responseCode = doQuery(urlWithId, GET);
                response = getServerResponse(responseCode);
            } else {
                responseCode = HttpURLConnection.HTTP_NOT_FOUND;
                errorMessage = new ErrorMessage();
                errorMessage.setCode(responseCode);
                errorMessage.setError("Id is empty");
            }
            msg.what = responseCode;
            if (responseCode == HttpURLConnection.HTTP_OK) {
                jobDetails = new JobDetails(response);
                msg.obj = jobDetails;
                if (id.equals(jobId)) {
                    jobStatus = jobDetails.getStatus();
                    jobType = jobDetails.getJobConfig().getJobType();
                }
            }
            if (responseCode == NO_DATA && id.equals(jobId))
                jobStatus = JobStatus.UNKNOWN;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Получения деталей всех работ с сервера.
     * По окончании - вызов onGetAllJobsDetailsFinished.
     */
    public void getAllJobsDetails(boolean includeDeleted) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_ALL_JOBS_DETAILS;
            JobDetails jobDetails;
            ArrayList<JobDetails> jobDetailsList = new ArrayList<>();
            String urlAllJobs = baseUrl + "?include_deleted=" + includeDeleted + "&limit=100";
            int responseCode = doQuery(urlAllJobs, GET);
            msg.what = responseCode;
            String response = getServerResponse(responseCode);
            if (responseCode == HttpURLConnection.HTTP_OK) {
                JSONArray jsonArray;
                try {
                    jsonArray = new JSONObject(response).getJSONArray(JOBS);

                    // Перебираем все работы в массиве и добавляем в список
                    for (int i = 0; i < jsonArray.length(); i++) {
                        jobDetails = new JobDetails(jsonArray.getJSONObject(i));
                        jobDetailsList.add(jobDetails);
                    }
                    msg.obj = jobDetailsList;
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            System.out.println(urlAllJobs);
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Получение общей статистики.
     * По окончании - вызов onGetStatisticsFinished.
     *
     * @param monthly true - за месяц, false - за весь срок
     */
    public void getStatistics(boolean monthly, int requestCode) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_STATISTICS;
            int countStandart = 0;
            float duration_hoursStandart = 0;
            int countEnhanced = 0;
            float duration_hoursEnhanced = 0;
            String url = statUrl;
            if (monthly) {
                Calendar date = Calendar.getInstance();
                date.set(Calendar.DAY_OF_MONTH, 1);
                String formattedDate = new SimpleDateFormat
                        (DATE_ISO8601_PATTERN, Locale.getDefault()).format(date.getTime());
                url = statUrl + "?since=" + formattedDate;
            }
            int responseCode = doQuery(url, GET);
            msg.what = responseCode;
            String response = getServerResponse(responseCode);
            System.out.println(response);

            if (responseCode == HttpURLConnection.HTTP_OK) {
                JSONArray jsonArray;
                JSONObject jsonObject;
                SummaryStatistics summaryStatistics;
                try {
                    jsonArray = new JSONObject(response).getJSONArray(DETAILS);
                    for (int i = 0; i < jsonArray.length(); i++) {
                        jsonObject = jsonArray.getJSONObject(i);
                        summaryStatistics = new SummaryStatistics(jsonObject);
                        JobConfig.JobType type = summaryStatistics.getType();
                        OperatingPoint op = summaryStatistics.getOperatingPoint();
                        if (type.equals(JobConfig.JobType.TRANSCRIPTION) && op.equals(OperatingPoint.STANDARD)) {
                            countStandart += summaryStatistics.getCount();
                            duration_hoursStandart += summaryStatistics.getDuration_hrs();
                        }
                        if (type.equals(JobConfig.JobType.TRANSCRIPTION) && op.equals(OperatingPoint.ENHANCED)) {
                            countEnhanced += summaryStatistics.getCount();
                            duration_hoursEnhanced += summaryStatistics.getDuration_hrs();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            SummaryStatistics dataStandard = new SummaryStatistics();
            dataStandard.setType(JobConfig.JobType.TRANSCRIPTION);
            dataStandard.setOperatingPoint(OperatingPoint.STANDARD);
            dataStandard.setCount(countStandart);
            dataStandard.setDuration_hrs(duration_hoursStandart);
            SummaryStatistics dataEnhanced = new SummaryStatistics();
            dataEnhanced.setType(JobConfig.JobType.TRANSCRIPTION);
            dataEnhanced.setOperatingPoint(OperatingPoint.ENHANCED);
            dataEnhanced.setCount(countEnhanced);
            dataEnhanced.setDuration_hrs(duration_hoursEnhanced);

            msg.obj = new SummaryStatistics[]{dataStandard, dataEnhanced};
            msg.arg2 = requestCode;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Получение расшифровки с сервера.
     * По окончании - вызов onGetTheTranscriptFinished.
     *
     * @param id          идентификатор работы
     * @param requestCode код запроса, передается в ответ для идентификации
     */
    public void getTheTranscript(@NonNull String id, int requestCode) {
        service.execute(() -> {
            Message msg = new Message();
            msg.arg1 = GET_THE_TRANSCRIPT;
            String urlForTranscript = baseUrl + "/" + id + "/transcript?format=txt";
            int responseCode = HttpURLConnection.HTTP_NOT_FOUND;
            String response = "";
            if (!id.isEmpty()) {
                responseCode = doQuery(urlForTranscript, GET);
                response = getServerResponse(responseCode);
            } else {
                errorMessage = new ErrorMessage();
                errorMessage.setCode(responseCode);
                errorMessage.setError("Id is empty");
            }
            msg.what = responseCode;
            msg.obj = response;
            msg.arg2 = requestCode;
            uiHandler.sendMessage(msg);
        });
    }

    /**
     * Выполняет запрос
     *
     * @param url   адрес для запроса
     * @param query - запрос (GET, DELETE)
     * @return код ответа
     */
    private int doQuery(String url, String query) {
        int responseCode = NO_DATA;
        try {
            URL requestURL = new URL(url);
            connection = (HttpURLConnection) requestURL.openConnection();
            connection.setConnectTimeout(TIMEOUT);
            connection.setReadTimeout(TIMEOUT);
            connection.setRequestMethod(query);
            connection.setRequestProperty("Authorization", "Bearer " + AUTH_TOKEN);
            connection.connect();
            responseCode = connection.getResponseCode();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return responseCode;
    }

    /**
     * Возвращает ответ сервера
     *
     * @param responseCode код ответа от сервера
     * @return ответ сервера
     */
    @NonNull
    private String getServerResponse(int responseCode) {
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
                errorMessage = new ErrorMessage();
                errorMessage.setCode(responseCode);
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
                    errorMessage = new ErrorMessage(response);
                } else {
                    errorMessage = new ErrorMessage();
                    errorMessage.setCode(responseCode);
                }
            } catch (NullPointerException | IOException e) {
                e.printStackTrace();
            }
        }

        // Иначе - ответа от сервера не было
        else {
            errorMessage = new ErrorMessage();
            errorMessage.setCode(NO_DATA);
        }

        return response;
    }

    /**
     * Сохранение текста расшифровки в файл
     *
     * @param uri  ссылка на содержимое файла
     * @param text текст, который пишется в файл
     * @return успешно ли записан текст в файл
     */
    public static boolean saveFile(@NonNull Context context,
                                   @Nullable Uri uri, @Nullable String text) {
        boolean result = false;

        // Проверка на пустой или null Uri
        if (uri == null) return false;
        if (uri.toString().isEmpty()) return false;

        // Проверка на тип файла
        String mimeType = context.getContentResolver().getType(uri);
        if (!mimeType.startsWith("text") || text == null) {
            return false;
        }

        try (OutputStream os = context.getContentResolver().openOutputStream(uri, "wt")) {
            byte[] tempBytesAudio = text.getBytes(StandardCharsets.UTF_16);
            int tempFileSize = tempBytesAudio.length;
            os.write(tempBytesAudio, 0, tempFileSize);
            os.flush();
            result = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Передача URL файла
     *
     * @param url ссылка на файл
     */
    public void setFileUrl(@Nullable String url) {
        if (url != null) {
            fileUrl = url;
            fileStatus = FileStatus.SELECTED;
        } else {
            fileUrl = "";
            fileStatus = FileStatus.NOT_SELECTED;
        }
    }

    /**
     * Передача файла для дальнейшей обработки и проверка на корректность
     *
     * @param uri ссылка на содержимое файла
     * @return успешно ли прочитаны данные из файла
     */
    public boolean setFile(@Nullable Uri uri) {
        fileSize = 0;

        // Проверка на пустой или null Uri
        if (uri == null) return false;
        if (uri.toString().isEmpty()) return false;

        // Проверка на тип файла
        String mimeType = context.getContentResolver().getType(uri);
        if (mimeType == null) {
            fileName = uri.getLastPathSegment();
            fileStatus = FileStatus.LOADING_ERROR;
            return false;
        }
        if (!mimeType.startsWith("audio") && !mimeType.startsWith("video")) {
            fileName = uri.getLastPathSegment();
            fileStatus = FileStatus.WRONG_FILE_TYPE;
            return false;
        }

        try {

            // Получение имени и длинны файла из Uri
            try (Cursor cursor = context.getContentResolver()
                    .query(uri, null, null, null, null)) {
                int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
                int sizeIndex = cursor.getColumnIndexOrThrow(OpenableColumns.SIZE);
                cursor.moveToFirst();
                fileName = cursor.getString(nameIndex);
                long tempFileSizeL = cursor.getLong(sizeIndex);

                // Проверка на поддерживаемое расширение и длину файла
                boolean ok = false;
                if (tempFileSizeL < maxFileSize) {
                    ok = true;
                    fileSize = (int) tempFileSizeL;
                }
                String ext = fileName.
                        substring(fileName.lastIndexOf(".") + 1).toLowerCase();
                for (String e : supportedExtension) {
                    if (ext.equals(e)) {
                        ok = true;
                        break;
                    }
                }
                if (!ok) {
                    fileStatus = FileStatus.WRONG_FILE_TYPE;
                    return false;
                }

            }
            // Если не удалось прочитать данные из Uri
            catch (SecurityException e) {
                e.printStackTrace();
                fileName = uri.getLastPathSegment();
                fileStatus = FileStatus.LOADING_ERROR;
                return false;
            }

            // Получение данных из файла в массив, если данные доступны
            if (fileSize > 0) {

                // Получение продолжительности аудиофайла (если тип не подходит - ошибка)
                String durationStr = "";
                MediaMetadataRetriever mmr = null;
                try {
                    mmr = new MediaMetadataRetriever();
                    mmr.setDataSource(context, uri);
                    durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                    try {
                        duration = Integer.parseInt(durationStr) / 1000;
                        fileStatus = FileStatus.SELECTED;
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        duration = 0;
                        fileStatus = FileStatus.LOADING_ERROR;
                        return false;
                    }
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    duration = 0;
                    fileStatus = FileStatus.LOADING_ERROR;
                    return false;
                }
                finally {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            mmr.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

            } else {
                duration = 0;
                fileStatus = FileStatus.LOADING_ERROR;
                return false;
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            fileStatus = FileStatus.LOADING_ERROR;
            return false;
        }
        fileStatus = FileStatus.SELECTED;
        return true;
    }

    /**
     * Возвращает последнюю ошибку, полученную от сервера
     *
     * @return класс ошибки
     */
    public ErrorMessage getErrorMessage() {
        return errorMessage;
    }

    /**
     * Инициализация значений ключей для статических методов в зависимости от языка
     */
    private static void initKeys() {
        boolean langRu = Locale.getDefault().getLanguage().equals("ru");
        String DAY_SYMBOL;
        String HOUR_SYMBOL;
        String MINUTE_SYMBOL;
        String SECOND_SYMBOL;
        if (langRu) {
            DAY_SYMBOL = "дн";
            HOUR_SYMBOL = "ч";
            MINUTE_SYMBOL = "м";
            SECOND_SYMBOL = "с";
        } else {
            DAY_SYMBOL = "d";
            HOUR_SYMBOL = "h";
            MINUTE_SYMBOL = "m";
            SECOND_SYMBOL = "s";
        }
        USER_DURATION_PATTERN_SECOND = "%02d" + SECOND_SYMBOL;
        USER_DURATION_PATTERN_MINUTE = "%02d" + MINUTE_SYMBOL + " " + USER_DURATION_PATTERN_SECOND;
        USER_DURATION_PATTERN_HOUR = "%d" + HOUR_SYMBOL + " " + USER_DURATION_PATTERN_MINUTE;
        USER_DURATION_HOUR_PATTERN_HOUR = "%02d" + HOUR_SYMBOL + " " + USER_DURATION_PATTERN_MINUTE;
        USER_DURATION_HOUR_PATTERN_DAY = "%d" + DAY_SYMBOL + " " + USER_DURATION_HOUR_PATTERN_HOUR;
    }

    /**
     * Преобразует полученное от сервера значение общей длительности записей в часах
     * в удобный формат
     *
     * @param durationHrs ответ от сервера в текстовом формате
     * @return строка, удобная для чтения пользователем
     */
    @SuppressLint("DefaultLocale")
    public static String durationHoursToString(String durationHrs) {
        initKeys();
        String result = durationHrs != null ? durationHrs : "";
        try {
            Objects.requireNonNull(durationHrs);
            float dur = Float.parseFloat(durationHrs) * 3600;
            if (dur < 0) dur = 0;
            int d = (int) Math.floor(dur / 86400);
            int h = (int) Math.floor((dur - d * 86400) / 3600);
            int m = (int) (Math.floor((dur - d * 86400 - h * 3600) / 60));
            int s = (int) (dur - d * 86400 - h * 3600 - m * 60);
            if (dur < 60) result = String.format(USER_DURATION_PATTERN_SECOND, s);
            else if (dur < 3600) result = String.format(USER_DURATION_PATTERN_MINUTE, m, s);
            else if (dur < 86400) result = String.format(USER_DURATION_HOUR_PATTERN_HOUR, h, m, s);
            else result = String.format(USER_DURATION_HOUR_PATTERN_DAY, d, h, m, s);
        } catch (NumberFormatException | NullPointerException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * Преобразует полученное от сервера значение длительности записи c секундах
     * в удобный формат
     *
     * @param duration ответ от сервера в текстовом формате
     * @return строка, удобная для чтения пользователем
     */
    @SuppressLint("DefaultLocale")
    public static String durationToString(String duration) {
        initKeys();
        String result = duration != null ? duration : "";
        try {
            Objects.requireNonNull(duration);
            float dur = Float.parseFloat(duration);
            if (dur < 0) dur = 0;
            int h = (int) Math.floor(dur / 3600);
            int m = (int) (Math.floor((dur - h * 3600) / 60));
            int s = (int) (dur - h * 3600 - m * 60);
            if (dur < 60) result = String.format(USER_DURATION_PATTERN_SECOND, s);
            else if (dur < 3600) result = String.format(USER_DURATION_PATTERN_MINUTE, m, s);
            else result = String.format(USER_DURATION_PATTERN_HOUR, h, m, s);
        } catch (NumberFormatException | NullPointerException e) {
            e.printStackTrace();
        }
        return result;
    }

}
