package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.heychat.R;
import com.example.heychat.adapters.PrivateChatAdapter;
import com.example.heychat.listeners.MessageListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.models.RoomChat;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class PrivateChatActivity extends BaseActivity implements MessageListener {

    private FirebaseFirestore database;
    private RoomChat roomChat;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> chatMessages;
    private PrivateChatAdapter chatAdapter;

    private AppCompatImageView imageBack;
    private RecyclerView chatRecyclerView;
    private EditText inputMessage;
    private CardView layoutSend;
    private ProgressDialog pd;
    private ListenerRegistration listenerRegistration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_chat);
        init();
        setListener();
        listenMessages();
    }


    private void setListener() {

        layoutSend.setOnClickListener(v -> sendMessage());

        imageBack.setOnClickListener(view -> {
            deleteData();
            preferenceManager.remove(Constants.KEY_COLLECTION_ROOM);
            showToast(getString(R.string.Delete_All_Message));
            finish();
        });
    }

    private void deleteData() {
        listenerRegistration.remove();
        removelistenMessages();
        if (listenerRegistration == null) {
            Log.d("roomAmout", "listenerRegistration null");
        }
        chatMessages.clear();
        chatAdapter.notifyDataSetChanged();
        database.collection(Constants.KEY_COLLECTION_ROOM)
                .document(roomChat.id)
                .collection(Constants.KEY_ROOM_MEMBER)
                .get()
                .addOnCompleteListener(task -> {
                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                        database.collection(Constants.KEY_COLLECTION_ROOM)
                                .document(roomChat.id)
                                .collection(Constants.KEY_ROOM_MEMBER)
                                .document(queryDocumentSnapshot.getId())
                                .delete();
                    }
                });
        database.collection(Constants.KEY_COLLECTION_ROOM)
                .document(roomChat.id)
                .delete();

        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, roomChat.id)
                .get()
                .addOnCompleteListener(task -> {
                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                                .document(queryDocumentSnapshot.getId())
                                .delete();
                    }
                });
        preferenceManager.remove(Constants.KEY_COLLECTION_ROOM);

