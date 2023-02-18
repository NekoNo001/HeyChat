package com.example.heychat.adapters;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.R;
import com.example.heychat.activities.InfoGroupActivity;
import com.example.heychat.listeners.MessageListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.models.Group;
import com.example.heychat.network.ApiClient;
import com.example.heychat.network.ApiService;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import gun0912.tedbottompicker.TedBottomPicker;
import gun0912.tedbottompicker.TedBottomSheetDialogFragment;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatGroupBottomSheetFragment extends BottomSheetDialogFragment implements MessageListener {

    private AppCompatImageView imageBack;
    private static TextView textName;
    private RecyclerView chatRecyclerView;
    private EditText inputeMessage;
    private View layoutSend, layoutImage, layoutAttact;
    private Group receiverUser;
    private List<ChatMessage> chatMessages;
    private ChatGroupAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversationId = null;
    private Boolean isReceiverAvailable = false;
    private String encodedImage;

    private TextView textSuggestion1, textSuggestion2, textSuggestion3, textSuggestion4, textSuggestion5;
    private ConstraintLayout layoutSuggestions;

    public static ChatGroupBottomSheetFragment newInstance(Group group) {
        ChatGroupBottomSheetFragment chatBottomSheetFragment = new ChatGroupBottomSheetFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Constants.KEY_GROUP, group);
        chatBottomSheetFragment.setArguments(bundle);
        return chatBottomSheetFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundleReceive = getArguments();
        if (bundleReceive != null) {
            receiverUser = (Group) bundleReceive.get(Constants.KEY_GROUP);
//            textName.setText(receiverUser.name);
        }

    }

    @SuppressLint("ResourceAsColor")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) new BottomSheetDialog(getContext(), R.style.ChatBottomSheet);
        Window window = bottomSheetDialog.getWindow();
        if (window == null) {
            return null;
        }
