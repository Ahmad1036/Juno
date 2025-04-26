package com.example.juno;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class JournalListAdapter extends RecyclerView.Adapter<JournalListAdapter.JournalViewHolder> {

    private final Context context;
    private final List<Journal> journals;
    private final OnJournalClickListener listener;

    public interface OnJournalClickListener {
        void onJournalClick(Journal journal);
    }

    public JournalListAdapter(Context context, List<Journal> journals, OnJournalClickListener listener) {
        this.context = context;
        this.journals = journals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public JournalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_journal_entry, parent, false);
        return new JournalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull JournalViewHolder holder, int position) {
        Journal journal = journals.get(position);
        holder.bindJournal(journal);
    }

    @Override
    public int getItemCount() {
        return journals.size();
    }

    class JournalViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateText;
        private final TextView previewText;
        private final TextView hasImageIndicator;
        private final TextView hasGratitudeIndicator;

        public JournalViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.item_journal_date);
            previewText = itemView.findViewById(R.id.item_journal_preview);
            hasImageIndicator = itemView.findViewById(R.id.item_has_image_indicator);
            hasGratitudeIndicator = itemView.findViewById(R.id.item_has_gratitude_indicator);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && listener != null) {
                    listener.onJournalClick(journals.get(position));
                }
            });
        }

        void bindJournal(Journal journal) {
            // Format date
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
            String formattedDate = dateFormat.format(new Date(journal.getTimestamp())).toLowerCase();
            
            // Add last updated indicator if edited
            if (journal.getLastUpdated() > journal.getTimestamp()) {
                SimpleDateFormat updateFormat = new SimpleDateFormat("MMM d, h:mm a", Locale.getDefault());
                String updatedTime = updateFormat.format(new Date(journal.getLastUpdated())).toLowerCase();
                formattedDate += " (edited " + updatedTime + ")";
            }
            
            dateText.setText(formattedDate);

            // Set journal preview text
            String content = journal.getContent();
            if (content != null && !content.isEmpty()) {
                previewText.setText(content);
            } else if (journal.getGratitudeContent() != null && !journal.getGratitudeContent().isEmpty()) {
                previewText.setText(journal.getGratitudeContent());
            } else {
                previewText.setText("(no text)");
            }

            // Show indicators for image and gratitude content
            hasImageIndicator.setVisibility(journal.hasImage() ? View.VISIBLE : View.GONE);
            hasGratitudeIndicator.setVisibility(journal.hasGratitude() ? View.VISIBLE : View.GONE);
        }
    }
} 