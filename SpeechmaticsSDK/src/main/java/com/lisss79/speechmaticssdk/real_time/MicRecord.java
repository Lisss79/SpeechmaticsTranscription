package com.lisss79.speechmaticssdk.real_time;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import com.lisss79.speechmaticssdk.real_time.data.StartRecognition;

/**
 * Класс для получения "сырого" потока аудио с микрофона.
 * Экземпляр возвращает статический метод getInstance,
 * получая в качестве параметра конфигурации StartRecognition
 */
public class MicRecord extends AudioRecord {

    // Параметры записи
    private static int sampleRateInHz;
    private static int channelConfig;
    private static int channelConfigMultiplier;
    private static int audioFormat;
    private static int audioFormatMultiplier;
    private static final int BUFFER_SIZE_FACTOR = 2;
    public static int BUFFER_SIZE;
    private static final int MIC = MediaRecorder.AudioSource.MIC;
    private int bytesPerSecond = 0;

    @RequiresPermission(value = "android.permission.RECORD_AUDIO")
    public static MicRecord getInstance(@Nullable StartRecognition startRecognition) {
        if(startRecognition == null) startRecognition = new StartRecognition();
        getAudioParams(startRecognition);
        return new MicRecord(MIC, sampleRateInHz, channelConfig, audioFormat, BUFFER_SIZE);
    }

    @RequiresPermission(value = "android.permission.RECORD_AUDIO")
    private MicRecord(int audioSource, int sampleRateInHz,
                     int channelConfig, int audioFormat, int bufferSizeInBytes) {
        super(audioSource, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        bytesPerSecond = sampleRateInHz * audioFormatMultiplier * channelConfigMultiplier / 8;
    }

    public int getBytesPerSecond() {
        return bytesPerSecond;
    }

    /**
     * Получаем значения аудиопараметров из конфигурации
     */
    private static void getAudioParams(StartRecognition startRecognition) {
        sampleRateInHz = startRecognition.getAudioFormat().getSampleRate();
        channelConfig = android.media.AudioFormat.CHANNEL_IN_MONO;
        channelConfigMultiplier = 1;
        final StartRecognition.AudioFormat.Encoding encoding = startRecognition.getAudioFormat().getEncoding();
        switch(encoding) {
            case MULAW:
                audioFormat = android.media.AudioFormat.ENCODING_PCM_8BIT;
                audioFormatMultiplier = 8;
                break;
            case PCM_16:
                audioFormat = android.media.AudioFormat.ENCODING_PCM_16BIT;
                audioFormatMultiplier = 16;
                break;
            case PCM_32:
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    audioFormat = android.media.AudioFormat.ENCODING_PCM_32BIT;
                    audioFormatMultiplier = 32;
                } else {
                    audioFormat = AudioFormat.ENCODING_PCM_16BIT;
                    audioFormatMultiplier = 16;
                }

                break;
        }
        BUFFER_SIZE = AudioRecord.getMinBufferSize(sampleRateInHz,
                channelConfig, audioFormat) * BUFFER_SIZE_FACTOR;
    }

}
