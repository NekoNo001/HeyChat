package com.example.heychat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.R;
import com.example.heychat.listeners.UserListener;
import com.example.heychat.models.User;

import java.util.List;

public class UserOnlineAdapter extends RecyclerView.Adapter<UserOnlineAdapter.OnlineViewHolder> {
    private final List<User> users;
    private final UserListener userListener;


    public UserOnlineAdapter(List<User> users, UserListener userListener) {
        this.users = users;
        this.userListener = userListener;
    }

    @NonNull
    @Override
    public OnlineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new OnlineViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.users_online_layout, parent,false));
    }

    @Override
    public void onBindViewHolder(@NonNull OnlineViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class OnlineViewHolder extends RecyclerView.ViewHolder{
        private final ImageView imageView;

        public OnlineViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.user_online_image);
        }

        void setUserData(User user){
            imageView.setImageBitmap(getUserImage(user.image));
            imageView.setOnClickListener(view -> userListener.onUserClicker(user));
        }

    }

    private Bitmap getUserImage(String encodedImage){
        byte[] bytes = new byte[0];
        if (encodedImage != null){
            bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        }

        return BitmapFactory.decodeByteArray(bytes,0, bytes.length);
    }

}
