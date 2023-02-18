package com.example.heychat.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.databinding.ItemContainerRecentConversionBinding;
import com.example.heychat.listeners.ConversationGroupListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.models.FontSize;
import com.example.heychat.models.Group;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;

import java.util.List;

public class RecentConversationGroupAdapter extends RecyclerView.Adapter<RecentConversationGroupAdapter.ConversionViewHolder>{

    private final List<ChatMessage> chatMessages;
    private final ConversationGroupListener conversionListener;

    public RecentConversationGroupAdapter(List<ChatMessage> chatMessages, ConversationGroupListener conversionListener) {
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

    class ConversionViewHolder extends RecyclerView.ViewHolder{

        ItemContainerRecentConversionBinding binding;

        ConversionViewHolder(ItemContainerRecentConversionBinding itemContainerRecentConversionBinding){
            super(itemContainerRecentConversionBinding.getRoot());
            binding = itemContainerRecentConversionBinding;
        }

        void setData(ChatMessage chatMessage){
            binding.imageProfile.setImageBitmap(getConversionImage(chatMessage.conversionImage));
            binding.textName.setText(chatMessage.conversionName);
            binding.textRecentMessage.setText(chatMessage.message);
            PreferenceManager preferenceManager = new PreferenceManager(this.itemView.getContext());
            binding.textRecentMessage.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE)));
            binding.getRoot().setOnClickListener(view -> {
                Group group = new Group();
                group.id = chatMessage.conversionId;
                group.name = chatMessage.conversionName;
                group.image = chatMessage.conversionImage;
                conversionListener.onConversionGroupClicker(group);
            });
        }

        private Bitmap getConversionImage(String encodedImage){
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }

    }

}
