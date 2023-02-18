package com.example.heychat.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


import com.example.heychat.R;
import com.example.heychat.databinding.ItemContainerUserBinding;
import com.example.heychat.listeners.GroupListener;
import com.example.heychat.models.Group;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupViewHolder> {

    private final List<Group> users;
    private final GroupListener groupListener;
    private FirebaseFirestore database;

    public GroupAdapter(List<Group> users, GroupListener groupListener) {
        this.users = users;
        this.groupListener = groupListener;
    }

    @NonNull
    @Override
    public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemContainerUserBinding itemContainerUserBinding = ItemContainerUserBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false
        );

        return new GroupViewHolder(itemContainerUserBinding);
    }

    @Override
    public void onBindViewHolder(@NonNull GroupViewHolder holder, int position) {
        holder.setUserData(users.get(position));
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    class GroupViewHolder extends RecyclerView.ViewHolder {

        ItemContainerUserBinding binding;

        GroupViewHolder(ItemContainerUserBinding itemContainerUserBinding) {
            super(itemContainerUserBinding.getRoot());
            binding = itemContainerUserBinding;
        }

        void setUserData(Group group) {
            binding.textName.setText(group.name);
            PreferenceManager preferenceManager = new PreferenceManager(itemView.getContext());
            binding.textName.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE)));
            binding.textEmail.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE))-4);
            binding.imageProfile.setImageBitmap(getUserImage(group.image));
            database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_GROUP)
                    .document(group.id)
                    .get()
                    .addOnCompleteListener(task -> {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        database.collection(Constants.KEY_COLLECTION_USER)
                                .document(documentSnapshot.getString(Constants.KEY_GROUP_OWNER))
                                .get()
                                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        DocumentSnapshot documentSnapshot1 = task.getResult();
                                        String text = itemView.getContext().getString(R.string.Lead) + " " + documentSnapshot1.getString(Constants.KEY_NAME);
                                        binding.textEmail.setText(text);
                                    }
                                });

                    });
            binding.getRoot().setOnClickListener(v -> groupListener.onGroupClicker(group));
        }

    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = new byte[0];
        if (encodedImage != null) {
            bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        }

        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
