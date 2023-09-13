package com.lisss79.speechmaticstranscription.audioplayer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.text.format.DateUtils;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.lisss79.speechmaticstranscription.R;

public class AudioPlayer extends LinearLayout {

    public static final int MODE_PLAY = 1;
    public static final int MODE_PAUSE = 2;
    public static final int MODE_NO_AUDIO = 3;
    private final long DELAY_INTERVAL = 1000;
    private final TextView textView;
    private final ImageButton imageButton;
    private final SeekBar seekBar;
    private int max;
    private int value;
    private final Drawable iconModePlay;
    private final Drawable iconModePause;
    private MediaPlayer player;
    private Runnable oneSecCounter;

    // Режим работы;
    private int mode;

    public AudioPlayer(Context context) {
        this(context, null);
    }

    public AudioPlayer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Resources.Theme theme = context.getTheme();
        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.audio_player, this);
        textView = findViewById(R.id.textView);
        imageButton = findViewById(R.id.button);
        seekBar = findViewById(R.id.seekBar);
        max = 0;
        value = 0;
        seekBar.setMax(max);
        mode = MODE_NO_AUDIO;
        iconModePlay = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_pause, theme);
        iconModePause = ResourcesCompat.getDrawable(getResources(), R.drawable.ic_play, theme);

        oneSecCounter = () -> {
            if(player != null) {
                value = player.getCurrentPosition() / 1000;
                seekBar.setProgress(value);
                showText();
                if(mode == MODE_PLAY) postDelayed(oneSecCounter, DELAY_INTERVAL);
            }
        };

        showText();
        setSeekbarCallback();
        setButtonCallback();
    }

    public void reset() {
        if(player != null) player.reset();
    }

    private void setButtonCallback() {
        imageButton.setOnClickListener(v -> {
            if(mode == MODE_PAUSE) {
                mode = MODE_PLAY;
                imageButton.setImageDrawable(iconModePlay);
                if(player != null) player.start();
                postDelayed(oneSecCounter, DELAY_INTERVAL);
            } else if(mode == MODE_PLAY) {
                mode = MODE_PAUSE;
                imageButton.setImageDrawable(iconModePause);
                if(player != null) player.pause();
            } else {
                imageButton.setImageDrawable(iconModePause);
                Toast.makeText(getContext(), "Нечего воспроизводить!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setSeekbarCallback() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value = progress;
                showText();
                if(fromUser) player.seekTo(progress * 1000);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
    }

    private void showText() {
        // long offset = TimeZone.getDefault().getRawOffset();
        // String valueText = String.format(Locale.getDefault(), "%1$tT", value * 1000L - offset);
        String valueText = DateUtils.formatElapsedTime(value);
        // String maxText = String.format(Locale.getDefault(),"%1$tT", max * 1000L - offset);
        String maxText = DateUtils.formatElapsedTime(max);
        textView.setText(String.format("%s / %s", valueText, maxText));
    }

    public boolean setSourceUri(@Nullable Uri uri) {
        if(uri == null & player != null) {
            player.reset();
            return true;
        } else if(uri == null & player == null) {
            return true;
        }
        player = MediaPlayer.create(getContext(), uri);
        try {
            player.setOnPreparedListener(mediaPlayer -> {
                mode = MODE_PAUSE;
                imageButton.setImageDrawable(iconModePause);
                value = 0;
                int duration = player.getDuration();
                max = duration / 1000;
                seekBar.setProgress(value);
                seekBar.setMax(max);
                showText();
            });
            player.setOnCompletionListener(mediaPlayer -> {
                seekBar.setProgress(value);
                showText();
                mode = MODE_PAUSE;
                imageButton.setImageDrawable(iconModePause);
                player.pause();
            });
            return true;
        }
        catch (Exception e) {
            e.printStackTrace();
            return false;
        }

    }
}
