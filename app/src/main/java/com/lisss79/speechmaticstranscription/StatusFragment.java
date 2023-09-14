package com.lisss79.speechmaticstranscription;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticstranscription.databinding.FragmentStatusBinding;

public class StatusFragment extends Fragment {

    private FragmentStatusBinding binding;
    private SpeechmaticsBatchSDK sm;
    private boolean isCreated = false;
    private final Context context;

    public StatusFragment(Context context) {
        this.context = context;
    }

    public void setSm(SpeechmaticsBatchSDK sm) {
        this.sm = sm;
    }
    public SpeechmaticsBatchSDK getSm() { return sm; }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentStatusBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(sm != null) refreshData();
        isCreated = true;
    }

    public void refreshData() {
        if(!sm.fileUrl.isEmpty()) {
            binding.textViewFileNameLabel.setText("URL файла: ");
            binding.textViewFileName.setText(sm.fileUrl);
        } else {
            binding.textViewFileNameLabel.setText("Имя файла: ");
            binding.textViewFileName.setText(sm.fileName);
        }
        binding.textViewFileStatus.setText(sm.fileStatus.getName());
        binding.textViewJobId.setText(sm.jobId);
        binding.textViewJobType.setText(sm.jobType.getName());
        binding.textViewJobStatus.setText(sm.jobStatus.getName());
        String ts = ((MainActivity) context).isTranscriptionLoaded ? "получена" : "недоступна";
        binding.textViewTranscriptionStatus.setText(ts);
        int userDicStatus = ((MainActivity) context).userDic;
        binding.textViewDictionaryStatus.setText(getDicStatus(userDicStatus));
    }

    private String getDicStatus(int userDicStatus) {
        String status = "";
        if ((userDicStatus & MainActivity.DIC_ENABLED)
                == MainActivity.DIC_ENABLED) status = "включен, ";
        else status = "выключен, ";
        if ((userDicStatus & MainActivity.DIC_LOADED_FROM_EXTERNAL_FILE)
                == MainActivity.DIC_LOADED_FROM_EXTERNAL_FILE) status += "загружен из файла";
        else if ((userDicStatus & MainActivity.DIC_LOADED_FROM_PREFS)
                == MainActivity.DIC_LOADED_FROM_PREFS) status += "загружен из памяти";
        else status += "не загружен";
        return status;
    }

    public void refreshData(SpeechmaticsBatchSDK sm) {
        this.sm = sm;
        if(isCreated) refreshData();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}