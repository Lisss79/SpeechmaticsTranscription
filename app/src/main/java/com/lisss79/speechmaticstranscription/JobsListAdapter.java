package com.lisss79.speechmaticstranscription;

import static com.lisss79.speechmaticssdk.common.JsonKeysValues.USER_CREATED_PATTERN;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.lisss79.speechmaticssdk.batch.SpeechmaticsBatchSDK;
import com.lisss79.speechmaticssdk.batch.data.JobDetails;
import com.lisss79.speechmaticssdk.batch.statuses.JobStatus;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class JobsListAdapter extends RecyclerView.Adapter<JobsListAdapter.JobsListHolder> {

    private final ArrayList<JobDetails> jobDetailsList;
    private final CardView[] cardViewArray;
    private final LayoutInflater inflater;
    private final ItemClickedListener itemClickedListener;
    private final Context context;
    private int lastPosition = -1;
    int colorActive = Color.MAGENTA;
    int colorDeleted = Color.RED;
    public final int DURATION_ANIMATION = 400;

    public JobsListAdapter(Context context, ArrayList<JobDetails> jobDetailsList,
                           ItemClickedListener itemClickedListener) {
        this.context = context;
        inflater = LayoutInflater.from(context);
        this.jobDetailsList = jobDetailsList;
        this.itemClickedListener = itemClickedListener;
        cardViewArray = new CardView[jobDetailsList.size()];
        DisplayMetrics dm = context.getResources().getDisplayMetrics();

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.colorPrimary, outValue, true);
        colorActive = outValue.data;
        context.getTheme().resolveAttribute(android.R.attr.colorError, outValue, true);
        colorDeleted = outValue.data;
    }

    public JobDetails getJobDetails(int index) {
        return jobDetailsList.get(index);
    }

    public void hideList() {
        for(int i = 0; i < getItemCount(); i++) {
            if(cardViewArray[i] != null) {
                ObjectAnimator animator1 = ObjectAnimator.ofFloat(cardViewArray[i],
                        "scaleY", 1f, 0f);
                animator1.setDuration(DURATION_ANIMATION);
                animator1.start();
                ObjectAnimator animator2 = ObjectAnimator.ofFloat(cardViewArray[i],
                        "scaleX", 1f, 0f);
                animator2.setDuration(DURATION_ANIMATION);
                animator2.start();
            }
        }
    }

    public void deleteJobFromList(int index) {
        jobDetailsList.remove(index);
        notifyItemRemoved(index);
    }

    @NonNull
    @Override
    public JobsListHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.jobslist_item, parent, false);
        return new JobsListHolder(itemView, this);
    }

    @Override
    @SuppressLint("RecyclerView")
    public void onBindViewHolder(@NonNull JobsListHolder holder, int position) {
        JobDetails current = jobDetailsList.get(position);
        String name = current.getDataName();
        int color = colorActive;
        if(name.isEmpty()) name = "URL";
        if(current.getStatus() == JobStatus.DELETED) {
            name = "Deleted";
            color = colorDeleted;
        }
        if(current.getStatus() == JobStatus.EXPIRED) {
            name = "Expired";
            color = colorDeleted;
        }
        holder.jl_name.setText(name);
        holder.jl_name.setTextColor(color);
        holder.jl_date.setText(new SimpleDateFormat(USER_CREATED_PATTERN, Locale.getDefault())
                .format(current.getCreatedAt()));
        holder.jl_length.setText(SpeechmaticsBatchSDK.durationToString(current.getDuration()));
        holder.jl_language.setText(current.getTranscriptionConfig().getLanguage().getName());
        holder.jl_id.setText(current.getId());
        holder.jl_op.setText(current.getTranscriptionConfig().getOperatingPoint().getName());
        holder.jl_type.setText(current.getJobConfig().getJobType().getName());
        holder.jl_status.setText(current.getStatus().getName());
        holder.jobsList_cardView.setOnClickListener(v -> {
            itemClickedListener.onItemClick(position);
        });
        holder.jobsList_cardView.setTag(position);
        cardViewArray[position] = holder.jobsList_cardView;
        ((Activity) context).registerForContextMenu(holder.jobsList_cardView);

        if(position > lastPosition) {
            cardViewArray[position].setScaleX(0f);
            cardViewArray[position].setScaleY(0f);
            ObjectAnimator animator1 = ObjectAnimator.ofFloat(cardViewArray[position],
                    "scaleY", 0f, 1f);
            animator1.setDuration(DURATION_ANIMATION);
            animator1.start();
            ObjectAnimator animator2 = ObjectAnimator.ofFloat(cardViewArray[position],
                    "scaleX", 0f, 1f);
            animator2.setDuration(DURATION_ANIMATION);
            animator2.start();
            lastPosition = position;
        }

    }

    @Override
    public void onViewDetachedFromWindow(@NonNull JobsListHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.jobsList_cardView.clearAnimation();
    }

    @Override
    public int getItemCount() {
        return jobDetailsList.size();
    }

    public static class JobsListHolder extends RecyclerView.ViewHolder {
        public final TextView jl_name;
        public final TextView jl_date;
        public final TextView jl_length;
        public final TextView jl_language;
        public final TextView jl_op;
        public final TextView jl_type;
        public final TextView jl_status;
        public final TextView jl_id;
        public final CardView jobsList_cardView;
        final JobsListAdapter adapter;

        public JobsListHolder(@NonNull View itemView, JobsListAdapter adapter) {
            super(itemView);
            this.jl_name = itemView.findViewById(R.id.jl_name);
            this.jl_date = itemView.findViewById(R.id.jl_date);
            this.jl_length = itemView.findViewById(R.id.jl_length);
            this.jl_language = itemView.findViewById(R.id.jl_language);
            this.jl_id = itemView.findViewById(R.id.jl_id);
            this.jl_type = itemView.findViewById(R.id.jl_type);
            this.jl_status = itemView.findViewById(R.id.jl_status);
            this.jl_op = itemView.findViewById(R.id.jl_op);
            this.jobsList_cardView = itemView.findViewById(R.id.jobsList_cardView);
            this.adapter = adapter;
        }
    }
}