//        database.collection(Constants.KEY_COLLECTION_ROOM)
//                .get()
//                .addOnCompleteListener(task -> {
//                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
//                        if (Integer.parseInt(String.valueOf(queryDocumentSnapshot.get(Constants.KEY_AMOUNT_OF_ROOM))) == 0) {
//                            database.collection(Constants.KEY_COLLECTION_ROOM)
//                                    .document(queryDocumentSnapshot.getId())
//                                    .delete();
//                        }
//                    }
//                });
    }

    private void init() {
        database = FirebaseFirestore.getInstance();
        roomChat = (RoomChat) getIntent().getSerializableExtra(Constants.KEY_COLLECTION_ROOM);
        preferenceManager = new PreferenceManager(getApplicationContext());
        imageBack = findViewById(R.id.imageBack);
        chatRecyclerView = findViewById(R.id.chatRecyclerView);
        inputMessage = findViewById(R.id.inputeMessage);
        layoutSend = findViewById(R.id.layoutSend);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        pd = new ProgressDialog(this);
        pd.setMessage(getString(R.string.Wait));
        pd.setCancelable(false);
        pd.setButton(DialogInterface.BUTTON_NEGATIVE,getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                pd.dismiss();//dismiss dialog
                deleteData();
                finish();
            }
        });
        pd.show();
        listenerRegistration = database.collection(Constants.KEY_COLLECTION_ROOM).document(roomChat.id)
                .addSnapshotListener(roomListener);

        chatMessages = new ArrayList<>();
        chatAdapter = new PrivateChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID),
                this
        );
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setItemAnimator(null);


    }

    private final EventListener<DocumentSnapshot> roomListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            String amount = value.getString(Constants.KEY_AMOUNT_OF_ROOM);
            if (amount == null) {
                deleteData();
                listenerRegistration.remove();
                Log.d("roomAmout", "null");
                listenerRegistration = null;
                if (listenerRegistration == null) {
                    Log.d("roomAmout", "listenerRegistration null");
                }
                createRoomChat();
                return;
            } else {
                Log.d("roomAmout", amount);
                if (amount.equals("1")) {
                    pd.show();
                } else if (amount.equals("2")) {
                    pd.dismiss();
                }
            }
        }
    };

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, roomChat.id);
        message.put(Constants.KEY_MESSAGE, inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT).add(message);
        inputMessage.setText(null);
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, roomChat.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, roomChat.id)
                .addSnapshotListener(eventListener);
    }

    private void removelistenMessages(){
        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, roomChat.id)
                .addSnapshotListener(eventListener).remove();
        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, roomChat.id)
                .addSnapshotListener(eventListener).remove();
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dataObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }

            }

            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dataObject.compareTo(obj2.dataObject));
            if (chatMessages.size() == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }

        }
    };

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("dd MMMM, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onMessageSelection(Boolean isSelected, int position, List<ChatMessage> lastMessages, ChatMessage chatMessage) {
        final Dialog dialog = openDialog(R.layout.layout_dialog_message_selection);
        assert dialog != null;
        TextView textMessage = dialog.findViewById(R.id.textMessage);
        TextView textDateTime = dialog.findViewById(R.id.textDateTime);
        TextView textSeenMessage = dialog.findViewById(R.id.textSeenMessage);
        RelativeLayout layoutTranslate = dialog.findViewById(R.id.relativeLayoutTranslate);
        RelativeLayout layoutCopy = dialog.findViewById(R.id.relativeLayoutCopy);
        RelativeLayout layoutDetail = dialog.findViewById(R.id.relativeLayoutDetail);
        RelativeLayout layoutDelete = dialog.findViewById(R.id.relativeLayoutDelete);
        ImageView imageCheck = dialog.findViewById(R.id.imageCheck);
        ImageView imageTranslate = dialog.findViewById(R.id.imageTranslate);

        textMessage.setText(lastMessages.get(position).message);
        textDateTime.setText(lastMessages.get(position).dateTime);
        if (lastMessages.get(position).isSeen) {
            textSeenMessage.setText("Seen");
            imageCheck.setVisibility(View.VISIBLE);
        } else {
            textSeenMessage.setText("Delivered");
            imageCheck.setVisibility(View.GONE);
        }

        layoutDelete.setVisibility(View.GONE);
        textSeenMessage.setVisibility(View.GONE);
        imageCheck.setVisibility(View.GONE);
        layoutCopy.setVisibility(View.GONE);
        layoutDetail.setVisibility(View.GONE);

        layoutTranslate.setOnClickListener(view -> {
            if (textMessage.getText().toString().equals(chatMessage.message)) {
                TranslatorOptions options;
                if (Objects.equals(preferenceManager.getString(Constants.KEY_LANGUAGE), "VI")) {
                    options = new TranslatorOptions.Builder()
                            .setSourceLanguage(TranslateLanguage.ENGLISH)
                            .setTargetLanguage(TranslateLanguage.VIETNAMESE)
                            .build();
                } else {
                    options = new TranslatorOptions.Builder()
                            .setSourceLanguage(TranslateLanguage.VIETNAMESE)
                            .setTargetLanguage(TranslateLanguage.ENGLISH)
                            .build();
                }

                Translator englishVITranslator = Translation.getClient(options);

                getLifecycle().addObserver(englishVITranslator);


                englishVITranslator.downloadModelIfNeeded().addOnSuccessListener(unused -> englishVITranslator.translate(chatMessage.message)
                        .addOnSuccessListener(textMessage::setText)
                        .addOnFailureListener(e -> showToast(e.getMessage()))).addOnFailureListener(e -> showToast(e.getMessage()));
                imageTranslate.setImageResource(R.drawable.ic_undo);
                showToast(getString(R.string.Translated_message));
            } else {
                imageTranslate.setImageResource(R.drawable.ic_translate);
                textMessage.setText(chatMessage.message);
            }
        });

        layoutCopy.setOnClickListener(view -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("message", textMessage.getText().toString());
            clipboard.setPrimaryClip(clip);
            showToast(getString(R.string.Copied_message));
            dialog.dismiss();
        });

        layoutDetail.setOnClickListener(view -> {

        });

