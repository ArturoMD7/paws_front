package com.example.pawss;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> postList;
    private OnPostClickListener clickListener;
    private OnPostDeleteListener deleteListener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public interface OnPostDeleteListener {
        void onDeletePost(Post post);
    }

    public PostAdapter(List<Post> postList, OnPostClickListener clickListener, OnPostDeleteListener deleteListener) {
        this.postList = postList;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_fotos, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private TextView tvPetName, tvContent, tvAuthor, tvDate;
        private ImageView ivPostImage, btnDelete;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPetName = itemView.findViewById(R.id.tvPetName);
            tvContent = itemView.findViewById(R.id.tvContent);
            tvAuthor = itemView.findViewById(R.id.tvAuthor);
            tvDate = itemView.findViewById(R.id.tvDate);
            ivPostImage = itemView.findViewById(R.id.ivPostImage);
            btnDelete = itemView.findViewById(R.id.btnDelete);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    clickListener.onPostClick(postList.get(position));
                }
            });

            btnDelete.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    deleteListener.onDeletePost(postList.get(position));
                }
            });
        }

        public void bind(Post post) {
            tvPetName.setText(post.getPetName());
            tvContent.setText(post.getContent());
            tvAuthor.setText(post.getAuthorName());
            tvDate.setText(post.getCreatedAt());

            if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
                Picasso.get()
                        .load(post.getImageUrls().get(0))
                        .placeholder(R.drawable.photoframe)
                        .error(R.drawable.warning)
                        .into(ivPostImage);
                ivPostImage.setVisibility(View.VISIBLE);
            } else {
                ivPostImage.setVisibility(View.GONE);
            }
        }
    }
}