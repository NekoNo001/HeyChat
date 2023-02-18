package com.example.heychat.listeners;

import com.example.heychat.models.User;

public interface GroupMemberListener {
    void onGroupMemberSelection(Boolean isSelected);
    void onChangeTeamLeadClicker(User user);
}
