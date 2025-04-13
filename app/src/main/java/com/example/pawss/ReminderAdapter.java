package com.example.pawss;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
        void onDeleteReminder(Reminder reminder);
    }

    private List<Reminder> reminderList;
    private final OnReminderClickListener listener;

    public ReminderAdapter(List<Reminder> reminderList, OnReminderClickListener listener) {
        this.reminderList = reminderList;
        this.listener = listener;
    }

    public void updateReminders(List<Reminder> newReminders) {
        this.reminderList = newReminders;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReminderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_reminder, parent, false);
        return new ReminderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReminderViewHolder holder, int position) {
        Reminder reminder = reminderList.get(position);
        holder.bind(reminder, listener);
    }

    @Override
    public int getItemCount() {
        return reminderList != null ? reminderList.size() : 0;
    }

    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvDateTime;
        private final TextView tvAssignedTo;
        private final TextView tvPet;
        private final ImageView ivDelete;

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvReminderTitle);
            tvDateTime = itemView.findViewById(R.id.tvReminderDateTime);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            tvPet = itemView.findViewById(R.id.tvPet);
            ivDelete = itemView.findViewById(R.id.ivDeleteReminder);
        }

        public void bind(Reminder reminder, OnReminderClickListener listener) {
            tvTitle.setText(reminder.getTitle());
            tvDateTime.setText(reminder.getFormattedDateTime());
            tvAssignedTo.setText("Para: " + reminder.getAssignedToName());
            tvPet.setText("Mascota: " + reminder.getPetName());

            itemView.setOnClickListener(v -> listener.onReminderClick(reminder));
            ivDelete.setOnClickListener(v -> listener.onDeleteReminder(reminder));
        }
    }
}