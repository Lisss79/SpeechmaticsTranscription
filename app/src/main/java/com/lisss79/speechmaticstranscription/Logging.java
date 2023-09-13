package com.lisss79.speechmaticstranscription;

import android.annotation.SuppressLint;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

/**
 * Класс для работы с логом приложения
 */
public class Logging {
    private final String build = "07072023";
    private final File logFile;
    private final String TEMPLATE = "%1$td.%1$tm.%1$tY %1$tH:%1$tM:%1$tS %2$s %3$s";
    private final String startMessage = "Log started";
    private final String stopMessage = "Log stopped";
    private final String clearMessage = "Log cleared";
    private String type;
    public final static String TYPE_BATCH = "Batch:";
    public final static String TYPE_REAL_TIME = "Real Time:";

    public Logging(File logFile, String type) {
        this.logFile = logFile;
        this.type = type;
    }

    public void write(String text) {
        Calendar now = Calendar.getInstance();
        @SuppressLint("DefaultLocale") String logText =
                String.format(TEMPLATE, now, type, text);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.append(logText);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        Calendar now = Calendar.getInstance();
        @SuppressLint("DefaultLocale") String logText =
                String.format(TEMPLATE, now, type, startMessage);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.newLine();
            writer.append("Build: ").append(build);
            writer.newLine();
            writer.append(logText);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        Calendar now = Calendar.getInstance();
        @SuppressLint("DefaultLocale") String logText =
                String.format(TEMPLATE, now, type, stopMessage);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.append(logText);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        Calendar now = Calendar.getInstance();
        @SuppressLint("DefaultLocale") String logText =
                String.format(TEMPLATE, now, type, clearMessage);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile))) {
            writer.write(logText);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