//        window.setBackgroundDrawableResource(R.color.primary);
        View viewDialog = LayoutInflater.from(getContext()).inflate(R.layout.activity_chat, null);
        bottomSheetDialog.setContentView(viewDialog);
        initView(viewDialog);
        init();
        setListeners();
        readMessages();

        return bottomSheetDialog;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        listenAvailabilityOfReceiver();
    }

    private void init() {
        preferenceManager = new PreferenceManager(getContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatGroupAdapter(
                chatMessages,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID),
                this
        );
        chatRecyclerView.setAdapter(chatAdapter);
        chatRecyclerView.setItemAnimator(null);
        database = FirebaseFirestore.getInstance();

    }

    private void initView(View view) {
        imageBack = view.findViewById(R.id.imageBack);
        textName = view.findViewById(R.id.textName);
        textSuggestion1 = view.findViewById(R.id.textSuggestion1);
        textSuggestion2 = view.findViewById(R.id.textSuggestion2);
        textSuggestion3 = view.findViewById(R.id.textSuggestion3);
        textSuggestion4 = view.findViewById(R.id.textSuggestion4);
        textSuggestion5 = view.findViewById(R.id.textSuggestion5);
        chatRecyclerView = view.findViewById(R.id.chatRecyclerView);
        inputeMessage = view.findViewById(R.id.inputeMessage);
        layoutSend = view.findViewById(R.id.layoutSend);
        layoutImage = view.findViewById(R.id.layoutImage);
        layoutAttact = view.findViewById(R.id.layoutAttact);
        layoutSuggestions = view.findViewById(R.id.layoutSuggestions);
        ImageView videoCall = view.findViewById(R.id.video_call_btn_chat_act);
        ImageView audioCall = view.findViewById(R.id.audio_call_btn_chat_act);
        videoCall.setVisibility(View.INVISIBLE);
        audioCall.setVisibility(View.INVISIBLE);
        textName.setText(receiverUser.name);
        textName.setOnClickListener(view1 -> {
            Intent intent = new Intent(getContext(), InfoGroupActivity.class);
            intent.putExtra(Constants.KEY_GROUP, receiverUser);
            startActivity(intent);
        });

        inputeMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (inputeMessage.getText().toString().isEmpty()) {
                    setBtnVisible(false);
                } else {
                    setBtnVisible(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void readMessages() {

        //Group receiverUser = (Group) getIntent().getSerializableExtra(Constants.KEY_GROUP);
        ArrayList<String> member = (ArrayList<String>) receiverUser.member;

        for (String item : member) {

            //database.collection(Constants.KEY_COLLECTION_USER)
            //.whereEqualTo(Constants.KEY_EMAIL, item)
            //.get()
            //.addOnCompleteListener(task -> {
            //if (task.isSuccessful() && task.getResult() != null) {
            //for (QueryDocumentSnapshot ignored : task.getResult()) {
            //Log.d("KKK", item + ": " + queryDocumentSnapshot.getId());
            if (item.equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS)
                        .get()
                        .addOnCompleteListener(task ->
                                database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS)
                                        .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                                        .addSnapshotListener(eventListener));
            }
        }
        //}
        // });
        //}
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
                    if (!Objects.equals(documentChange.getDocument().getString(Constants.KEY_SENDER_ID), preferenceManager.getString(Constants.KEY_USER_ID))) {
                        HashMap<String, Object> id = new HashMap<>();
                        id.put(Constants.KEY_MEMBER_SEEN_MESSAGE, preferenceManager.getString(Constants.KEY_USER_ID));
                        database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS)
                                .document(documentChange.getDocument().getId())
                                .collection(Constants.KEY_MEMBER_SEEN_MESSAGE)
                                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                                .set(id);
                    }

                    if (Boolean.FALSE.equals(documentChange.getDocument().getBoolean(Constants.KEY_SEEN_MESSAGE))) {
                        HashMap<String, Object> seen = new HashMap<>();
                        seen.put(Constants.KEY_SEEN_MESSAGE, true);
                        seen.put(Constants.KEY_SEEN_TIME, new Date());
                        database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS)
                                .document(documentChange.getDocument().getId())
                                .update(seen);
                    }
                    ChatMessage chatMessage = new ChatMessage();
                    chatMessage.id = documentChange.getDocument().getId();
                    chatMessage.type = documentChange.getDocument().getString(Constants.KEY_MESSAGE_TYPE);
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dataObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.seenTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_SEEN_TIME));

                    if (Objects.equals(chatMessage.senderId, preferenceManager.getString(Constants.KEY_USER_ID)))
                        chatMessage.model = "sender";
                    else chatMessage.model = "receiver";
                    chatMessage.isSeen = documentChange.getDocument().getBoolean(Constants.KEY_SEEN_MESSAGE);
                    chatMessages.add(chatMessage);
                }
            }

            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dataObject.compareTo(obj2.dataObject));
            if (count == 0) {
                chatAdapter.notifyItemRangeInserted(0, chatAdapter.getItemCount());
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
        }
        if (conversationId == null) {
            checkForConversion();
        }
    };

    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_GROUP).document(
                receiverUser.id
        ).addSnapshotListener(this.getActivity(), (value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if (receiverUser.image == null) {
                    receiverUser.image = value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodedString(receiverUser.image));
                    chatAdapter.notifyItemRangeInserted(0, chatMessages.size());
                }
            }

        });
    }

    private void setListeners() {
        imageBack.setOnClickListener(view -> {
            dismiss();
        });
        layoutSend.setOnClickListener(v -> sendMessage(inputeMessage.getText().toString()));
        layoutImage.setOnClickListener(v -> requestImagePermission());
        layoutAttact.setOnClickListener(v -> requestFilePermission());

        textSuggestion1.setOnClickListener(view -> {
            sendMessage(textSuggestion1.getText().toString());
            layoutSuggestions.setVisibility(View.GONE);
        });

        textSuggestion2.setOnClickListener(view -> {
            sendMessage(textSuggestion2.getText().toString());
            layoutSuggestions.setVisibility(View.GONE);
        });

        textSuggestion3.setOnClickListener(view -> {
            sendMessage(textSuggestion3.getText().toString());
            layoutSuggestions.setVisibility(View.GONE);
        });

        textSuggestion4.setOnClickListener(view -> {
            sendMessage(textSuggestion4.getText().toString());
            layoutSuggestions.setVisibility(View.GONE);
        });

        textSuggestion5.setOnClickListener(view -> {
            sendMessage(textSuggestion5.getText().toString());
            layoutSuggestions.setVisibility(View.GONE);
        });
    }

    private void requestImagePermission() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                openImagePicker();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(getContext(), getString(R.string.PermissionDenied) + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage(getString(R.string.PermissionDeniedNoice))
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();
    }

    private void requestFilePermission() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                openFileChoser();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(getContext(), getString(R.string.PermissionDenied)  + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage(getString(R.string.PermissionDeniedNoice))
                .setPermissions(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .check();

    }

    private void openFileChoser() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent = Intent.createChooser(intent, "Chose a file");
        pickFileActivity.launch(intent);
    }


    ActivityResultLauncher<Intent> pickFileActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent data = result.getData();
                Uri uri = data.getData();

