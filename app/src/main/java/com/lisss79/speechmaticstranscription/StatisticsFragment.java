package com.lisss79.speechmaticstranscription;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.batch.data.SummaryStatistics;
import com.lisss79.speechmaticstranscription.databinding.FragmentStatisticsBinding;

public class StatisticsFragment extends Fragment {

    private final float STANDARD_LIMIT = 2;
    private final float ENHANCED_LIMIT = 2;
    private FragmentStatisticsBinding binding;
    private SharedPreferences prefs;
    private final Context context;
    private SummaryStatistics[] summaryStatistics;
    private SummaryStatistics[] monthlyStatistics;
    private boolean isCreated = false;

    public StatisticsFragment(Context context) {
        this.context = context;
    }

    public void setStatistics(SummaryStatistics[] summaryStatistics,
                              SummaryStatistics[] monthlyStatistics) {
        this.summaryStatistics = summaryStatistics;
        this.monthlyStatistics = monthlyStatistics;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(summaryStatistics != null && monthlyStatistics != null) refreshData();
        isCreated = true;
    }

    public void refreshData() {

        int visibility = prefs.getBoolean("show_limit", false) ? View.VISIBLE : View.GONE;

        // Заполняем поле общей ствтистики
        binding.textViewSummaryCount.setText(String.valueOf(summaryStatistics[0].getCount() +
                summaryStatistics[1].getCount()));
        String summaryDuration = SpeechmaticsBatchSDK
                .durationHoursToString(String.valueOf(summaryStatistics[0].getDuration_hrs() +
                        summaryStatistics[1].getDuration_hrs()));
        binding.textViewSummaryDuration.setText(summaryDuration);

        // Статистика за месяц для стандартного качества
        binding.textViewMonthlyAccuracyStandard
                .setText(monthlyStatistics[0].getOperatingPoint().getName());
        binding.textViewMonthlyCountStandard.setText(String.valueOf(monthlyStatistics[0].getCount()));
        String monthlyDurationS = SpeechmaticsBatchSDK
                .durationHoursToString(String.valueOf(monthlyStatistics[0].getDuration_hrs()));
        binding.textViewMonthlyDurationStandard.setText(monthlyDurationS);

        // Статистика за месяц для улучшенного качества
        binding.textViewMonthlyAccuracyEnhanced
                .setText(monthlyStatistics[1].getOperatingPoint().getName());
        binding.textViewMonthlyCountEnhanced.setText(String.valueOf(monthlyStatistics[1].getCount()));
        String monthlyDurationE = SpeechmaticsBatchSDK
                .durationHoursToString(String.valueOf(monthlyStatistics[1].getDuration_hrs()));
        binding.textViewMonthlyDurationEnhanced.setText(monthlyDurationE);

        // Доступные лимиты
        float limitS = STANDARD_LIMIT - monthlyStatistics[0].getDuration_hrs();
        float limitE = ENHANCED_LIMIT - monthlyStatistics[1].getDuration_hrs();
        String limit = String.format("%s / %s",
                SpeechmaticsBatchSDK.durationHoursToString(String.valueOf(limitS)),
                SpeechmaticsBatchSDK.durationHoursToString(String.valueOf(limitE)));
        binding.textViewLimit.setText(limit);
        binding.textViewLimitLabel.setVisibility(visibility);
        binding.textViewLimit.setVisibility(visibility);
    }

    public void refreshData(SummaryStatistics[] summaryStatistics,
                            SummaryStatistics[] monthlyStatistics) {
        this.summaryStatistics = summaryStatistics;
        this.monthlyStatistics = monthlyStatistics;
        if(isCreated) refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}