package com.lisss79.speechmaticstranscription;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class InfoScreenAdapter extends FragmentStateAdapter {
    public Fragment statusFragment;
    public Fragment statisticsFragment;

    public InfoScreenAdapter(@NonNull FragmentManager fragmentManager,
                             @NonNull Lifecycle lifecycle, Context context) {
        super(fragmentManager, lifecycle);
        statusFragment = new StatusFragment(context);
        statisticsFragment = new StatisticsFragment(context);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Fragment fragment;
        if(position == 0) fragment = statusFragment;
        else fragment = statisticsFragment;
        return fragment;
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