//                inputeMessage.setText(uri.toString());

//                File file = new File(uri.toString());
                String path = new File(uri.toString()).getAbsolutePath();

                if (path != null) {
                    String filename;
                    Cursor cursor = getContext().getContentResolver().query(uri, null, null, null, null);

                    if (cursor == null) filename = uri.getPath();
                    else {
                        cursor.moveToFirst();
                        int idx = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                        filename = cursor.getString(idx);
                        cursor.close();
                    }

                    String name = filename.substring(0, filename.lastIndexOf("."));
                    String extension = filename.substring(filename.lastIndexOf(".") + 1);


                    uploadFile(uri, name, extension);
                }


            }
        }
    });

    private void uploadFile(Uri uri, String fileName, String fileExtension) {
        final ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setTitle(getString(R.string.Uploading));
        progressDialog.show();

        StorageReference reference = FirebaseStorage.getInstance().getReference().child(preferenceManager.getString(Constants.KEY_USER_ID))
                .child(fileName + "--__" + System.currentTimeMillis() + "." + fileExtension);
        reference.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while (!uriTask.isComplete()) ;
                        progressDialog.dismiss();
                        taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                sendFile(uri.toString());
                            }
                        });
                    }
                }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                        double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                        progressDialog.setMessage(getString(R.string.Uploaded) + (int) progress + "%");
                    }
                });

    }

    private void sendFile(String download) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_MESSAGE_TYPE, Constants.MESSAGE_FILE);
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, download);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put(Constants.KEY_SEEN_MESSAGE, false);
        message.put(Constants.KEY_SEEN_TIME, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS).add(message);
        if (conversationId != null) {
            updateConversion(Constants.MESSAGE_FILE, Constants.MESSAGE_FILE);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_MESSAGE_TYPE, Constants.MESSAGE_FILE);
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, Constants.MESSAGE_FILE);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {

                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();

                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, Constants.MESSAGE_FILE);

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());

            } catch (Exception e) {
                //showToast(e.getMessage());
            }
        }
        inputeMessage.setText(null);
    }

    private void openImagePicker() {

        TedBottomPicker.with(this.getActivity())
                .show(new TedBottomSheetDialogFragment.OnImageSelectedListener() {
                    @Override
                    public void onImageSelected(Uri uri) {
                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContext().getContentResolver(), uri);
                            encodedImage = encodeImage(bitmap);
                            sendImage();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });

    }

    private void sendImage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_MESSAGE_TYPE, Constants.MESSAGE_IMAGE);
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, encodedImage);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put(Constants.KEY_SEEN_MESSAGE, false);
        message.put(Constants.KEY_SEEN_TIME, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS).add(message);
        if (conversationId != null) {
            updateConversion(Constants.MESSAGE_IMAGE, Constants.MESSAGE_IMAGE);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_MESSAGE_TYPE, Constants.MESSAGE_IMAGE);
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, Constants.MESSAGE_IMAGE);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {

                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();

                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, inputeMessage.getText().toString());

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());

            } catch (Exception e) {
                //showToast(e.getMessage());
            }
        }
        inputeMessage.setText(null);
    }

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = bitmap.getWidth() / 2;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private void sendMessage(String text) {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_MESSAGE_TYPE, Constants.MESSAGE_TEXT);
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, text);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put(Constants.KEY_SEEN_MESSAGE, false);
        message.put(Constants.KEY_SEEN_TIME, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS).add(message);

        if (conversationId != null) {
            updateConversion(text, Constants.MESSAGE_TEXT);
        } else {
            HashMap<String, Object> conversion = new HashMap<>();
            conversion.put(Constants.KEY_MESSAGE_TYPE, Constants.MESSAGE_TEXT);
            conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversion.put(Constants.KEY_LAST_MESSAGE, text);
            conversion.put(Constants.KEY_TIMESTAMP, new Date());
            addConversion(conversion);
        }
        if (!isReceiverAvailable) {
            try {

                JSONArray tokens = new JSONArray();
                tokens.put(receiverUser.token);

                JSONObject data = new JSONObject();
                data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                data.put(Constants.KEY_MESSAGE, text);

                JSONObject body = new JSONObject();
                body.put(Constants.REMOTE_MSG_DATA, data);
                body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

                sendNotification(body.toString());

            } catch (Exception e) {
                //showToast(e.getMessage());
            }
        }
        inputeMessage.setText(null);
    }

    private void sendNotification(String messageBody) {
        ApiClient.getClient().create(ApiService.class).sendMessage(
                Constants.getRemoteMsgHeaders(),
                messageBody
        ).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject responseJson = new JSONObject(response.body());
                            JSONArray results = responseJson.getJSONArray("results");
                            if (responseJson.getInt("failure") == 1) {
                                JSONObject error = (JSONObject) results.get(0);
                                //showToast(error.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //showToast("Notification sent successfully!");
                } else {
                    //showToast("Error: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversationId = documentReference.getId());
        //Log.d("EEE", conversationId);
    }

    private void updateConversion(String message, String type) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_MESSAGE_TYPE, type,
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, new Date()
        );
    }

    private void checkForConversion() {
        if (chatMessages.size() > 0) {
            checkForConversionRemotely(
                    receiverUser.id
            );
        }
    }

    private void setBtnVisible(boolean visible) {
        if (visible) {
            layoutImage.setVisibility(View.INVISIBLE);
            layoutAttact.setVisibility(View.INVISIBLE);
            layoutSend.setVisibility(View.VISIBLE);
            layoutSuggestions.setVisibility(View.GONE);
        } else {
            layoutImage.setVisibility(View.VISIBLE);
            layoutAttact.setVisibility(View.VISIBLE);
            layoutSend.setVisibility(View.INVISIBLE);
            layoutSuggestions.setVisibility(View.GONE);
        }
    }

    @NonNull
    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("dd MMMM, yyyy\nhh:mm a", Locale.getDefault()).format(date);
    }

    private void checkForConversionRemotely(String receiverId) {
        Log.d("EEE", receiverId);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);

    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversationId = documentSnapshot.getId();

        }
    };

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return null;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
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
        ConstraintLayout layoutMessage = dialog.findViewById(R.id.layout_message);
        ImageView imageCheck = dialog.findViewById(R.id.imageCheck);
        ImageView imageTranslate = dialog.findViewById(R.id.imageTranslate);
        ImageView imageMessage = dialog.findViewById(R.id.imageMessage);

        if (chatMessage.type.equals("text")) {
            textMessage.setVisibility(View.VISIBLE);
            textMessage.setText(lastMessages.get(position).message);
        } else if (chatMessage.type.equals(Constants.MESSAGE_IMAGE)) {
            layoutMessage.setBackground(null);
            textMessage.setVisibility(View.GONE);
            imageMessage.setVisibility(View.VISIBLE);
            imageMessage.setImageBitmap(getBitmapFromEncodedString(lastMessages.get(position).message));
        } else {
            StorageReference storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(lastMessages.get(position).message);
            String fileName = storageReference.getName().toString();
            String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
            String name = fileName.split("--__")[0];
            textMessage.setText(name + "." + extension);
            textMessage.setVisibility(ViewGroup.VISIBLE);
        }

        textDateTime.setText(lastMessages.get(position).dateTime);
        if (lastMessages.get(position).isSeen) {
            textSeenMessage.setText(getString(R.string.Seen));
            imageCheck.setVisibility(View.VISIBLE);
        } else {
            textSeenMessage.setText(getString(R.string.Delivered));
            imageCheck.setVisibility(View.GONE);
        }

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
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("message", textMessage.getText().toString());
            clipboard.setPrimaryClip(clip);
            showToast(getString(R.string.Copied_message));
            dialog.dismiss();
        });

        layoutDetail.setOnClickListener(view -> {
            if (chatMessage.type.equals("text")) {
                final Dialog detailDialog = openDialog(R.layout.layout_detail_message_dialog);
                assert detailDialog != null;

                ImageView imageCheckDetail = detailDialog.findViewById(R.id.imageCheck);
                TextView txtTitle = detailDialog.findViewById(R.id.dialog_title);
                TextView textMessageDetail = detailDialog.findViewById(R.id.textMessage);
                TextView textSeenMessageDetail = detailDialog.findViewById(R.id.textSeenMessage);
                TextView textSeenTime = detailDialog.findViewById(R.id.textSeenTime);
                ConstraintLayout layoutMessageDetail = detailDialog.findViewById(R.id.layout_message);

                if (chatMessage.type.equals("text"))
                    layoutMessageDetail.setVisibility(View.VISIBLE);
                else layoutMessageDetail.setVisibility(View.GONE);

                txtTitle.setText(receiverUser.name);
                textMessageDetail.setText(chatMessage.message);
                if (lastMessages.get(position).model.equals("sender")) {
                    textSeenMessageDetail.setVisibility(View.VISIBLE);
                    if (lastMessages.get(position).isSeen) {
                        textSeenMessageDetail.setText(getString(R.string.Seen));
                        imageCheckDetail.setVisibility(View.VISIBLE);
                    } else {
                        textSeenMessageDetail.setText(getString(R.string.Delivered));
                        imageCheckDetail.setVisibility(View.GONE);
                    }
                } else {
                    textSeenMessageDetail.setVisibility(View.GONE);
                    imageCheckDetail.setVisibility(View.GONE);
                }
                textSeenTime.setVisibility(View.VISIBLE);
                if (chatMessage.isSeen) {
                    database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS)
                            .document(chatMessage.id)
                            .collection(Constants.KEY_MEMBER_SEEN_MESSAGE)
                            .get()
                            .addOnCompleteListener(task -> {
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                    database.collection(Constants.KEY_COLLECTION_USER)
                                            .document(queryDocumentSnapshot.getId())
                                            .get()
                                            .addOnCompleteListener(task1 -> {
                                                DocumentSnapshot documentSnapshot = task1.getResult();
                                                textSeenTime.setText(documentSnapshot.getString(Constants.KEY_NAME) + " " + getString(R.string.at)
                                                        + chatMessage.seenTime + "\n"
                                                        + textSeenTime.getText().toString().trim());
                                            });
                                }

                            });
                } else {
                    textSeenTime.setText(getString(R.string.Everyone_not_seen));
                }

                detailDialog.show();
                dialog.dismiss();
            }
        });

        layoutDelete.setOnClickListener(view -> {
            chatMessages.remove(position);
            chatAdapter.notifyItemRemoved(position);
            chatAdapter.notifyItemChanged(position);
            chatAdapter.notifyDataSetChanged();
            chatAdapter.notifyItemRangeInserted(0, chatMessages.size());
            updateDataOnFB(chatMessage.id);
            if (lastMessages.size() >= 1) {
                updateConversionAfterDeleteMessage(lastMessages.get(lastMessages.size() - 1).message,
                        lastMessages.get(lastMessages.size() - 1).type,
                        lastMessages.get(lastMessages.size() - 1).dataObject);
            } else {
                lastMessages.size();
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId).delete();
            }
            dialog.dismiss();
        });

        dialog.show();

    }

    private void updateDataOnFB(String key) {
        database.collection(Constants.KEY_COLLECTION_CHAT_GROUPS)
                .document(key)
                .delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        showToast(getString(R.string.Delete_Message));
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        showToast(e.getMessage());
                    }
                });
    }

    private void updateConversionAfterDeleteMessage(String message, String type, Date time) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_MESSAGE_TYPE, type,
                Constants.KEY_LAST_MESSAGE, message,
                Constants.KEY_TIMESTAMP, time
        );
    }

    private Dialog openDialog(int layout) {
        final Dialog dialog = new Dialog(getContext());
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
}
