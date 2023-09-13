package com.lisss79.speechmaticstranscription;

import androidx.core.content.FileProvider;

public class AudioFileProvider extends FileProvider {

    public AudioFileProvider() {
        super(R.xml.file_paths);
    }
}