//        layoutDelete.setOnClickListener(view -> {
//            chatMessages.remove(position);
//            chatAdapter.notifyItemRemoved(position);
//            chatAdapter.notifyItemChanged(position);
//            chatAdapter.notifyDataSetChanged();
//            chatAdapter.notifyItemRangeInserted(0, chatMessages.size());
//            updateDataOnFB(chatMessage.id);
//            dialog.dismiss();
//        });

        dialog.show();
    }

    private void createRoomChat() {
        roomChat = new RoomChat();
        database.collection(Constants.KEY_COLLECTION_ROOM)
                .get()
                .addOnCompleteListener(task -> {
                    Boolean isExist = false;
                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                        if (Objects.equals(queryDocumentSnapshot.getString(Constants.KEY_AMOUNT_OF_ROOM), "1")) {
                            HashMap<String, Object> room = new HashMap<>();
                            room.put(Constants.KEY_AMOUNT_OF_ROOM, "2");
                            HashMap<String, Object> member = new HashMap<>();
                            member.put(Constants.KEY_ROOM_MEMBER, preferenceManager.getString(Constants.KEY_USER_ID));
                            database.collection(Constants.KEY_COLLECTION_ROOM)
                                    .document(queryDocumentSnapshot.getId())
                                    .update(room);
                            database.collection(Constants.KEY_COLLECTION_ROOM)
                                    .document(queryDocumentSnapshot.getId())
                                    .collection(Constants.KEY_ROOM_MEMBER)
                                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                                    .set(member);
                            preferenceManager.putString(Constants.KEY_COLLECTION_ROOM, queryDocumentSnapshot.getId());
                            roomChat.id = queryDocumentSnapshot.getId();
                            listenerRegistration = database.collection(Constants.KEY_COLLECTION_ROOM).document(roomChat.id).addSnapshotListener(roomListener);
                            listenMessages();
                            isExist = true;
                            break;
                        } else if (Objects.equals(queryDocumentSnapshot.getString(Constants.KEY_AMOUNT_OF_ROOM), "2")) {
                            isExist = false;
                        }
                    }
                    if (!isExist) {
                        HashMap<String, Object> room = new HashMap<>();
                        room.put(Constants.KEY_AMOUNT_OF_ROOM, "1");
                        HashMap<String, Object> member = new HashMap<>();
                        member.put(Constants.KEY_ROOM_MEMBER, preferenceManager.getString(Constants.KEY_USER_ID));

                        database.collection(Constants.KEY_COLLECTION_ROOM)
                                .add(room)
                                .addOnCompleteListener(task1 -> {
                                    DocumentReference documentReference = task1.getResult();

                                    preferenceManager.putString(Constants.KEY_COLLECTION_ROOM, task1.getResult().getId());
                                    database.collection(Constants.KEY_COLLECTION_ROOM)
                                            .document(documentReference.getId())
                                            .collection(Constants.KEY_ROOM_MEMBER)
                                            .document(preferenceManager.getString(Constants.KEY_USER_ID))
                                            .set(member);

                                    roomChat.id = task1.getResult().getId();
                                    pd.show();
                                    listenerRegistration = database.collection(Constants.KEY_COLLECTION_ROOM).document(roomChat.id).addSnapshotListener(roomListener);
                                    listenMessages();
                                });
                    }
                });
        roomChat.id = preferenceManager.getString(Constants.KEY_COLLECTION_ROOM);
//        Log.d("roomAmout", "roomChat "+roomChat.id);
//        listenerRegistration = database.collection(Constants.KEY_COLLECTION_ROOM).document(roomChat.id).addSnapshotListener(roomListener);
    }

    private void updateDataOnFB(String key) {
        database.collection(Constants.KEY_COLLECTION_PRIVATE_CHAT)
                .document(key)
                .delete()
                .addOnSuccessListener(unused -> showToast(getString(R.string.Delete_Message)))
                .addOnFailureListener(e -> showToast(e.getMessage()));
    }


    private Dialog openDialog(int layout) {
        final Dialog dialog = new Dialog(PrivateChatActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(layout);
        dialog.setCancelable(true);
        Window window = dialog.getWindow();
        if (window == null) {
            return null;
        }
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        WindowManager.LayoutParams windowAttributes = window.getAttributes();
        windowAttributes.gravity = Gravity.BOTTOM;
        window.setAttributes(windowAttributes);

        return dialog;
    }

    @Override
    protected void onStop() {
        super.onStop();
        listenerRegistration.remove();
    }

    @Override
    public void finishActivity(int requestCode) {
        super.finishActivity(requestCode);
        deleteData();
    }
}