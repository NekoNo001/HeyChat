package com.example.heychat.fragments;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import com.example.heychat.R;
import com.example.heychat.adapters.ChatBottomSheetFragment;
import com.example.heychat.adapters.ChatGroupBottomSheetFragment;
import com.example.heychat.adapters.RecentConversationAdapter;
import com.example.heychat.listeners.ConversionListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.models.Group;
import com.example.heychat.models.User;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class HomeFragment extends Fragment implements ConversionListener {

    private RecyclerView recyclerView;
    private PreferenceManager preferenceManager;
    private List<ChatMessage> conversations;
    private RecentConversationAdapter conversationAdapter;
    private FirebaseFirestore database;
    private ProgressBar progressBar;


    public HomeFragment() {
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        recyclerView = view.findViewById(R.id.home_recyclerview);
        progressBar = view.findViewById(R.id.home_progressBar);

        preferenceManager = new PreferenceManager(getContext());
        init();
        getToken();
        listenConversation();
        return view;
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(conversations, this);
        recyclerView.setAdapter(conversationAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USER).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(e -> showToast("Unable to update token"));
    }


    private void listenConversation() {

        database.collection(Constants.KEY_COLLECTION_USER).document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_GROUP_ID).get().addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> groupIds = new ArrayList<>();
                    for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                        groupIds.add(queryDocumentSnapshot.getId());
                    }
                    groupIds.add("1");

                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                            .whereNotIn(Constants.KEY_RECEIVER_ID, groupIds)
                            .addSnapshotListener(eventListener);
                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                            .whereNotIn(Constants.KEY_RECEIVER_ID, groupIds)
                            .addSnapshotListener(eventListener);

                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .whereIn(Constants.KEY_RECEIVER_ID, groupIds)
                            .addSnapshotListener(eventListener);
                });

    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }

        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage = new ChatMessage();

                    chatMessage.senderId = senderId;
                    chatMessage.receiverId = receiverId;

                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    } else if (preferenceManager.getString(Constants.KEY_USER_ID).equals(receiverId)) {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    } else {
                        chatMessage.conversionImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }

                    chatMessage.type = documentChange.getDocument().getString(Constants.KEY_MESSAGE_TYPE);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dataObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversations.add(chatMessage);
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        //Log.d("BBB", "+++" + receiverId);
                        if (conversations.get(i).senderId.equals(senderId) && conversations.get(i).receiverId.equals(receiverId)) {
                            conversations.get(i).type = documentChange.getDocument().getString(Constants.KEY_MESSAGE_TYPE);
                            conversations.get(i).message = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).dataObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }
                    }
                }
            }
            Collections.sort(conversations, (obj1, obj2) -> obj2.dataObject.compareTo(obj1.dataObject));
            conversationAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(0);
            recyclerView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    };

    @Override
    public void onConversionClicker(User user) {
        ChatBottomSheetFragment bottomSheetFragment = ChatBottomSheetFragment.newInstance(user);
        bottomSheetFragment.show(getActivity().getSupportFragmentManager(), bottomSheetFragment.getTag());
    }

    @Override
    public void onConversionClicker(Group group) {
        Group groupIntent = new Group();
        groupIntent.id = group.id;
        groupIntent.image = group.image;
        groupIntent.name = group.name;
        //group = (Group) getIntent().getSerializableExtra(Constants.KEY_GROUP);
        database.collection(Constants.KEY_COLLECTION_GROUP).document(group.id)
                .collection(Constants.KEY_GROUP_MEMBER).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    ArrayList<String> members = new ArrayList<>();
                    for (QueryDocumentSnapshot queryDocumentSnapshot : queryDocumentSnapshots) {
                        String member = queryDocumentSnapshot.getId();
                        members.add(member);
                    }
                    groupIntent.member = members;
                    ChatGroupBottomSheetFragment bottomSheetDialog = ChatGroupBottomSheetFragment.newInstance(groupIntent);
                    bottomSheetDialog.show(getActivity().getSupportFragmentManager(), bottomSheetDialog.getTag());
                });

    }


}