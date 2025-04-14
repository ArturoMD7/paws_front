package com.example.pawss;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReminderAdapter extends RecyclerView.Adapter<ReminderAdapter.ReminderViewHolder> {

    public interface OnReminderClickListener {
        void onReminderClick(Reminder reminder);
        void onDeleteReminder(Reminder reminder);

        void onCompleteReminder(Reminder reminder);
    }

    private List<Reminder> reminderList;
    private final OnReminderClickListener listener;
    private Context context = null;

    public ReminderAdapter(List<Reminder> reminderList, OnReminderClickListener listener) {
        this.reminderList = reminderList;
        this.listener = listener;
        this.context = context;
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

        holder.tvReminderTitle.setText(reminder.getTitle());

        // Mostrar información de asignación y mascota
        holder.tvAssignedTo.setText("Asignado a: " + reminder.getAssignedToName());

        if (reminder.getPet() != null) {
            holder.tvPet.setText("Mascota: " + reminder.getPetName());
        } else {
            holder.tvPet.setText("Mascota: No especificada");
        }

        // Formatear fecha
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date date = sdf.parse(reminder.getDueDate());
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            holder.tvReminderDateTime.setText(outputFormat.format(date));
        } catch (Exception e) {
            holder.tvReminderDateTime.setText(reminder.getDueDate());
        }
        holder.bind(reminder, listener);
    }

    @Override
    public int getItemCount() {
        return reminderList != null ? reminderList.size() : 0;
    }

    // En ReminderAdapter.java
    static class ReminderViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvReminderTitle;
        private final TextView tvReminderDateTime;
        private final TextView tvAssignedTo;
        private final TextView tvPet;
        private final TextView tvStatus;
        private final ImageView ivDeleteReminder;
        private final Button btnComplete; // Añade esta línea

        public ReminderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvReminderTitle = itemView.findViewById(R.id.tvReminderTitle);
            tvReminderDateTime = itemView.findViewById(R.id.tvReminderDateTime);
            tvAssignedTo = itemView.findViewById(R.id.tvAssignedTo);
            tvPet = itemView.findViewById(R.id.tvPet);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            ivDeleteReminder = itemView.findViewById(R.id.ivDeleteReminder);
            btnComplete = itemView.findViewById(R.id.btnComplete); // Añade esta línea
        }

        public void bind(final Reminder reminder, final OnReminderClickListener listener) {
            tvReminderTitle.setText(reminder.getTitle());
            tvReminderDateTime.setText(formatDateTime(reminder.getDueDate()));
            tvAssignedTo.setText("Asignado a: " + (reminder.getAssignedTo() != null ? reminder.getAssignedTo().name : "Todos"));
            tvPet.setText("Mascota: " + (reminder.getPet() != null ? reminder.getPet().name : "Ninguna"));

            // Mostrar estado
            if ("COMPLETED".equals(reminder.getStatus())) {
                tvStatus.setText("Completado");
                tvStatus.setTextColor(Color.GREEN);
            } else if (isOverdue(reminder.getDueDate())) {
                tvStatus.setText("Vencido");
                tvStatus.setTextColor(Color.RED);
            } else {
                tvStatus.setText("Pendiente");
                tvStatus.setTextColor(Color.YELLOW);
            }

            // Configurar botón de completar
            if ("PENDING".equals(reminder.getStatus())) {
                btnComplete.setVisibility(View.VISIBLE);
                btnComplete.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onCompleteReminder(reminder);
                    }
                });
            } else {
                btnComplete.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReminderClick(reminder);
                }
            });

            ivDeleteReminder.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteReminder(reminder);
                }
            });
        }

        private String formatDateTime(String dueDate) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                Date date = sdf.parse(dueDate);
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                return outputFormat.format(date);
            } catch (ParseException e) {
                return dueDate;
            }
        }

        private boolean isOverdue(String dueDate) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
                Date date = sdf.parse(dueDate);
                return date != null && date.before(new Date());
            } catch (ParseException e) {
                return false;
            }
        }
    }
}