package com.lisss79.speechmaticstranscription;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ADDITIONAL_VOCAB;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.DIARIZATION;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.ID;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.JOB_TYPE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.LANGUAGE;
import static com.lisss79.speechmaticssdk.common.JsonKeysValues.OPERATING_POINT;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.MenuCompat;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchListener;
import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.batch.data.JobConfig;
import com.lisss79.speechmaticssdk.batch.data.JobDetails;
import com.lisss79.speechmaticssdk.batch.data.SummaryStatistics;
import com.lisss79.speechmaticssdk.batch.statuses.FileStatus;
import com.lisss79.speechmaticssdk.batch.statuses.JobStatus;
import com.lisss79.speechmaticssdk.common.AdditionalVocab;
import com.lisss79.speechmaticssdk.common.Language;
import com.lisss79.speechmaticssdk.common.OperatingPoint;
import com.lisss79.speechmaticstranscription.databinding.ActivityMainBinding;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements SpeechmaticsBatchListener {

    // Глобальные неизменяемые переменные
    private static final String AUTH_TOKEN = "AUTH_TOKEN";
    private static final String FILE_URI = "FILE_URI";
    private static final String FILE_URL = "FILE_URL";
    private static final String JOB_ID = "JOB_ID";
    private static final String DIC_URI = "DIC_URI";
    private static final int REQUESTS_INTERVAL = 15000;
    private static final int INIT_INTERVAL = 100;
    private static final String RESTART_FLAG = "RESTART_FLAG";
    static final String VOC_PREFS_NAME = "vocabulary_preferences";

    // Переменные для уведомлений
    private final String[] NOTIFICATION_CHANNELS_ID = {
            "channel_sending", "channel_sent_ok", "channel_sent_error", "channel_job_done"
    };
    private static final int NOTIFICATION_SENDING_ID = 117;
    private static final int NOTIFICATION_SENT_OK = 118;
    private static final int NOTIFICATION_SENT_ERROR = 119;
    private static final int NOTIFICATION_JOB_DONE_ID = 120;
    private static final int SENDING_INDEX = 0;
    private static final int SENT_OK_INDEX = 1;
    private static final int SENT_ERROR_INDEX = 2;
    private static final int JOB_DONE_INDEX = 3;

    // Ключи для пунктов контекстного меню
    private static final int SAVE_FILE = 1;
    private static final int SHOW = 3;
    private static final int LOAD_TO_MEMORY = 4;

    // Ключи для запроса статистики
    private static final int SUMMARY_STATISTICS = 1;
    private static final int MONTHLY_STATISTICS = 2;

    // Ключи для определения состояния пользовательского словаря
    public static final int UNSPECIFIED = 0;
    public static final int DIC_ENABLED = 0b1;
    public static final int DIC_LOADED_FROM_EXTERNAL_FILE = 0b10;
    public static final int DIC_LOADED_FROM_PREFS = 0b100;

    private SpeechmaticsBatchSDK sm;
    private Logging log;
    private SharedPreferences prefs;
    private ActivityResultLauncher<String> fileSaverLauncher;
    private ActivityResultLauncher<String[]> filePickerLauncher;
    private ActivityResultLauncher<Intent> settingsLauncher;
    private ActivityMainBinding binding;
    private InfoScreenAdapter infoScreenAdapter;
    private final SummaryStatistics[] summaryStatistics = new SummaryStatistics[2];
    private final SummaryStatistics[] monthlyStatistics = new SummaryStatistics[2];
    private AdditionalVocab[] additionalVocabs = null;
    private Runnable refreshStatus;
    private AlertDialog dialogError;
    private TextView errorView;
    private ProgressBar readingProgressBar;
    private Menu optionsMenu;
    private TabLayout tabLayout;
    private NotificationManager notifyManager;
    private NotificationCompat.Builder[] notifyBuilder;
    private ObjectAnimator errorAnimator;

    // Runnable, который будет периодически выполняться,
    // пока все данные не будут полностью инициализированы
    // и переменные его стадий
    private Runnable initRunnable;
    // Константы для стадий инициализации
    private final int NO = 0;
    private final int IN_PROGRESS = 1;
    private final int YES = 2;
    // Получен ли токен и инициализирована SDK
    private int tokenGot = NO;
    // Получена ли статистика
    private int statisticsGot = NO;
    // Получены ли сохраненные данные о файле и работы
    private int savedDataGot = NO;
    // Получена ли расшифровка
    private int transcriptionGot = NO;
    // Обновлены ли данные на экране
    private int dataRefreshed = NO;

    private String text = "";
    private Uri fileUri;
    private String fileUrl;
    private String auth_token;
    private String idFromNotification;

    // Установлено ли успешное соединение
    private boolean successfullyConnected = false;

    // Выводить ли звук, если статус работы - выполнен
    private boolean needToInformWhenFinish = false;

    // Загружена ли в память расшифровка
    public boolean isTranscriptionLoaded = false;

    // Состояние пользовательского словаря
    public int userDic = UNSPECIFIED;

    @SuppressLint({"SourceLockedOrientationActivity", "ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // Инициализация хранилища, уведомлений, медиаплеера и лога
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        createNotificationChannels();
        notifyBuilder = getNotificationBuilders();
        log = new Logging(new File(
                getExternalFilesDir(null), "events_batch.log"), Logging.TYPE_BATCH);
        log.clear();
        log.start();

        getInfoFromIntent(getIntent());

        // Создание вкладок
        ViewPager2 pager = binding.pager;
        infoScreenAdapter =
                new InfoScreenAdapter(getSupportFragmentManager(), getLifecycle(), this);
        pager.setAdapter(infoScreenAdapter);
        tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, pager, (tab, position) -> {
            String tabLabel;
            if (position == 0) tabLabel = "Информация";
            else if (position == 1) tabLabel = "Статистика";
            else tabLabel = "";
            tab.setText(tabLabel);
        }
        ).attach();
        refreshStatus = this::refreshFragment;

        // Добавление view с ошибкой и progress bar'ом
        errorView = getErrorView();
        errorAnimator = ObjectAnimator.ofFloat(errorView, "rotationX", 0f, 360f);
        errorAnimator.setDuration(5000);
        errorAnimator.setRepeatCount(3);
        readingProgressBar = getReadingProgressBar();
        binding.mainLayout.addView(errorView);
        binding.mainLayout.addView(readingProgressBar);

        // Лончер настроек
        settingsLauncher =
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                    boolean restart = false;
                    if (result.getData() != null)
                        restart = result.getData().getBooleanExtra(RESTART_FLAG, false);
                    if (restart) {
                        restart();
                    }
                });

        // Лончер выбора аудиофайла для чтения
        filePickerLauncher =
                registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                    if (uri != null) {
                        binding.audioPlayer.reset();
                        fileUrl = "";
                        sm.setFileUrl(fileUrl);
                        sm.setFile(uri);
                        fileUri = uri;

                        // Предоставить постоянные права на чтение к Uri (в т.ч. после перезагрузки)
                        getContentResolver().
                                takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        refreshFragment();
                        putDataToPrefs();

                        // Выбран корректный файл
                        if (sm.fileStatus.equals(FileStatus.SELECTED)) {
                            binding.audioPlayer.setSourceUri(uri);
                            binding.audioPlayer.setVisibility(View.VISIBLE);
                            log.write("File with audio: " + uri + " selected");
                            if (successfullyConnected) {
                                optionsMenu.findItem(R.id.menuSubmitJob).setEnabled(true);
                                optionsMenu.findItem(R.id.menuSubmitJob2).setEnabled(true);
                            }
                        }

                        // Выбран некорректный файл
                        else {
                            binding.audioPlayer.setSourceUri(null);
                            binding.audioPlayer.setVisibility(View.INVISIBLE);
                            optionsMenu.findItem(R.id.menuSubmitJob).setEnabled(false);
                            optionsMenu.findItem(R.id.menuSubmitJob2).setEnabled(false);
                        }
                    }
                });

        // Лончер выбора файла для сохранения расшифровки
        fileSaverLauncher = registerForActivityResult
                (new ActivityResultContracts.CreateDocument("text/plain"), result -> {
                    if (result != null) {
                        log.write("File to save transcription: " + result + " selected");
                        SpeechmaticsBatchSDK.saveFile(this, result, text);
                    }
                });

        binding.fabRefresh.setOnClickListener(v -> {
            restart();
        });

        // Инициализирующий данные блок кода, запускающийся вначале
        initRunnable = () -> {

            // Читаем токен
            if (tokenGot == NO) {
                tokenGot = IN_PROGRESS;
                log.write("App is reading data...");
                readingProgressBar.setVisibility(View.VISIBLE);
                errorView.setVisibility(View.INVISIBLE);
                statisticsGot = NO;
                savedDataGot = NO;
                transcriptionGot = NO;
                dataRefreshed = NO;
                getTokenFromPrefs();
            }

            // Читаем статистику
            if (tokenGot == YES && statisticsGot == NO) {
                statisticsGot = IN_PROGRESS;
                sm.getStatistics(false, SUMMARY_STATISTICS);
            }

            // Читаем сохраненные данные
            if (tokenGot == YES && statisticsGot == YES && savedDataGot == NO) {
                savedDataGot = IN_PROGRESS;
                getDataFromPrefs();
            }

            // Получаем расшифровку
            if (tokenGot == YES && statisticsGot == YES && savedDataGot == YES
                    && transcriptionGot == NO) {
                transcriptionGot = IN_PROGRESS;
                putDataToPrefs();
                if (sm.jobType.equals(JobConfig.JobType.TRANSCRIPTION))
                    sm.getTheTranscript(sm.jobId, LOAD_TO_MEMORY);
            }

            // Обновляем данные на экране
            if (tokenGot == YES && statisticsGot == YES && savedDataGot == YES
                    && transcriptionGot == YES && dataRefreshed == NO) {
                dataRefreshed = IN_PROGRESS;
                refreshFragment();
                if (successfullyConnected) readingProgressBar.setVisibility(View.INVISIBLE);
            }
            if (dataRefreshed != YES)
                binding.mainLayout.postDelayed(initRunnable, INIT_INTERVAL);

                // Приложение запущено и данные прочитаны
            else {
                log.write("App is ready!");
                if (idFromNotification != null) {
                    notifyManager.cancelAll();
                    // Сразу показывается расшифровка, если было запущено по уведомлению и она доступна
                    if (idFromNotification.equals(sm.jobId) && isTranscriptionLoaded)
                        showTranscript();
                        // Сообщить, если расшифровка недоступна
                    else {
                        String errorText = String.format("Расшифровка для работы с id = %s недоступна",
                                idFromNotification);
                        InfoDialog noTranscript = new InfoDialog(this, "Ошибка",
                                InfoDialog.ERROR, errorText);
                        noTranscript.show();
                    }
                }
            }
        };
        binding.mainLayout.postDelayed(initRunnable, INIT_INTERVAL);
    }

    /**
     * Получение информации из вызывающего intent
     */
    private void getInfoFromIntent(Intent intent) {
        // Если приложение вызвано через "Открыть с помощью" и передан Uri файла
        Uri externalFileUri = intent.getData();
        if (externalFileUri != null && intent.getAction().equals(Intent.ACTION_VIEW)) {
            log.write("Got external uri: " + externalFileUri);
            log.write("Intent flags: " + intent.getFlags());
            Uri newFileUri = createCopyOfExternalFile(externalFileUri);
            if (newFileUri == null) newFileUri = Uri.EMPTY;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(FILE_URI, newFileUri.toString());
            editor.putString(FILE_URL, "");
            editor.apply();
        } else log.write("No external uri.");

        // Если приложение было вызвано по клику по уведомлению о завершенной работе
        idFromNotification = intent.getStringExtra(ID);
        if (idFromNotification != null) log.write("External job id = " + idFromNotification);
        else log.write("No external job id");
    }

    /**
     * Создает копию файла в локальной папке приложения и возвращает ссылку на него
     *
     * @param externalFileUri исходный файл
     * @return созданный файл
     */
    @Nullable
    private Uri createCopyOfExternalFile(Uri externalFileUri) {
        String fileName = "";
        Uri newFileUri = null;
        try (Cursor cursor = getContentResolver()
                .query(externalFileUri, null, null, null, null)) {
            int nameIndex = cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            fileName = cursor.getString(nameIndex);

            InputStream is = null;
            OutputStream os = null;
            try {
                is = getContentResolver().openInputStream(externalFileUri);
                File newFile = new File(getFilesDir(), fileName);
                os = new BufferedOutputStream(new FileOutputStream(newFile));

                byte[] buffer = new byte[1024];
                int lengthRead;
                while ((lengthRead = is.read(buffer)) > 0) {
                    os.write(buffer, 0, lengthRead);
                    os.flush();
                }
                newFileUri = FileProvider.getUriForFile(this,
                        "com.lisss79.fileprovider", newFile);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (is != null && os != null) {
                        is.close();
                        os.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return newFileUri;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        log.write("Got new intent!");
        getInfoFromIntent(intent);
        restart();
    }

    @Override
    protected void onDestroy() {
        notifyManager.cancel(NOTIFICATION_SENDING_ID);
        log.stop();
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.options_menu, menu);
        MenuCompat.setGroupDividerEnabled(menu, true);
        optionsMenu = menu;
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {

            // Выбрать файл
            case R.id.menuSelectFile:
                log.write("Trying to select audio file");
                String typeAudio = "audio/*";
                String typeVideo = "video/*";
                filePickerLauncher.launch(new String[]{typeAudio, typeVideo});
                break;

            // Ввести URL
            case R.id.menuSelectURL:
                log.write("Trying to input URL");
                getUrlFromUser();
                break;

            // Отправить на расшифровку
            case R.id.menuSubmitJob:
                if (sm.fileStatus.equals(FileStatus.SELECTED)) {
                    log.write("Trying to submit the job");

                    // Очищаем данные о предыдущей работе
                    sm.jobId = "";
                    sm.jobStatus = JobStatus.NONE;
                    putDataToPrefs();
                    isTranscriptionLoaded = false;

                    // Получаем конфигурацию работы из сохраненных настроек
                    JobConfig jobConfig = createJobConfigFromPrefs();
                    sm.jobType = jobConfig.getJobType();
                    log.write("The config of submitting job: " + jobConfig);

                    optionsMenu.findItem(R.id.menuGetTranscript).setEnabled(false);
                    optionsMenu.findItem(R.id.menuSaveTranscript).setEnabled(false);
                    refreshFragment();

                    // Отправляем работу
                    notifyBuilder[SENDING_INDEX].setProgress(100, 0, false);
                    notifyBuilder[SENDING_INDEX].setContentTitle("Отправка работы");
                    notifyManager.notify(NOTIFICATION_SENDING_ID, notifyBuilder[SENDING_INDEX].build());

                    readingProgressBar.setVisibility(View.VISIBLE);
                    sm.submitJob(fileUri, jobConfig);
                    Toast.makeText(this, "Работа отправляется...",
                            Toast.LENGTH_SHORT).show();
                } else if (sm.fileStatus.equals(FileStatus.NOT_SELECTED))
                    Toast.makeText(MainActivity.this,
                            "Файл не выбран", Toast.LENGTH_SHORT).show();
                else Toast.makeText(MainActivity.this,
                            "Выберите файл снова", Toast.LENGTH_SHORT).show();
                break;

            // Отправить на расшифровку 2
            case R.id.menuSubmitJob2:
                if (sm.fileStatus.equals(FileStatus.SELECTED)) {
                    log.write("Trying to submit the job 2");

                    // Очищаем данные о предыдущей работе
                    sm.jobId = "";
                    sm.jobStatus = JobStatus.NONE;
                    putDataToPrefs();
                    isTranscriptionLoaded = false;

                    // Получаем конфигурацию работы из сохраненных настроек
                    JobConfig jobConfig = createJobConfigFromPrefs();
                    sm.jobType = jobConfig.getJobType();
                    log.write("The config of submitting job: " + jobConfig);

                    optionsMenu.findItem(R.id.menuGetTranscript).setEnabled(false);
                    optionsMenu.findItem(R.id.menuSaveTranscript).setEnabled(false);
                    refreshFragment();

                    // Отправляем работу
                    notifyBuilder[SENDING_INDEX].setProgress(100, 0, false);
                    notifyBuilder[SENDING_INDEX].setContentTitle("Отправка работы");
                    notifyManager.notify(NOTIFICATION_SENDING_ID, notifyBuilder[SENDING_INDEX].build());

                    readingProgressBar.setVisibility(View.VISIBLE);
                    sm.submitJob2(fileUri, jobConfig);
                    Toast.makeText(this, "Работа отправляется...",
                            Toast.LENGTH_SHORT).show();
                } else if (sm.fileStatus.equals(FileStatus.NOT_SELECTED))
                    Toast.makeText(MainActivity.this,
                            "Файл не выбран", Toast.LENGTH_SHORT).show();
                else Toast.makeText(MainActivity.this,
                            "Выберите файл снова", Toast.LENGTH_SHORT).show();
                break;

            // Показать расшифровку
            case R.id.menuGetTranscript:
                log.write("Trying to show the transcription");
                if (sm.jobStatus.equals(JobStatus.DONE)) {
                    if (!isTranscriptionLoaded) {
                        if (sm.jobType.equals(JobConfig.JobType.TRANSCRIPTION))
                            sm.getTheTranscript(sm.jobId, SHOW);
                    } else showTranscript();
                } else Toast.makeText(MainActivity.this, "Работа не готова",
                        Toast.LENGTH_SHORT).show();
                break;

            // Сохранить расшифровку
            case R.id.menuSaveTranscript:
                log.write("Trying to save the job");
                if (sm.jobStatus.equals(JobStatus.DONE)) {
                    if (!isTranscriptionLoaded) {
                        if (sm.jobType.equals(JobConfig.JobType.TRANSCRIPTION))
                            sm.getTheTranscript(sm.jobId, SAVE_FILE);
                    } else saveTranscript();
                } else Toast.makeText(MainActivity.this, "Работа не готова",
                        Toast.LENGTH_SHORT).show();
                break;

            // Настройки
            case R.id.menuSettings:
                log.write("Trying to go to settings activity");
                Intent intentSettings = new Intent(this, SettingsActivity.class);
                settingsLauncher.launch(intentSettings);
                break;

            // Показать все работы
            case R.id.menuJobsList:
                log.write("Trying to go to jobs list activity");
                Intent intentListAllJobs = new Intent(this, JobsListActivity.class);
                DisplayMetrics dm = new DisplayMetrics();
                intentListAllJobs.putExtra(AUTH_TOKEN, auth_token);
                startActivity(intentListAllJobs);
                break;

            // Расшифровка с микрофона
            case R.id.menuRealTime:
                log.write("Trying to go to real time activity");
                Intent intentRealTime = new Intent(this, RealTimeActivity.class);
                intentRealTime.putExtra(AUTH_TOKEN, auth_token);
                startActivity(intentRealTime);
                break;

            // О программе
            case R.id.menuAbout:
                log.write("Trying to show about window");
                String text = String.format("Speechmatics Transcription %s\n" +
                                "Build %s\n(c)2023 Lisss79 Studio",
                        BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE);
                InfoDialog about = new InfoDialog(this, "О программе", InfoDialog.INFO,
                        text);
                about.show();
                break;

            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * Получение и проверка URL файла от пользователя
     */
    private void getUrlFromUser() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.text_request, null);
        builder.setView(dialogView).setCancelable(false).setTitle("URL файла")
                .setIcon(R.drawable.ic_warning).setNegativeButton("Отмена", null)
                .setPositiveButton("OK", (dialogInterface, i) -> {
                    EditText editText = dialogView.findViewById(R.id.editTextText);
                    String url = editText.getText().toString();
                    if (!url.isEmpty() && URLUtil.isValidUrl(url)) {
                        Handler handler = new Handler(getMainLooper()) {
                            @Override
                            public void handleMessage(@NonNull Message msg) {
                                super.handleMessage(msg);
                                if (msg.what == HttpURLConnection.HTTP_OK) {
                                    fileUrl = url;
                                    fileUri = Uri.EMPTY;
                                    boolean success = binding.audioPlayer.setSourceUri(Uri.parse(fileUrl));
                                    if (success) binding.audioPlayer.setVisibility(View.VISIBLE);
                                    else binding.audioPlayer.setVisibility(View.INVISIBLE);
                                    sm.setFileUrl(fileUrl);
                                    putDataToPrefs();
                                    refreshFragment();
                                } else {
                                    getUrlFromUser();
                                }
                            }
                        };
                        ExecutorService executor = Executors.newSingleThreadExecutor();
                        executor.execute(() -> {
                            try {
                                HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
                                int code = conn.getResponseCode();
                                handler.sendEmptyMessage(code);
                            } catch (IOException e) {
                                e.printStackTrace();
                                handler.sendEmptyMessage(0);
                            }
                        });
                    } else {
                        getUrlFromUser();
                    }
                });
        builder.show();
    }

    /**
     * Создает каналы для уведомлений
     */
    private void createNotificationChannels() {
        notifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        ArrayList<NotificationChannel> actualChannels = new ArrayList<>();

        // Канал уведомления об отправке работы
        NotificationChannel nc1 = getNotificationChannel(SENDING_INDEX, "Отправка работы",
                "Индикатор прогресса отправки работы на сервер в уведомлениях");
        new NotificationChannel(NOTIFICATION_CHANNELS_ID[SENDING_INDEX],
                "Отправка работы", NotificationManager.IMPORTANCE_DEFAULT);
        actualChannels.add(nc1);

        // Канал уведомления об успешной отправке работы
        NotificationChannel nc2 = getNotificationChannel(SENT_OK_INDEX,
                "Успешная отправка работы",
                "Уведомление об успешно отправленной работе");
        actualChannels.add(nc2);

        // Канал уведомления об ошибке при отправке работы
        NotificationChannel nc3 = getNotificationChannel(SENT_ERROR_INDEX,
                "Ошибка при отправке работы",
                "Уведомление об ошибке при отправке работе");
        actualChannels.add(nc3);

        // Канал уведомления об успешно выполненной работе
        NotificationChannel nc4 = getNotificationChannel(JOB_DONE_INDEX,
                "Работа выполнена",
                "Уведомление об успешно выполненной работе");
        actualChannels.add(nc4);

        // Создание актуальных каналов
        actualChannels.forEach(ac -> notifyManager.createNotificationChannel(ac));
        ArrayList<NotificationChannel> existingChannels =
                new ArrayList<>(notifyManager.getNotificationChannels());
        // Удаление каналов, имеющихся в системе, но больше не актуальных
        existingChannels.forEach(ec -> {
            if (actualChannels.stream().noneMatch(ac -> ac.getId().equals(ec.getId()))) {
                notifyManager.deleteNotificationChannel(ec.getId());
                System.out.println("Channel deleted: " + ec.getId());
            }
        });
    }

    /**
     * Возвращает канал уведомлений с заданными параметрами
     *
     * @param index       индекс массива с идентификаторами канала
     * @param name        имя канала
     * @param description описание канала
     * @return Notification Channel
     */
    private NotificationChannel getNotificationChannel(int index, String name, String description) {
        NotificationChannel nc = new NotificationChannel(NOTIFICATION_CHANNELS_ID[index],
                name, NotificationManager.IMPORTANCE_DEFAULT);
        nc.enableLights(true);
        nc.setLightColor(Color.MAGENTA);
        nc.enableVibration(true);
        nc.setDescription(description);
        return nc;
    }

    /**
     * Возвращает билдеры для уведомлений
     *
     * @return билдер
     */
    private NotificationCompat.Builder[] getNotificationBuilders() {
        NotificationCompat.Builder[] builders =
                new NotificationCompat.Builder[NOTIFICATION_CHANNELS_ID.length];
        builders[SENDING_INDEX] =
                getNotificationBuilder(SENDING_INDEX, "Отправка работы", R.drawable.ic_upload)
                        .setProgress(100, 0, false)
                        .setSilent(true);
        builders[SENT_OK_INDEX] =
                getNotificationBuilder(SENT_OK_INDEX, "Статус отправки", R.drawable.ic_info);
        builders[SENT_ERROR_INDEX] =
                getNotificationBuilder(SENT_ERROR_INDEX, "Статус отправки", R.drawable.ic_error);
        builders[JOB_DONE_INDEX] =
                getNotificationBuilder(JOB_DONE_INDEX, "Статус работы", R.drawable.ic_show_info);
        return builders;
    }

    /**
     * Возвращает билдер для уведомлений с заданными параметрами
     *
     * @param index   индекс массива с идентификаторами канала
     * @param title   заголовок
     * @param ic_icon ссылка на ресурс иконки
     * @return Notification Compat Builder
     */
    private NotificationCompat.Builder getNotificationBuilder(int index, String title, int ic_icon) {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNELS_ID[index])
                .setContentTitle(title)
                .setSmallIcon(ic_icon)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setAutoCancel(true);
    }

    /**
     * Перезапустить подключение
     */
    private void restart() {
        log.write("Trying to restart app to check the connection again");
        binding.audioPlayer.setSourceUri(null);
        binding.audioPlayer.setVisibility(View.INVISIBLE);
        tokenGot = NO;
        binding.mainLayout.postDelayed(initRunnable, INIT_INTERVAL);
    }

    /**
     * Получение конфигурации работы из сохраненных настроек
     *
     * @return конфигурация работы
     */
    private JobConfig createJobConfigFromPrefs() {
        AdditionalVocab[] av = additionalVocabs;
        JobConfig.JobType type = JobConfig.JobType.getJobType
                (prefs.getString(JOB_TYPE, SpeechmaticsBatchSDK.defJobType.getCode()));
        Language lang = Language.getLanguage
                (prefs.getString(LANGUAGE, SpeechmaticsBatchSDK.defLanguage.getCode()));
        JobConfig.TranscriptionConfig.Diarization diar = JobConfig.TranscriptionConfig.Diarization.getDiarization
                (prefs.getString(DIARIZATION, SpeechmaticsBatchSDK.defDiarization.getCode()));
        OperatingPoint op = OperatingPoint.getOperationPoint
                (prefs.getString(OPERATING_POINT, SpeechmaticsBatchSDK.defOperatingPoint.getCode()));
        return new JobConfig.Builder().jobType(type).url(fileUrl).additionalVocab(av)
                .operatingPoint(op).diarization(diar).language(lang).build();
    }

    /**
     * Возвращает пользовательский словарь
     *
     * @return пользовательский словарь
     */
    @Nullable
    private AdditionalVocab[] getUserVocabulary() {

        // Получаем настройку "использовать пользовательский словарь"
        userDic = UNSPECIFIED;
        boolean enable = prefs.getBoolean(ADDITIONAL_VOCAB, false);
        if (enable) userDic = DIC_ENABLED;

        // Получаем uri файла со словарем
        SharedPreferences vocPrefs = getSharedPreferences(VOC_PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = vocPrefs.edit();
        Uri dicUri = Uri.parse(prefs.getString(DIC_URI, ""));
        try (InputStream is = getContentResolver().openInputStream(dicUri)) {

            // Файл найден, читаем данные в отдельный shared preferences (vocPrefs)
            userDic = userDic | DIC_LOADED_FROM_EXTERNAL_FILE;
            BufferedReader br = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_16));
            editor.clear();
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                Map.Entry<String, TreeSet<String>> entry = getMapFromString(responseLine);
                if (entry != null) {
                    System.out.println(entry);
                    editor.putStringSet(entry.getKey(), entry.getValue());
                }
            }
            editor.apply();
        } catch (FileNotFoundException e) {
            // Файл с пользовательским словарем не найден
            e.printStackTrace();
        } catch (IOException e) {
            // Другая ошибка ввода-вывода
            e.printStackTrace();
            return null;
        }

        // Читаем данные из shared preferences
        AdditionalVocab[] voc = null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, TreeSet<String>> map = (Map<String, TreeSet<String>>) vocPrefs.getAll();
            if (map.isEmpty()) throw new Exception("No data in shared preferences");
            userDic = userDic | DIC_LOADED_FROM_PREFS;
            voc = AdditionalVocab.fromMap(map);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return enable ? voc : null;
    }

    /**
     * Преобразует строку файла в запись map
     * @param responseLine строка, прочитанная из файла
     * @return запись map
     */
    @Nullable
    private Map.Entry<String, TreeSet<String>> getMapFromString(String responseLine) {
        if (responseLine.isEmpty()) return null;
        String[] s = responseLine.split("=", 2);
        String key = s[0].trim();
        List<String> valuesList = new ArrayList<>();
        if(s.length > 1) {
            valuesList = Arrays.asList(s[1].split(","));
            valuesList.replaceAll(String::trim);
        }
        TreeSet<String> values = new TreeSet<>(valuesList);
        return Map.entry(key, values);
    }


    /**
     * Получить токен из SharedPreferences
     */
    private void getTokenFromPrefs() {
        auth_token = prefs.getString(AUTH_TOKEN, "");

        // Если токен не сохранен, запросить
        if (auth_token.isEmpty()) {
            log.write("No token in prefs");
            requestAuthToken();
        } else {
            log.write("Token got from prefs");
            sm = new SpeechmaticsBatchSDK(this, auth_token, this);
            // putTokenToPrefs();
            tokenGot = YES;
        }
    }

    /**
     * Запросить токен
     */
    private void requestAuthToken() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.text_request, null);
        builder.setView(dialogView).setCancelable(false).setTitle("Токен")
                .setIcon(R.drawable.ic_warning).setPositiveButton("OK", (dialogInterface, i) -> {
                    EditText editText = dialogView.findViewById(R.id.editTextText);
                    auth_token = editText.getText().toString();
                    if (!auth_token.isEmpty()) {
                        sm = new SpeechmaticsBatchSDK(this, auth_token, this);
                        putTokenToPrefs();
                        tokenGot = YES;
                    } else requestAuthToken();
                });
        builder.show();
    }

    /**
     * Получить данные из SharedPreferences
     */
    private void getDataFromPrefs() {
        log.write("Getting data from prefs");
        sm.jobId = prefs.getString(JOB_ID, "");
        fileUrl = prefs.getString(FILE_URL, "");
        String fileUriString = prefs.getString(FILE_URI, "");
        fileUri = Uri.parse(fileUriString);

        if (fileUrl.isEmpty()) sm.setFile(fileUri);
        else {
            sm.setFileUrl(fileUrl);
            if (successfullyConnected) sm.fileStatus = FileStatus.SELECTED;
            else sm.fileStatus = FileStatus.NOT_SELECTED;
        }
        if (sm.fileStatus.equals(FileStatus.SELECTED)) {
            if (!fileUriString.isEmpty()) {
                binding.audioPlayer.setSourceUri(fileUri);
                binding.audioPlayer.setVisibility(View.VISIBLE);
            } else {
                boolean success = binding.audioPlayer.setSourceUri(Uri.parse(fileUrl));
                if (success) binding.audioPlayer.setVisibility(View.VISIBLE);
                else binding.audioPlayer.setVisibility(View.INVISIBLE);
            }
            if (successfullyConnected) {
                optionsMenu.findItem(R.id.menuSubmitJob).setEnabled(true);
                optionsMenu.findItem(R.id.menuSubmitJob2).setEnabled(true);
            }

        } else {
            binding.audioPlayer.setSourceUri(null);
            binding.audioPlayer.setVisibility(View.INVISIBLE);
            optionsMenu.findItem(R.id.menuSubmitJob).setEnabled(false);
            optionsMenu.findItem(R.id.menuSubmitJob2).setEnabled(false);
        }
        sm.getJobDetails(sm.jobId);
        additionalVocabs = getUserVocabulary();
    }

    /**
     * Сохранить токен в SharedPreferences
     */
    private void putTokenToPrefs() {
        log.write("Putting token to prefs");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(AUTH_TOKEN, auth_token);
        editor.apply();
    }

    /**
     * Сохранить данные в SharedPreferences
     */
    private void putDataToPrefs() {
        log.write("Putting data to prefs");
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(JOB_ID, sm.jobId);
        editor.putString(FILE_URI, fileUri.toString());
        editor.putString(FILE_URL, fileUrl);
        editor.apply();
    }

    /**
     * Обновить данные, отображаемые на экране (во вкладках).
     * И запланировать повторный запуск через интервал, если работа в процессе выполнения.
     */
    @SuppressLint("UnspecifiedImmutableFlag")
    private void refreshFragment() {
        log.write("Refreshing data in main activity");
        StatusFragment statusFragment = (StatusFragment) infoScreenAdapter.statusFragment;
        StatisticsFragment statisticsFragment = (StatisticsFragment) infoScreenAdapter.statisticsFragment;
        statusFragment.refreshData(sm);

        // Обновление статистики, если необходимо
        if (summaryStatistics != null && monthlyStatistics != null) {
            statisticsFragment.refreshData(summaryStatistics, monthlyStatistics);
        }

        // Если работа имеет статус "в обработке", перезапросить статус через ХХХ секунд
        if (sm.jobStatus.equals(JobStatus.RUNNING)) {
            log.write("Wait for job finish...");
            needToInformWhenFinish = true;
            sm.getJobDetails(sm.jobId);
            binding.getRoot().removeCallbacks(refreshStatus);
            binding.getRoot().postDelayed(refreshStatus, REQUESTS_INTERVAL);
        }

        // Если статус не "в обработке", прекратить запросы статуса
        else {
            binding.getRoot().removeCallbacks(refreshStatus);

            // Если нужно информировать пользователя о статусе работы
            if (needToInformWhenFinish) {
                needToInformWhenFinish = false;
                int notificationId = NOTIFICATION_JOB_DONE_ID +
                        notifyManager.getActiveNotifications().length;
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra(ID, sm.jobId);
                PendingIntent pi = PendingIntent.getActivity(this, 0,
                        intent, PendingIntent.FLAG_UPDATE_CURRENT);
                String notifyText = "";

                // Если статус работы "выполнена"
                if (sm.jobStatus.equals(JobStatus.DONE)) {
                    notifyText = String.format("Работа с id = %s успешно выполнена", sm.jobId);
                    optionsMenu.findItem(R.id.menuGetTranscript).setEnabled(true);
                    optionsMenu.findItem(R.id.menuSaveTranscript).setEnabled(true);
                }

                // Если статус работы "отклонена"
                else if (sm.jobStatus.equals(JobStatus.REJECTED)) {
                    notifyText = String.format("Работа с id = %s отклонена сервером", sm.jobId);
                }

                // Если статус работы "неизвестно"
                else if (sm.jobStatus.equals(JobStatus.UNKNOWN)) {
                    notifyText = String.format("Статус работы с id = %s неизвестен. " +
                            "Запросите позже.", sm.jobId);
                }

                notifyManager.notify(notificationId, notifyBuilder[JOB_DONE_INDEX]
                        .setStyle(new NotificationCompat.BigTextStyle().bigText(notifyText))
                        .setContentIntent(pi).build());
            }
        }
        dataRefreshed = YES;
    }

    /**
     * Проверка подключения к серверу и отображение ошибки, если подключения нет
     * или оно неверное
     *
     * @param responseCode полученный ранее код ответа сервера
     */
    private void checkForConnectionErrors(int responseCode) {
        String errorText;
        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
            case HttpURLConnection.HTTP_CREATED:
                log.write("Connection successfully established");
                successfullyConnected = true;
                if (dialogError != null) {
                    dialogError.cancel();
                    dialogError = null;
                }
                errorView.setVisibility(View.INVISIBLE);
                optionsMenu.findItem(R.id.menuSubmitJob).setEnabled(true);
                optionsMenu.findItem(R.id.menuSubmitJob2).setEnabled(true);
                optionsMenu.findItem(R.id.menuJobsList).setEnabled(true);
                return;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                successfullyConnected = false;
                errorText = "Код %d. Ошибка авторизации.";
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
                successfullyConnected = false;
                errorText = "Код %d. Файл или работа не найдены.";
                break;
            case SpeechmaticsBatchSDK.NO_DATA:
                successfullyConnected = false;
                errorText = "Нет соединения с сервером.";
                break;
            default:
                successfullyConnected = false;
                errorText = "Код %d.";
                break;
        }

        readingProgressBar.setVisibility(View.INVISIBLE);
        errorView.setVisibility(View.VISIBLE);
        if (!errorAnimator.isRunning()) errorAnimator.start();
        log.write("Connection didn't establish");

        // Отключение пунктов меню "отправить работ" и "список работ", если нет соединения
        optionsMenu.findItem(R.id.menuSubmitJob).setEnabled(false);
        optionsMenu.findItem(R.id.menuSubmitJob2).setEnabled(false);
        optionsMenu.findItem(R.id.menuJobsList).setEnabled(false);
        if (dialogError == null) {
            dialogError = new InfoDialog(this, "Ошибка",
                    InfoDialog.ERROR, String.format(errorText, responseCode));
        } else {
            dialogError.setMessage(String.format(errorText, responseCode));
            if (!dialogError.isShowing()) dialogError.show();
        }
    }

    /**
     * Показ расшифровки на экране с возможностью копирования в буфер обмена
     */
    private void showTranscript() {
        InfoDialog showTranscriptDialog = new InfoDialog(this,
                "Расшифровка файла", InfoDialog.TRANSCRIPT, text, sm.fileName);
        showTranscriptDialog.show();
    }

    /**
     * Сохранение расшифровки в файл
     */
    private void saveTranscript() {
        fileSaverLauncher.launch("");
    }

    private TextView getErrorView() {
        TextView textView = new TextView(this);
        textView.setText("Нет соединения!!!");
        textView.setTextColor(Color.RED);
        textView.setTextSize(24);
        textView.setVisibility(View.INVISIBLE);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.
                LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToTop = R.id.audio_player;
        params.bottomMargin = 20;
        textView.setLayoutParams(params);
        return textView;
    }

    private ProgressBar getReadingProgressBar() {
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyle);
        bar.setVisibility(View.INVISIBLE);
        ConstraintLayout.LayoutParams params = new ConstraintLayout.
                LayoutParams(ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT);
        params.topToTop = ConstraintLayout.LayoutParams.PARENT_ID;
        params.bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID;
        params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID;
        params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID;
        params.verticalBias = 0.5f;
        bar.setLayoutParams(params);
        return bar;
    }

    @Override
    public void onAuthorizationCheckFinished(int responseCode) {
        log.write("Authorization response code: " + responseCode);
        log.write("Result: " + sm.getErrorMessage().toString());
    }

    @Override
    public void onGetJobDetailsFinished(int responseCode, JobDetails jobDetails) {
        log.write("Get a Job response code: " + responseCode);
        log.write("Get a Job values: " + jobDetails);
        log.write("Result: " + sm.getErrorMessage().toString());
        savedDataGot = YES;
    }

    @Override
    public void onGetAllJobsDetailsFinished(int responseCode, ArrayList<JobDetails> jobDetailsList) {
        log.write("Get All Jobs response code: " + responseCode);
        log.write("Get All Jobs values: " + jobDetailsList);
        log.write("Result: " + sm.getErrorMessage().toString());
    }

    @Override
    public void onGetTheTranscriptFinished(int responseCode, String response, int requestCode) {
        log.write("Get the Transcript response code: " + responseCode);
        String logText = response;
        if (response.length() > 100) logText = response.substring(0, 99) + "...";
        log.write("Get the Transcript text: " + logText);
        log.write("Result: " + sm.getErrorMessage().toString());
        if (requestCode == LOAD_TO_MEMORY) transcriptionGot = YES;
        if (responseCode == HttpURLConnection.HTTP_OK) {
            isTranscriptionLoaded = true;
            optionsMenu.findItem(R.id.menuGetTranscript).setEnabled(true);
            optionsMenu.findItem(R.id.menuSaveTranscript).setEnabled(true);
            text = response;
            switch (requestCode) {
                case SAVE_FILE:
                    Toast.makeText(this, "Расшифровка получена", Toast.LENGTH_SHORT).show();
                    refreshFragment();
                    saveTranscript();
                    break;
                case SHOW:
                    Toast.makeText(this, "Расшифровка получена", Toast.LENGTH_SHORT).show();
                    refreshFragment();
                    showTranscript();
                    break;
                case LOAD_TO_MEMORY:
                    break;
            }
        } else {
            isTranscriptionLoaded = false;
            optionsMenu.findItem(R.id.menuGetTranscript).setEnabled(false);
            optionsMenu.findItem(R.id.menuSaveTranscript).setEnabled(false);
            if (requestCode != LOAD_TO_MEMORY) {
                refreshFragment();
                Toast.makeText(this, "Расшифровка не получена", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDeleteJobFinished(int responseCode, JobDetails jobDetails) {
        log.write("Delete a Job response code: " + responseCode);
        log.write("Delete a Job values: " + jobDetails);
        log.write("Result: " + sm.getErrorMessage().toString());
    }

    @Override
    public void onGetStatisticsFinished(int responseCode, SummaryStatistics[] statistics, int requestCode) {
        log.write("Get Statistics response code: " + responseCode);
        log.write("Get Statistics data: " + Arrays.toString(statistics));
        log.write("Result: " + sm.getErrorMessage().toString());
        checkForConnectionErrors(responseCode);
        if (requestCode == SUMMARY_STATISTICS) {
            summaryStatistics[0] = statistics[0];
            summaryStatistics[1] = statistics[1];
            sm.getStatistics(true, MONTHLY_STATISTICS);
        } else {
            monthlyStatistics[0] = statistics[0];
            monthlyStatistics[1] = statistics[1];
            statisticsGot = YES;
        }
    }

    @Override
    @SuppressLint("UnspecifiedImmutableFlag")
    public void onSubmitJobFinished(int responseCode, String id, String extraInfo) {
        log.write("Submit Job response code: " + responseCode);
        log.write("Submit job id: " + id);
        log.write("Result: " + sm.getErrorMessage().toString());
        log.write("Debug info: " + extraInfo);
        readingProgressBar.setVisibility(View.INVISIBLE);
        notifyManager.cancel(NOTIFICATION_SENDING_ID);
        putDataToPrefs();

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_CREATED) {
            Toast.makeText(this, "Работа отправлена", Toast.LENGTH_SHORT).show();
            String notifyText = String.format("Работа с id = %s успешно отправлена", id);
            notifyManager.notify(NOTIFICATION_SENT_OK, notifyBuilder[SENT_OK_INDEX]
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notifyText))
                    .setContentIntent(pi).build());
        } else {
            Toast.makeText(this, "Ошибка отправки", Toast.LENGTH_SHORT).show();
            String notifyText = "";
            switch (responseCode) {
                case -1:
                    notifyText = String.format("Работу с id = %s отправить не удалось. " +
                            "Соединение прервано.", id);
                    break;
                case HttpURLConnection.HTTP_UNAUTHORIZED:
                    notifyText = String.format("Работу с id = %s отправить не удалось. " +
                            "Нет прав доступа к серверу. Проверьте токен.", id);
                    break;
                case HttpURLConnection.HTTP_FORBIDDEN:
                    notifyText = String.format("Работу с id = %s отправить не удалось. " +
                            "Возможно, превышен лимит доступного времени расшифровки.", id);
                    break;
                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    notifyText = String.format("Работу с id = %s отправить не удалось. " +
                            "Внутренняя ошибка сервера.", id);
                    break;
                default:
                    notifyText = String.format("Работу с id = %s отправить не удалось. " +
                            "Причина неизвестна.", id);
                    break;
            }


            notifyManager.notify(NOTIFICATION_SENT_ERROR, notifyBuilder[SENT_ERROR_INDEX]
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(notifyText))
                    .setContentIntent(pi).build());
        }
        refreshFragment();
    }

    @Override
    public void onJobSubmitting(int percent) {
        log.write("The job is sending: " + percent + "%");
        if (percent < 99) {
            notifyBuilder[SENDING_INDEX].setContentTitle("Работа отправляется")
                    .setProgress(100, percent, false);
        } else {
            notifyBuilder[SENDING_INDEX].setContentTitle("Ожидание ответа сервера")
                    .setProgress(100, percent, true);
        }
        notifyManager.notify(NOTIFICATION_SENDING_ID, notifyBuilder[SENDING_INDEX].build());
    }
}