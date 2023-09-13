package com.lisss79.speechmaticstranscription;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Диалог с текстом, кнопкой "ОК" и возможностью копирования текста в буфер
 * по клику
 */
public class InfoDialog extends AlertDialog {

    // Константы для определения типа диалога (иконки)
    public final static int TRANSCRIPT = 33;
    public final static int INFO = 34;
    public final static int ERROR = 35;

    private final String title;
    private String text;
    private final int dialogType;
    private final Context context;
    private TextView textView;

    public InfoDialog(Context context, String title, int dialogType, String text) {
        super(context);
        this.title = title;
        this.dialogType = dialogType;
        this.text = text;
        this.context = context;
        initDialog();
    }

    public void setText(String text) {
        this.text = text;
        textView.setText(text);
    }

    @SuppressLint("InflateParams")
    private void initDialog() {
        setTitle(title);
        setIcon(getIconId());
        setCancelable(false);
        View dialogView = getLayoutInflater().inflate(R.layout.show_info, null);
        textView = dialogView.findViewById(R.id.textViewInfo);
        textView.setText(text);
        textView.setOnClickListener(v -> {
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "Данные скопированы", Toast.LENGTH_LONG).show();
        });
        setView(dialogView);
        setButton(BUTTON_POSITIVE, "OK", (dialogInterface, i) -> dismiss());
        if(dialogType == TRANSCRIPT) textView.setTextSize(14f);
    }

    private int getIconId() {
        int id;
        switch (dialogType) {
            case TRANSCRIPT:
                id = R.drawable.ic_show_info;
                break;
            case INFO:
                id = R.drawable.ic_info;
                break;
            case ERROR:
                id = R.drawable.ic_error;
                break;
            default:
                id = R.drawable.ic_error;
                break;
        }
        return id;
    }

    @Override
    public void setMessage(CharSequence message) {
        textView.setText(message);
    }
}
