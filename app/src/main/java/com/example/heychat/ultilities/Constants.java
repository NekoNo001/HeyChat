package com.example.heychat.ultilities;

import java.util.HashMap;

import retrofit2.http.PUT;

public class Constants {
    public static final String KEY_COLLECTION_USER = "users";
    public static final String KEY_NAME = "name";
    public static final String KEY_EMAIL = "email";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_PREFERENCE_NAME = "heyChatPreference";
    public static final String KEY_IS_SIGNED_IN = "isSignedIn";
    public static final String KEY_USER_ID = "userId";
    public static final String KEY_IMAGE = "image";
    public static final String KEY_FCM_TOKEN = "fcmToken";
    public static final String KEY_USER = "user";
    public static final String KEY_COLLECTION_CHAT = "chat";
    public static final String KEY_SENDER_ID = "senderId";
    public static final String KEY_RECEIVER_ID = "receiverId";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_TIMESTAMP = "timestamp";
    public static final String KEY_COLLECTION_CONVERSATIONS = "conversations";
    public static final String KEY_SENDER_NAME = "senderName";
    public static final String KEY_RECEIVER_NAME = "receiverName";
    public static final String KEY_SENDER_IMAGE = "senderImage";
    public static final String KEY_RECEIVER_IMAGE = "receiverImage";
    public static final String KEY_LAST_MESSAGE = "lastMessage";
    public static final String KEY_AVAILABILITY = "availability";
    public static final String KEY_COLLECTION_CALL = "call";
    public static final String KEY_USER_CALL = "usercall";
    public static final String KEY_USER_RECEIVER = "userreceiver";
    public static final String KEY_CALL_DURATION = "duration";
    public static final String KEY_CALL_CAUSE = "cause";
    public static final String KEY_CALL_TYPE = "calltype";
    public static final String REMOTE_MSG_AUTHORIZATION ="Authorization";
    public static final String REMOTE_MSG_CONTENT_TYPE = "Content-Type";

    public static final String REMOTE_MSG_DATA = "data";
    public static final String REMOTE_MSG_REGISTRATION_IDS = "registration_ids";

    public static final String KEY_MESSAGE_TYPE = "type";
    public static final String MESSAGE_TEXT = "text";
    public static final String MESSAGE_IMAGE = "image";
    public static final String MESSAGE_FILE = "file";
    public static final String KEY_FILE = "download_file";

    public static final String REMOTE_MSG_TYPE = "type";
    public static final String REMOTE_MSG_INVITATION = "invitation";
    public static final String REMOTE_MSG_MEETING_TYPE = "meetingtype";
    public static final String REMOTE_MSG_INVITER_TOKEN = "invitertoken";

    public static final String REMOTE_MSG_INVITATION_RESPONSE = "invitationResponse";
    public static final String REMOTE_MSG_INVITATION_ACCEPTED = "accepted";
    public static final String REMOTE_MSG_INVITATION_REJECTED = "rejected";
    public static final String REMOTE_MSG_INVITATION_CANCELLED = "cancelled";

    public static final String REMOTE_MSG_MEETING_ROOM = "meetingRoom";

    public static final String KEY_COLLECTION_GROUP = "groups";
    public static final String KEY_GROUP_NAME = "groupName";
    public static final String KEY_GROUP_IMAGE = "GroupImage";
    public static final String KEY_GROUP_ID = "groupId";
    public static final String KEY_GROUP_MEMBER = "groupMember";
    public static final String KEY_GROUP = "group";
    public static final String KEY_COLLECTION_CHAT_GROUPS = "chatGroups";
    public static final String KEY_COLLECTION_CONVERSATIONS_GROUP = "conversationGroups";
    public static final String KEY_GROUP_OWNER = "owner";


    public static final String KEY_PRIVATE_ACCOUNT_NAME = "privateName";
    public static final String KEY_COLLECTION_ROOM = "rooms";
    public static final String KEY_AMOUNT_OF_ROOM = "amount";
    public static final String KEY_ROOM_MEMBER = "roomMember";
    public static final String KEY_COLLECTION_PRIVATE_CHAT = "privateChats";
    public static final String KEY_SEEN_MESSAGE = "seen";
    public static final String KEY_MEMBER_SEEN_MESSAGE = "memberSeen";
    public static final String KEY_SEEN_TIME = "seenTime";


    public static final String KEY_LANGUAGE = "language";
    public static final String KEY_TEXTSIZE = "textSize";
    public static final String KEY_BLOCK_SCREENSHOT = "blockScreenShot";
    public static final String KEY_EMPTY_ROOM = "emptyRoom";


    public static HashMap<String, String> remoteMsgHeaders = null;
    public  static HashMap<String, String> getRemoteMsgHeaders(){
        if(remoteMsgHeaders == null){
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(
                    REMOTE_MSG_AUTHORIZATION,
                    "key=AAAAtfm5WI8:APA91bFgQUl08EUT3XFI_ieSpjwqoUwMkIuKDF3zNq7OQobgoJt2dYWid9ATtbJAG1AnkYVtJl8J2TkZIghnTpd4mgZewErcydXHiNGrfByPC9guSFgQ3aa6bCE-QWFJWSO9OHZiDzfW"
            );
            remoteMsgHeaders.put(
                    REMOTE_MSG_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remoteMsgHeaders;
    }
}
