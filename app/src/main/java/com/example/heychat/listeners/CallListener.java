package com.example.heychat.listeners;

import com.example.heychat.models.User;

public interface CallListener {

    void initiateVideoCall(User user);

    void initiateAudioCall(User user);
}
