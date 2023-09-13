package com.lisss79.speechmaticssdk;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Okio;
import okio.Source;

public class ContentUriRequestBody extends RequestBody {
    private final ContentResolver contentResolver;
    private final Uri uri;
    private final long contentLength;
    private final ProgressListener listener;

    public ContentUriRequestBody(ContentResolver contentResolver,
                                 Uri uri, int contentLength, ProgressListener listener) {
        this.contentResolver = contentResolver;
        this.uri = uri;
        this.contentLength = contentLength;
        this.listener = listener;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        String mediaType = contentResolver.getType(uri);
        return MediaType.parse(mediaType);
    }

    @Override
    public long contentLength() {
        return contentLength;
    }

    @Override
    public void writeTo(@NonNull BufferedSink bufferedSink) {
        InputStream is = null;
        Source source = null;
        try {
            is = contentResolver.openInputStream(uri);
            source = Okio.source(is);

            // Считываем байт из входящего потока (файл) и записываем в исходящий (тело http запроса)
            int j = 0;
            int step = (int) Math.ceil(contentLength / 9f);
            int numOfBytes;
            byte[] nextBytes = new byte[step];
            do {
                numOfBytes = is.read(nextBytes);
                if(numOfBytes > 0) bufferedSink.write(nextBytes, 0, numOfBytes);
                if(j < 90) j += 10;
                else j = 99;
                System.out.println("Step: " + step + ", numOfBytes: " + numOfBytes + ", j: " + j);
                listener.onProgressListener(j);
            } while (numOfBytes >= step);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (source != null) source.close();
                if (is != null) is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }
}
