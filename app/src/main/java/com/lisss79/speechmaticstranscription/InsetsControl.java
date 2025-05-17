package com.lisss79.speechmaticstranscription;

import static androidx.core.view.WindowInsetsCompat.CONSUMED;
import static androidx.core.view.WindowInsetsCompat.Type.systemBars;

import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;

public class InsetsControl {
    AppCompatActivity activity;
    InsetsControl(AppCompatActivity activity) {
        this.activity = activity;
    }

    void setSystemBarsInsetsForView(@IdRes Integer viewId) {
        ViewCompat.setOnApplyWindowInsetsListener(activity.findViewById(viewId), (v, insets) -> {
            Insets systemInsets = insets.getInsets(systemBars());
            v.setPadding(
                    systemInsets.left,
                    systemInsets.top,
                    systemInsets.right,
                    systemInsets.bottom
            );

            return CONSUMED;
        });
    }
}
