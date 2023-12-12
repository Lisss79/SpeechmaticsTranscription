package com.lisss79.speechmaticstranscription;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.batch.data.JobConfig;
import com.lisss79.speechmaticssdk.common.JsonKeysValues;
import com.lisss79.speechmaticssdk.common.Language;
import com.lisss79.speechmaticssdk.common.OperatingPoint;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public class SettingsActivity extends AppCompatActivity {

    // Ключи для обмена данными с activity и shared preferences
    private static final String AUTH_TOKEN = "AUTH_TOKEN";
    private static final String RESTART_FLAG = "RESTART_FLAG";
    private static final String FILE_URI = "FILE_URI";
    private static final String JOB_ID = "JOB_ID";
    private static final String DIC_URI = "DIC_URI";

    // Текущее значение длинны части
    private static int currentPieceValue;

    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.settings, new SettingsFragment()).commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        private SharedPreferences prefs;
        private SharedPreferences dicPrefs;
        private Activity activity;
        private Context context;
        private ActivityResultLauncher<String[]> filePickerLauncher;


        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            activity = getActivity();
            context = getContext();
            prefs = PreferenceManager.getDefaultSharedPreferences(context);
            dicPrefs = context.getSharedPreferences(MainActivity.VOC_PREFS_NAME, MODE_PRIVATE);

            // Лончер выбора текстового файла для чтения в словарь
            filePickerLauncher =
                    registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                        if (uri != null) {
                            context.getContentResolver().takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(DIC_URI, uri.toString());
                            editor.apply();
                            Intent intent = new Intent();
                            intent.putExtra(RESTART_FLAG, true);
                            activity.setResult(RESULT_OK, intent);
                        }
                    });

            // Наполняем пункты настройки
            ListPreference lpType = findPreference(JsonKeysValues.JOB_TYPE);
            lpType.setEntries(JobConfig.JobType.getAllNames());
            lpType.setEntryValues(JobConfig.JobType.getAllCodes());
            if(lpType.getValue() == null)
                lpType.setValue(SpeechmaticsBatchSDK.defJobType.getCode());

            ListPreference lpLanguage = findPreference(JsonKeysValues.LANGUAGE);
            lpLanguage.setEntries(Language.getAllNames());
            lpLanguage.setEntryValues(Language.getAllCodes());
            if(lpLanguage.getValue() == null)
                lpLanguage.setValue(SpeechmaticsBatchSDK.defLanguage.getCode());

            ListPreference lpDiarization = findPreference(JsonKeysValues.DIARIZATION);
            lpDiarization.setEntries(JobConfig.TranscriptionConfig.Diarization.getAllNames());
            lpDiarization.setEntryValues(JobConfig.TranscriptionConfig.Diarization.getAllCodes());
            if(lpDiarization.getValue() == null)
                lpDiarization.setValue(SpeechmaticsBatchSDK.defDiarization.getCode());

            ListPreference lpOP = findPreference(JsonKeysValues.OPERATING_POINT);
            lpOP.setEntries(OperatingPoint.getAllNames());
            lpOP.setEntryValues(OperatingPoint.getAllCodes());
            if(lpOP.getValue() == null)
                lpOP.setValue(SpeechmaticsBatchSDK.defOperatingPoint.getCode());

            // Использовать ли пользовательский словарь
            SwitchPreference spVocab = findPreference(JsonKeysValues.ADDITIONAL_VOCAB);
            spVocab.setOnPreferenceChangeListener((preference, newValue) -> {
                Intent intent = new Intent();
                intent.putExtra(RESTART_FLAG, true);
                activity.setResult(RESULT_OK, intent);
                return true;
            });

            // Обработка нажатия "очистить данные"
            Preference pReset = findPreference("reset_data");
            pReset.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(true).setTitle("Предупреждение")
                        .setMessage("Вы точно хотите очистить данные о текущем файле и работе?")
                        .setIcon(R.drawable.ic_warning).setNegativeButton("Отмена", null)
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            Intent intent = new Intent();
                            intent.putExtra(RESTART_FLAG, true);
                            activity.setResult(RESULT_OK, intent);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(JOB_ID, "");
                            editor.putString(FILE_URI, "");
                            editor.apply();
                            Toast.makeText(activity, "Данные очищены", Toast.LENGTH_SHORT).show();
                        });
                builder.show();
                return true;
            });

            // Обработка нажатия "удалить временные файлы"
            Preference pDelete = findPreference("delete_temp_files");
            pDelete.setOnPreferenceClickListener(preference -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setCancelable(true).setTitle("Предупреждение")
                        .setMessage("Вы точно хотите удалить временные файлы?")
                        .setIcon(R.drawable.ic_warning).setNegativeButton("Отмена", null)
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            File[] files = context.getFilesDir().listFiles();
                            boolean res = true;
                            if (files != null) {
                                for (File file : files) {
                                    if (!file.isDirectory()) res = res && file.delete();
                                }

                            }
                            if (files != null && res) Toast.makeText(activity,
                                    "Файлы удалены", Toast.LENGTH_SHORT).show();
                            else Toast.makeText(activity,
                                    "Не удалось удалить файлы", Toast.LENGTH_SHORT).show();
                        });
                builder.show();
                return true;
            });

            // Обработка нажатия "новый токен"
            Preference pToken = findPreference("token");
            pToken.setOnPreferenceClickListener(preference -> {
                requestNewToken();
                return true;
            });

            // Обработка нажатия "показывать лимит"
            Preference pLimit = findPreference("show_limit");
            pLimit.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent();
                intent.putExtra(RESTART_FLAG, true);
                activity.setResult(RESULT_OK, intent);
                return false;
            });

            // Обработка нажатия "длительность части"
            Preference pPiece = findPreference("length_piece");
            currentPieceValue = 10;
            try {
                currentPieceValue = prefs.getInt("length_piece", 10);
            }
            catch (ClassCastException e) {
                e.printStackTrace();
            }
            pPiece.setSummary(String.valueOf(currentPieceValue));

            pPiece.setOnPreferenceClickListener(preference -> {
                LayoutInflater inflater = getLayoutInflater();
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                View groupView = inflater.inflate(R.layout.length_piece, null);
                NumberPicker picker = groupView.findViewById(R.id.lengthPicker);
                picker.setMinValue(1);
                picker.setMaxValue(60);
                builder.setView(groupView).setTitle("Выберите длину части")
                        .setIcon(R.drawable.ic_info)
                        .setPositiveButton("OK", (dialogInterface, i) -> {
                            SharedPreferences.Editor editor = prefs.edit();
                            int newValue = picker.getValue();
                            editor.putInt("length_piece", newValue);
                            editor.apply();
                            pPiece.setSummary(String.valueOf(newValue));
                            currentPieceValue = newValue;
                        })
                        .setNegativeButton("Отмена", null);
                builder.show();
                picker.setValue(currentPieceValue);
                return true;
            });

            // Обработка нажатия "уведомления"
            Preference pNotifications = findPreference("notifications");
            pNotifications.setOnPreferenceClickListener(preference -> {
                Intent intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                startActivity(intent);
                return true;
            });

            // Обработка нажатия "загрузить словарь"
            Preference pVocabulary = findPreference("vocabulary");
            pVocabulary.setOnPreferenceClickListener(preference -> {
                String fileType = "text/plain";
                filePickerLauncher.launch(new String[]{fileType});
                return true;
            });

        }

        @Nullable
        private Map.Entry<String, TreeSet<String>> getMapFromString(String responseLine) {
            String[] s = responseLine.split("=", 2);
            if(s.length > 1) {
                String key = s[0].trim();
                List<String> valuesList = Arrays.asList(s[1].split(","));
                valuesList.replaceAll(String::trim);
                TreeSet<String> values = new TreeSet<>(valuesList);
                return Map.entry(key, values);
            } else return null;
        }

        /**
         * Запросить новый токен (пункт меню)
         */
        private void requestNewToken() {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            View dialogView = getLayoutInflater().inflate(R.layout.text_request, null);
            builder.setView(dialogView).setCancelable(true).setTitle("Новый токен")
                    .setIcon(R.drawable.ic_warning).setNegativeButton("Отмена", null)
                    .setPositiveButton("OK", (dialogInterface, i) -> {
                        EditText editText = dialogView.findViewById(R.id.editTextText);
                        String auth_token = editText.getText().toString();
                        if (!auth_token.isEmpty()) {
                            Intent intent = new Intent();
                            intent.putExtra(RESTART_FLAG, true);
                            activity.setResult(RESULT_OK, intent);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putString(AUTH_TOKEN, auth_token);
                            editor.apply();
                            Toast.makeText(activity, "Новый токен сохранен", Toast.LENGTH_SHORT).show();
                        } else requestNewToken();
                    });
            builder.show();
        }

    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}