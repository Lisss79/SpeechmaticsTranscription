package com.lisss79.speechmaticstranscription;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchListener;
import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.batch.data.JobConfig;
import com.lisss79.speechmaticssdk.batch.data.JobDetails;
import com.lisss79.speechmaticssdk.batch.data.SummaryStatistics;

import java.net.HttpURLConnection;
import java.util.ArrayList;

public class JobsListActivity extends AppCompatActivity
        implements SpeechmaticsBatchListener, ItemClickedListener {

    private static final String AUTH_TOKEN = "AUTH_TOKEN";

    // Ключи для пунктов контекстного меню
    private static final int SAVE_FILE = 1;
    private static final int SHOW = 2;

    private ActivityResultLauncher<String> fileSaverLauncher;
    private JobsListAdapter adapter;
    private RecyclerView recyclerView;
    private LinearLayoutManager layoutManager;
    private SpeechmaticsBatchSDK sm;
    private String text;
    boolean includeDeleted = false;

    // Выбранный пункт списка для контекстного меню
    private int position;
    private String fileName;

    // Верхний отображаемый элемент
    private int firstPosition;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_jobs_list);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            if (prefs.contains("show_deleted")) includeDeleted =
                    prefs.getBoolean("show_deleted", false);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }

        fileSaverLauncher = registerForActivityResult
                (new ActivityResultContracts.CreateDocument("text/plain"), result -> {
                    if (result != null) {
                        System.out.println(result);
                        SpeechmaticsBatchSDK.saveFile(this, result, text);
                    }
                });

        firstPosition = 0;
        recyclerView = findViewById(R.id.recyclerViewJobsList);
        Intent data = getIntent();
        String auth_token = "";
        if (data != null) auth_token = data.getStringExtra(AUTH_TOKEN);
        sm = new SpeechmaticsBatchSDK(this, auth_token, this);
        sm.getAllJobsDetails(includeDeleted);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
        position = Integer.parseInt(v.getTag().toString());
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        JobDetails jobDetails = adapter.getJobDetails(position);
        String id = jobDetails.getId();
        JobConfig.JobType jobType = jobDetails.getJobConfig().getJobType();
        switch (item.getItemId()) {
            case R.id.menuShowDetails:
                sm.getJobDetails(id);
                return true;
            case R.id.menuShowTranscript:
                if (jobType.equals(JobConfig.JobType.TRANSCRIPTION)) sm.getTheTranscript(id, SHOW);
                return true;
            case R.id.menuSaveTranscriptToFile:
                if (jobType.equals(JobConfig.JobType.TRANSCRIPTION))
                    sm.getTheTranscript(id, SAVE_FILE);
                return true;
            case R.id.menuDelete:
                if(layoutManager != null)
                    firstPosition = layoutManager.findFirstVisibleItemPosition();
                sm.deleteJob(id);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }

    @Override
    public void onBackPressed() {
        adapter.hideList();
        recyclerView.postDelayed(super::onBackPressed, (long) (adapter.DURATION_ANIMATION * 1.5));
    }

    /**
     * Возвращает экземпляр билдера диалогового окна с ошибкой
     *
     * @param text   текст ошибки
     * @param finish закрывать ли activity по нажатию ОК
     * @return билдер
     */
    private AlertDialog.Builder getErrorDialogBuilder(String text, boolean finish) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Ошибка").setIcon(R.drawable.ic_error)
                .setMessage(text).setCancelable(false);
        if (finish) builder.setPositiveButton("OK", (dialogInterface, i) -> finish());
        else builder.setPositiveButton("OK", null);
        return builder;
    }

    /**
     * Проверить результат на ошибки и выдать сообщение при необходимости
     *
     * @param responseCode код ответа сервера
     * @param finish       завершать ли activity при ошибке
     */
    private void checkForErrors(int responseCode, boolean finish) {
        AlertDialog.Builder builder;
        switch (responseCode) {
            case HttpURLConnection.HTTP_OK:
                return;
            case SpeechmaticsBatchSDK.NO_DATA:
                builder = getErrorDialogBuilder("Нет соединения с сервером", finish);
                break;
            case HttpURLConnection.HTTP_NOT_FOUND:
                builder = getErrorDialogBuilder("Работа не найдена", finish);
                break;
            case HttpURLConnection.HTTP_GONE:
                builder = getErrorDialogBuilder("Файл удален с сервера", finish);
                break;
            case HttpURLConnection.HTTP_UNAUTHORIZED:
                builder = getErrorDialogBuilder("Ошибка авторизации", finish);
                break;
            case HttpURLConnection.HTTP_INTERNAL_ERROR:
                builder = getErrorDialogBuilder("Внутренняя ошибка сервера", finish);
                break;
            default:
                builder = getErrorDialogBuilder("Невозможно прочитать данные", finish);
                break;
        }
        builder.show();
    }

    @Override
    public void onAuthorizationCheckFinished(int responseCode) {

    }

    @Override
    public void onGetJobDetailsFinished(int responseCode, JobDetails jobDetails) {
        checkForErrors(responseCode, false);
        if(responseCode == HttpURLConnection.HTTP_OK) {
            InfoDialog jobDetailsDialog = new InfoDialog(this,
                    "Детали работы", InfoDialog.INFO, jobDetails.toString());
            jobDetailsDialog.show();
        }
    }

    @Override
    public void onGetAllJobsDetailsFinished(int responseCode, ArrayList<JobDetails> jobDetailsList) {
        checkForErrors(responseCode, true);
        if(responseCode == HttpURLConnection.HTTP_OK) {
            layoutManager = new LinearLayoutManager(this);
            adapter = new JobsListAdapter(this, jobDetailsList,
                    this);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(layoutManager);
            recyclerView.scrollToPosition(firstPosition);
        }
    }

    @Override
    public void onGetTheTranscriptFinished(int responseCode, String response, int requestCode) {
        System.out.println("Get the Transcript: " + responseCode);
        checkForErrors(responseCode, false);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            text = response;
            if (requestCode == SAVE_FILE) {
                fileSaverLauncher.launch("");
            } else if (requestCode == SHOW) {
                InfoDialog showTranscriptDialog = new InfoDialog(this,
                        "Расшифровка аудио", InfoDialog.TRANSCRIPT, text);
                showTranscriptDialog.show();
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onDeleteJobFinished(int responseCode, JobDetails jobDetails) {
        checkForErrors(responseCode, false);
        if (responseCode == HttpURLConnection.HTTP_OK) {
            adapter.hideList();
            recyclerView.postDelayed(() -> {sm.getAllJobsDetails(includeDeleted);
                }, (long) (adapter.DURATION_ANIMATION * 1.5));
        }
    }

    @Override
    public void onGetStatisticsFinished(int responseCode, SummaryStatistics[]
            summaryStatistics, int requestCode) {}

    @Override
    public void onSubmitJobFinished(int responseCode, String id, String extraInfo) {}

    @Override
    public void onItemClick(int position) {

    }
}