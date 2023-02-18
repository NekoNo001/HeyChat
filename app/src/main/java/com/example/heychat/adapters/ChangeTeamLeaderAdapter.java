package com.example.heychat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.databinding.ItemContainerUserBinding;
import com.example.heychat.listeners.GroupMemberListener;
import com.example.heychat.listeners.UserListener;
import com.example.heychat.models.User;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;

import java.util.List;

public class ChangeTeamLeaderAdapter extends RecyclerView.Adapter<ChangeTeamLeaderAdapter.UserViewHolder>{

    private final List<User> users;
    private final GroupMemberListener groupMemberListener;

    public ChangeTeamLeaderAdapter(List<User> users, GroupMemberListener groupMemberListener) {
        this.users = users;
        this.groupMemberListener = groupMemberListener;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()),parent,false
        );

        return new UserViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class UserViewHolder extends RecyclerView.ViewHolder{

        ItemContainerUserBinding binding;
        UserViewHolder(ItemContainerUserBinding itemContainerUserBinding){
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        void setUserData(User user){
            binding.textName.setText(user.name);
            binding.textEmail.setText(user.email);
            binding.imageProfile.setImageBitmap(getUserImage(user.image));
            PreferenceManager preferenceManager = new PreferenceManager(itemView.getContext());
            binding.textName.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE)));
            binding.textEmail.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE))-4);
            binding.getRoot().setOnClickListener(v -> groupMemberListener.onChangeTeamLeadClicker(user));
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
