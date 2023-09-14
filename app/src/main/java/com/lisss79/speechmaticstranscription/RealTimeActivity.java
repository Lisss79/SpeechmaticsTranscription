package com.lisss79.speechmaticstranscription;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ADDITIONAL_VOCAB;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.DIARIZATION;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.LANGUAGE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.OPERATING_POINT;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.lisss79.speechmaticssdk.common.AdditionalVocab;
import com.lisss79.speechmaticssdk.common.Language;
import com.lisss79.speechmaticssdk.common.OperatingPoint;
import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.batch.data.JobConfig;
import com.lisss79.speechmaticssdk.real_time.MicRecord;
import com.lisss79.speechmaticssdk.real_time.statuses.RealTimeStatus;
import com.lisss79.speechmaticssdk.real_time.SpeechmaticsRealTimeListener;
import com.lisss79.speechmaticssdk.real_time.SpeechmaticsRealTimeSDK;
import com.lisss79.speechmaticssdk.real_time.data.StartRecognition;
import com.lisss79.speechmaticstranscription.databinding.ActivityRealTimeBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RealTimeActivity extends AppCompatActivity implements SpeechmaticsRealTimeListener {

    private static final String AUTH_TOKEN = "AUTH_TOKEN";
    private static final String VOC_PREFS_NAME = "vocabulary_preferences";
    private ActivityRealTimeBinding binding;
    private SpeechmaticsRealTimeSDK sm;
    private ActivityResultLauncher<String> fileSaverLauncher;
    private MicRecord mr;
    private StartRecognition startRecognition;
    private Runnable timerRunnable;
    private ByteBuffer recordingBuffer;
    private File recordingFile;
    private SharedPreferences prefs;
    private ObjectAnimator recAnimator;
    private Logging log;
    private RealTimeStatus status;

    // Распознанных текст (в том числе в текущий момент)
    private String transcript = "";

    // true, если идет процесс распознавания (а также подготовки и завершения)
    private boolean isListening;

    // true, если идет процесс завершения распознавания (отправка оставшихся данных)
    private boolean isFlushed;

    // Общее время распознавания, сек
    private int recSeconds = 0;

    // Длинна куска записи, отправляемого на сервер для распознавания, сек
    int lengthOfPiece = 10;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRealTimeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        status = RealTimeStatus.NO_STATUS;
        isListening = false;
        isFlushed = false;
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        log = new Logging(new File(
                getExternalFilesDir(null), "events_rt.log"), Logging.TYPE_REAL_TIME);
        log.clear();
        log.start();

        startRecognition = createStartRecognition();

        // Обработка нажатия кнопки старт/стоп
        binding.buttonStartRT.setOnClickListener(v -> {
            if (!isListening) {
                // Запуск распознавания
                status = RealTimeStatus.CONNECTION;
                showStatus();
                transcript = "";
                isFlushed = false;
                binding.textViewTranscription.setText(transcript);
                recSeconds = 0;
                binding.textViewTimer.setText(getFormattedTime(recSeconds));
                isListening = true;
                sm.getTempKey();
            } else {
                // Остановка распознавания
                status = RealTimeStatus.FINISHING;
                showStatus();
                flushRecording();
                mr.stop();
                isListening = false;
                sm.sendEndOfStream();
            }
        });

        // Обработка нажатия кнопки сохранить
        binding.buttonSaveRT.setOnClickListener(v -> {
            if(transcript.isEmpty()) {
                Toast.makeText(this, "Нечего сохранять", Toast.LENGTH_LONG).show();
            } else {
                fileSaverLauncher.launch("");
            }
        });

        // Обработка нажатия на текст
        binding.textViewTranscription.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", transcript);
            clipboard.setPrimaryClip(clip);
            log.write("Transcription copied to clipboard");
            Toast.makeText(this, "Данные скопированы", Toast.LENGTH_LONG).show();
        });

        // Лончер выбора файла для сохранения расшифровки
        fileSaverLauncher = registerForActivityResult
                (new ActivityResultContracts.CreateDocument("text/plain"), result -> {
                    if (result != null) {
                        log.write("File to save transcription: " + result + " selected");
                        SpeechmaticsBatchSDK.saveFile(this, result, transcript);
                    }
                });

        recAnimator = ObjectAnimator.ofFloat(binding.textViewStatusRT, "alpha", 1f, 0f, 1f);
        recAnimator.setDuration(1000);
        recAnimator.setRepeatCount(ValueAnimator.INFINITE);
        recAnimator.setRepeatMode(ValueAnimator.REVERSE);
        showStatus();
        binding.textViewTimer.setText(getFormattedTime(recSeconds));

        Intent data = getIntent();
        String auth_token = "";
        if (data != null) auth_token = data.getStringExtra(AUTH_TOKEN);
        sm = new SpeechmaticsRealTimeSDK(this, auth_token, startRecognition, this);
        checkMicPermission();

    }

    @Override
    protected void onDestroy() {
        log.stop();
        super.onDestroy();
    }

    /**
     * Получение данных, сохраненных в Shared Preferences
     * и создание конфигурации для расшифровки на их основе
     */
    private StartRecognition createStartRecognition() {
        SharedPreferences vocPrefs = getSharedPreferences(VOC_PREFS_NAME, MODE_PRIVATE);
        StartRecognition.Builder builder = new StartRecognition.Builder();
        try {
            lengthOfPiece = prefs.getInt("length_piece", 10);
        } catch (ClassCastException | NumberFormatException e) {
            e.printStackTrace();
        }
        Language lang = Language.getLanguage
                (prefs.getString(LANGUAGE, SpeechmaticsRealTimeSDK.defLanguage.getCode()));
        JobConfig.TranscriptionConfig.Diarization diar =
                JobConfig.TranscriptionConfig.Diarization.getDiarization
                (prefs.getString(DIARIZATION, SpeechmaticsBatchSDK.defDiarization.getCode()));
        StartRecognition.TranscriptionConfigRT.DiarizationRT diarRT;
        if(diar == JobConfig.TranscriptionConfig.Diarization.NONE) diarRT =
                StartRecognition.TranscriptionConfigRT.DiarizationRT.NONE;
        else diarRT = StartRecognition.TranscriptionConfigRT.DiarizationRT.SPEAKER;
        OperatingPoint op = OperatingPoint.getOperationPoint
                (prefs.getString(OPERATING_POINT, SpeechmaticsRealTimeSDK.defOperatingPoint.getCode()));
        boolean userVoc = prefs.getBoolean(ADDITIONAL_VOCAB, false);
        AdditionalVocab[] addVoc = null;
        if (userVoc) {
            try {
                Map<String, TreeSet<String>> map = (Map<String, TreeSet<String>>) vocPrefs.getAll();
                if (map.isEmpty()) throw new Exception("No data in shared preferences");
                addVoc = AdditionalVocab.fromMap(map);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        StartRecognition sr = builder.diarization(diarRT).operatingPoint(op)
                .language(lang).additionalVocab(addVoc).build();
        log.write("Created start recognition (transcription config):");
        log.write(sr.toString());
        return sr;
    }

    /**
     * Проверка разрешения на доступ к микрофону
     */
    private void checkMicPermission() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            log.write("Permission granted");
            mr = MicRecord.getInstance(startRecognition);
            status = RealTimeStatus.READY;
            showStatus();
        } else {
            ActivityCompat.requestPermissions
                    (this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult
            (int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Необходим доступ к микрофону!", Toast.LENGTH_SHORT).show();
            status = RealTimeStatus.NO_PERMISSION;
            showStatus();
        } else checkMicPermission();
    }

    /**
     * Показать статус и при необходимости Progress Bar
     */
    private void showStatus() {
        log.write("Status has changed to " + status.getName());
        binding.textViewStatusRT.setText(status.getName());
        binding.textViewStatusRT.setTextColor(status.getColor());
        String buttonStartText;
        boolean buttonStartEnabled;
        boolean showProgressBar;
        switch (status) {
            case NO_STATUS:
            case READY:
            case CONNECTION_ERROR:
            case RUNTIME_ERROR:
                buttonStartText = "Начать";
                buttonStartEnabled = true;
                showProgressBar = false;
                break;
            case WORKING:
                buttonStartText = "Стоп";
                buttonStartEnabled = true;
                showProgressBar = false;
                break;
            case FINISHING:
                buttonStartText = "Стоп";
                buttonStartEnabled = false;
                showProgressBar = true;
                break;
            case CONNECTION:
                buttonStartText = "Начать";
                buttonStartEnabled = false;
                showProgressBar = true;
                break;
            case NO_PERMISSION:
                buttonStartText = "Начать";
                buttonStartEnabled = false;
                showProgressBar = false;
                break;
            default:
                buttonStartText = "Начать";
                buttonStartEnabled = true;
                showProgressBar = false;
                break;
        }
        binding.buttonStartRT.setText(buttonStartText);
        binding.buttonStartRT.setEnabled(buttonStartEnabled);
        if (showProgressBar) {
            binding.progressBarRec.setVisibility(View.VISIBLE);
        } else {
            binding.progressBarRec.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onGetTempKeyFinished(int responseCode, String tempKey) {
        System.out.println("Code: " + responseCode);
        String errorText = "";
        if (tempKey != null && (responseCode == HttpURLConnection.HTTP_OK
                || responseCode == HttpURLConnection.HTTP_CREATED)) {
            log.write("Got temporary key: " + tempKey);
            System.out.println(tempKey);
            sm.createConnection(tempKey);
            return;
        }
        else if(responseCode == SpeechmaticsRealTimeSDK.NO_DATA) {
            log.write("Connection error");
            status = RealTimeStatus.CONNECTION_ERROR;
            errorText = "Невозможно установить соединение с сервером";
        } else {
            log.write("Runtime error #" + responseCode);
            status = RealTimeStatus.RUNTIME_ERROR;
            errorText = "Код: " + responseCode + " (" + tempKey + ")";
        }
            showStatus();
            recAnimator.cancel();
            System.out.println("Error: " + responseCode);
            isListening = false;
            InfoDialog errorDialog = new InfoDialog(this,
                    "Ошибка", InfoDialog.ERROR, errorText);
            errorDialog.show();
    }

    @Override
    public void onRecognitionStarted(String extraInfo) {
        log.write("Recognition started with length of piece " + lengthOfPiece + "sec");
        System.out.println("Recognition Started");
        status = RealTimeStatus.WORKING;
        showStatus();
        recAnimator.start();
        mr.startRecording();

        timerRunnable = () -> {
            binding.textViewTimer.setText(getFormattedTime(recSeconds));
            recSeconds++;
            if (isListening) binding.getRoot().postDelayed(timerRunnable, 1000);
        };
        binding.getRoot().postDelayed(timerRunnable, 1000);

        ExecutorService service = Executors.newCachedThreadPool();
        service.execute(() -> {
            while (isListening) {
                recordingFile = new File(getExternalFilesDir(null), "recording.pcm");
                int bufferSize = MicRecord.BUFFER_SIZE;
                int numOfReadings = mr.getBytesPerSecond() * lengthOfPiece / bufferSize;
                recordingBuffer = ByteBuffer.allocateDirect(bufferSize * numOfReadings);
                final ByteBuffer tempBuffer = ByteBuffer.allocateDirect(bufferSize);
                while (numOfReadings > 0) {
                    mr.read(tempBuffer, bufferSize);
                    recordingBuffer.put(tempBuffer);
                    tempBuffer.clear();
                    numOfReadings--;
                }

                if (!isFlushed) {
                    try (final FileOutputStream outStream = new FileOutputStream(recordingFile, false)) {
                        outStream.write(recordingBuffer.array(), 0, recordingBuffer.array().length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println("Adding after 10 seconds");
                    sm.addAudio(recordingBuffer.array());
                }

            }
        });
    }

    private void flushRecording() {
        log.write("Finishing recognition");
        isFlushed = true;
        try (final FileOutputStream outStream = new FileOutputStream(recordingFile, false)) {
            outStream.write(recordingBuffer.array(), 0, recordingBuffer.array().length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Adding from flush");
        sm.addAudio(recordingBuffer.array());
    }

    private String getFormattedTime(int seconds) {
        return DateUtils.formatElapsedTime(seconds);
    }

    @Override
    public void onAudioAdded(int seqNo) {
        log.write("Got audio #" + seqNo + " added message");
        System.out.println("Audio Added No:" + seqNo);
    }

    @Override
    public void onError(String errorText) {
        log.write("Got error message: " + errorText);
        recAnimator.cancel();
        System.out.println("Error: " + errorText);
        status = RealTimeStatus.RUNTIME_ERROR;
        showStatus();
        isListening = false;
        InfoDialog showTranscriptDialog = new InfoDialog(this,
                "Ошибка", InfoDialog.ERROR, errorText);
        showTranscriptDialog.show();
    }

    @Override
    public void onEndOfTranscript() {
        log.write("Got end of transcript message");
        System.out.println("End of Transcript");
    }

    @Override
    public void onTranscriptAdded(String text) {
        log.write("Got transcript added message: \"" + text + "\"");
        System.out.println("Add transcript: " + text);
        transcript += text;
        binding.textViewTranscription.setText(transcript);
    }

    @Override
    public void onWebSocketException(String errorText) {
        log.write("Caught web socket exception: " + errorText);
        recAnimator.cancel();
        System.out.println("Error: " + errorText);
        status = RealTimeStatus.RUNTIME_ERROR;
        showStatus();
        isListening = false;
        InfoDialog showTranscriptDialog = new InfoDialog(this,
                "Ошибка", InfoDialog.ERROR, errorText);
        showTranscriptDialog.show();
        sm.closeConnection();
    }

    @Override
    public void onConnectionOpen() {
        log.write("Connection is open");
        sm.sendStartRecognition();
    }

    @Override
    public void onConnectionClosed() {
        log.write("Connection is closed");
        System.out.println("Connection closed");
        recAnimator.cancel();
        status = RealTimeStatus.READY;
        showStatus();
        isListening = false;
    }
}