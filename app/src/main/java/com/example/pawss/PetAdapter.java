package com.example.pawss;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {
    private List<Pet> petList;
    private final OnPetClickListener onPetClickListener;
    private final OnPetDeleteListener onPetDeleteListener;

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    public interface OnPetDeleteListener {
        void onDeletePet(Pet pet);
    }

    public PetAdapter(List<Pet> petList, OnPetClickListener onPetClickListener,
                      OnPetDeleteListener onPetDeleteListener) {
        this.petList = petList;
        this.onPetClickListener = onPetClickListener;
        this.onPetDeleteListener = onPetDeleteListener;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        Pet pet = petList.get(position);

        holder.tvPetName.setText(pet.getName());
        holder.tvPetType.setText("Tipo: " + pet.getType());
        holder.tvPetAge.setText("Edad: " + pet.getAge() + " años");
        holder.tvPetBreed.setText("Raza: " + pet.getBreed());

        String imageUrl = pet.getPhotoUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Picasso.get()
                    .load(imageUrl)
                    .placeholder(R.drawable.photoframe)
                    .error(R.drawable.warning)
                    .fit()
                    .centerCrop()
                    .into(holder.ivPetPhoto, new Callback() {
                        @Override
                        public void onSuccess() {
                            Log.d("PetAdapter", "Imagen cargada: " + imageUrl);
                        }

                        @Override
                        public void onError(Exception e) {
                            Log.e("PetAdapter", "Error al cargar imagen: " + imageUrl, e);
                            holder.ivPetPhoto.setImageResource(R.drawable.happy);
                        }
                    });
        } else {
            holder.ivPetPhoto.setImageResource(R.drawable.happy);
        }

        holder.itemView.setOnClickListener(v -> onPetClickListener.onPetClick(pet));
        holder.btnDelete.setOnClickListener(v -> onPetDeleteListener.onDeletePet(pet));
    }

    @Override
    public int getItemCount() {
        return petList.size();
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        TextView tvPetName, tvPetType, tvPetAge, tvPetBreed;
        ImageView ivPetPhoto, btnDelete;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPetName = itemView.findViewById(R.id.tvPetName);
            tvPetType = itemView.findViewById(R.id.tvPetType);
            tvPetAge = itemView.findViewById(R.id.tvPetAge);
            tvPetBreed = itemView.findViewById(R.id.tvPetBreed);
            ivPetPhoto = itemView.findViewById(R.id.ivPetPhoto);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}