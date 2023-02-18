package com.example.heychat.listeners;

import com.example.heychat.models.Group;
import com.example.heychat.models.User;

public interface ConversionListener {
    void onConversionClicker(User user);
    void onConversionClicker(Group group);
}
