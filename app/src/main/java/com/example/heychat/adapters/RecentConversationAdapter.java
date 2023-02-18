package com.example.heychat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.databinding.ItemContainerRecentConversionBinding;
import com.example.heychat.listeners.ConversionListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.models.FontSize;
import com.example.heychat.models.Group;
import com.example.heychat.models.User;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversionViewHolder> {

    private final List<ChatMessage> chatMessages;
    private final ConversionListener conversionListener;
    private FirebaseFirestore database = FirebaseFirestore.getInstance();

    public RecentConversationAdapter(List<ChatMessage> chatMessages, ConversionListener conversionListener) {
        this.chatMessages = chatMessages;
        this.conversionListener = conversionListener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(
                ItemContainerRecentConversionBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(chatMessages.get(position));
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {

        ItemContainerRecentConversionBinding binding;

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding) {
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage) {
//            if (chatMessage.type.equals(Constants.MESSAGE_IMAGE)){
//                binding.imageview.setVisibility(View.VISIBLE);
//            } else  if (chatMessage.type.equals(Constants.MESSAGE_TEXT)){
//                binding.imageview.setVisibility(View.GONE);
//            }
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            binding.textRecentMessage.setText(chatMessage.message);
            PreferenceManager preferenceManager = new PreferenceManager(this.itemView.getContext());
            binding.textName.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE))+1);
            binding.textRecentMessage.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE)));
//            Log.d("OOOO", "++"+chatMessage.type);

            binding.getRoot().setOnClickListener(view -> {
                FirebaseFirestore database = FirebaseFirestore.getInstance();
                database.collection(Constants.KEY_COLLECTION_GROUP).get().addOnSuccessListener(queryDocumentSnapshots -> {


                    ArrayList<String> groupIds = new ArrayList<>();
                    for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                        groupIds.add(queryDocumentSnapshot.getId());
                    }
                    groupIds.add("1");

                    if (groupIds.contains(chatMessage.receiverId)) {
                        Group group = new Group();
                        group.id = chatMessage.receiverId;
                        group.name = chatMessage.conversionName;
                        group.image = chatMessage.conversionImage;
                        conversionListener.onConversionClicker(group);

                    } else {
                        User user = new User();
                        user.id = chatMessage.conversionId;
                        user.name = chatMessage.conversionName;
                        user.image = chatMessage.conversionImage;
                        conversionListener.onConversionClicker(user);
                    }

                });


            });
        }

        private Bitmap getConversionImage(String encodedImage) {
            if (encodedImage != null) {
                byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
                return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            } else {
                return null;
            }
        }

    }

}
