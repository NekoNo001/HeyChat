package com.example.heychat.listeners;

import com.example.heychat.models.ChatMessage;

import java.util.List;

public interface MessageListener {

    void onMessageSelection(Boolean isSelected, int position, List<ChatMessage> chatMessages, ChatMessage chatMessage);

}
