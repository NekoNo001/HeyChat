package com.example.heychat.fragments;

import static android.app.Activity.RESULT_OK;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;

import com.example.heychat.R;
import com.example.heychat.activities.MainActivity2;
import com.example.heychat.activities.PrivateChatActivity;
import com.example.heychat.activities.SignInActivity;
import com.example.heychat.activities.SlashMainScreen;
import com.example.heychat.adapters.LanguageManager;
import com.example.heychat.models.FontSize;
import com.example.heychat.models.RoomChat;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.jakewharton.processphoenix.ProcessPhoenix;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileFragment extends Fragment {
    private CircleImageView profile_image;
    private Bitmap image;
    private TextView user_name, phone_number;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    private FirebaseFirestore database;
    private String userId;
    private int textSize = 0;
    private RoomChat roomChat;

    public ProfileFragment() {

    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        database = FirebaseFirestore.getInstance();
        profile_image = view.findViewById(R.id.profile_image);
        user_name = view.findViewById(R.id.user_name);
        phone_number = view.findViewById(R.id.phone_number);
        if (getContext() != null) {
            preferenceManager = new PreferenceManager(getContext());
        }
        preferenceManager.remove(Constants.KEY_COLLECTION_ROOM);
        HashMap<String, Object> room = new HashMap<>();
        loadUserDetail();

        View log_out_btn = view.findViewById(R.id.log_out_btn);
        View change_language_btn = view.findViewById(R.id.change_language_btn);
        View change_text_size = view.findViewById(R.id.change_text_size);
        View edit_profile = view.findViewById(R.id.edit_profile_btn);
        View block_screen_shot = view.findViewById(R.id.BlockShotID);
        LinearLayout layoutPrivateAccount = view.findViewById(R.id.layoutPrivateAccount);
        edit_profile.setOnClickListener(v -> editProfile());
        log_out_btn.setOnClickListener(v -> logout());
        change_language_btn.setOnClickListener(v -> changeLanguage());
        change_text_size.setOnClickListener(v -> changeTextSize());

        layoutPrivateAccount.setOnClickListener(view1 -> {
            database.collection(Constants.KEY_COLLECTION_USER)
                    .document(preferenceManager.getString(Constants.KEY_USER_ID))
                    .get()
                    .addOnCompleteListener(task -> {
                        DocumentSnapshot documentSnapshot = task.getResult();
                        if (documentSnapshot.getString(Constants.KEY_PRIVATE_ACCOUNT_NAME) == null) {
                            privateChat();
                        } else {
                            createRoomChat();
                        }
                    });

        });

        block_screen_shot.setOnClickListener(view12 -> {
            final Dialog dialog = openDialog(R.layout.layout_dialog_block_screenshot);
            Switch switch_block = dialog.findViewById(R.id.switch_block);
            Button yes_btn = dialog.findViewById(R.id.yes_btn);
            Button no_btn = dialog.findViewById(R.id.no_btn);

            if (preferenceManager.getString(Constants.KEY_BLOCK_SCREENSHOT).equals("unblock")) {
                switch_block.setChecked(false);
                switch_block.setText(R.string.Unblock);
            } else if (preferenceManager.getString(Constants.KEY_BLOCK_SCREENSHOT).equals("block")) {
                switch_block.setChecked(true);
                switch_block.setText(R.string.Block);
            }

            switch_block.setOnClickListener(view15 -> {
                if (switch_block.isChecked()) {
                    switch_block.setText(R.string.Block);
                } else {
                    switch_block.setText(R.string.Unblock);
                }
            });

            yes_btn.setOnClickListener(view14 -> {
                if (switch_block.isChecked()) {
                    showToast(getString(R.string.Disable_Shot));
                    preferenceManager.putString(Constants.KEY_BLOCK_SCREENSHOT, "block");
                    HashMap<String, Object> blockShot = new HashMap<>();
                    blockShot.put(Constants.KEY_BLOCK_SCREENSHOT, true);
                    database.collection(Constants.KEY_COLLECTION_USER)
                            .document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .update(blockShot);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setCancelable(false);
                    builder.setMessage(R.string.dialog_message)
                            .setTitle(R.string.dialog_title);
                    builder.setNegativeButton(R.string.Yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent nextIntent = new Intent(ProfileFragment.this.getActivity(),SignInActivity.class);
                            ProcessPhoenix.triggerRebirth(context, nextIntent);
                        }
                    });
                    AlertDialog noice = builder.create();
                    noice.show();
                } else {
                    showToast(getString(R.string.Enable_shot));
                    preferenceManager.putString(Constants.KEY_BLOCK_SCREENSHOT, "unblock");
                    HashMap<String, Object> blockShot = new HashMap<>();
                    blockShot.put(Constants.KEY_BLOCK_SCREENSHOT, false);
                    database.collection(Constants.KEY_COLLECTION_USER)
                            .document(preferenceManager.getString(Constants.KEY_USER_ID))
                            .update(blockShot);
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setCancelable(false);
                    builder.setMessage(R.string.dialog_message)
                            .setTitle(R.string.dialog_title);
                    builder.setNegativeButton(R.string.Yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent nextIntent = new Intent(ProfileFragment.this.getActivity(),SignInActivity.class);
                            ProcessPhoenix.triggerRebirth(context, nextIntent);
                        }
                    });
                    AlertDialog noice = builder.create();
                    noice.show();
                }
                dialog.dismiss();
            });

            no_btn.setOnClickListener(view13 -> dialog.dismiss());
            dialog.show();


        });

        return view;
    }

    CircleImageView change_image;

    private void privateChat() {
        final Dialog dialog = openDialog(R.layout.layout_dialog_edit_profile);
        EditText edit_name = dialog.findViewById(R.id.name_edit_text);
        Button yes_btn = dialog.findViewById(R.id.yes_btn);
        Button no_btn = dialog.findViewById(R.id.no_btn);

        yes_btn.setOnClickListener(view -> {
            if (edit_name.getText().toString().trim().isEmpty()) {
                showToast(getString(R.string.Enter_name));
            } else {
                HashMap<String, Object> account = new HashMap<>();
                account.put(Constants.KEY_PRIVATE_ACCOUNT_NAME, edit_name.getText().toString());
                database.collection(Constants.KEY_COLLECTION_USER)
                        .document(preferenceManager.getString(Constants.KEY_USER_ID))
                        .update(account)
                        .addOnSuccessListener(unused -> {
                            createRoomChat();
                        });

                dialog.dismiss();
            }
        });
        no_btn.setOnClickListener(view -> dialog.dismiss());

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
                            Intent intent = new Intent(getContext(), PrivateChatActivity.class);
                            intent.putExtra(Constants.KEY_COLLECTION_ROOM, roomChat);
                            startActivity(intent);

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
                                    Intent intent = new Intent(getContext(), PrivateChatActivity.class);
                                    intent.putExtra(Constants.KEY_COLLECTION_ROOM, roomChat);
                                    startActivity(intent);
                                });
                    }
                });
    }

    private void editProfile() {
        final Dialog dialog = openDialog(R.layout.layout_dialog_edit_profile);
        change_image = dialog.findViewById(R.id.image_edit_profile);
        TextView change_image_tv = dialog.findViewById(R.id.change_image_text_view);
        EditText edit_name = dialog.findViewById(R.id.name_edit_text);
        Button yes_btn = dialog.findViewById(R.id.yes_btn);
        Button no_btn = dialog.findViewById(R.id.no_btn);

        edit_name.setText(user_name.getText());
        change_image.setImageBitmap(image);

        change_image.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pickImage.launch(intent);
        });

        change_image_tv.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pickImage.launch(intent);
        });

        yes_btn.setOnClickListener(view -> {
            if (edit_name.getText().toString().trim().isEmpty()) {
                showToast(getString(R.string.Enter_name));
            } else {
                updateProfile(edit_name.getText().toString().trim());
                dialog.dismiss();
            }
        });
        no_btn.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private void updateProfile(String name) {
        database.collection(Constants.KEY_COLLECTION_USER).document(userId).update("image", encodedImage);
        database.collection(Constants.KEY_COLLECTION_USER).document(userId).update("name", name);
        preferenceManager.putString(Constants.KEY_NAME, name);
        preferenceManager.putString(Constants.KEY_IMAGE, encodedImage);
        loadUserDetail();
        showToast(getString(R.string.Update_successful));
    }

    private final ActivityResultLauncher pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getActivity().getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            change_image.setImageBitmap(bitmap);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }


    private void changeTextSize() {
        final Dialog dialog = openDialog(R.layout.layout_dialog_textsize);
        SeekBar size = dialog.findViewById(R.id.seekBar);
        TextView text = dialog.findViewById(R.id.TextSize1);
        TextView text2 = dialog.findViewById(R.id.TextSize2);
        TextView text3 = dialog.findViewById(R.id.textRecentMessage);
        Button yes_btn = dialog.findViewById(R.id.yes_btn);
        Button no_btn = dialog.findViewById(R.id.no_btn);

        if (Objects.equals(preferenceManager.getString(Constants.KEY_TEXTSIZE), "14")) {
            size.setProgress(0);
        } else if (Objects.equals(preferenceManager.getString(Constants.KEY_TEXTSIZE), "18")) {
            size.setProgress(1);
        } else if (Objects.equals(preferenceManager.getString(Constants.KEY_TEXTSIZE), "24")) {
            size.setProgress(2);
        } else {
            size.setProgress(3);
        }

        size.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                textSize = getSize(i);
                text.setTextSize(textSize);
                text2.setTextSize(textSize);
                text3.setTextSize(textSize);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        yes_btn.setOnClickListener(view -> {
            FontSize.fontSize = textSize;
            preferenceManager.putString(Constants.KEY_TEXTSIZE, String.valueOf(textSize));
            showToast(getString(R.string.FontSize));
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setCancelable(false);
            builder.setMessage(R.string.dialog_message)
                    .setTitle(R.string.dialog_title);
            builder.setNegativeButton(R.string.Yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    Intent nextIntent = new Intent(ProfileFragment.this.getActivity(),SignInActivity.class);
                    ProcessPhoenix.triggerRebirth(context, nextIntent);
                }
            });
            AlertDialog noice = builder.create();
            noice.show();
            dialog.dismiss();
        });
        no_btn.setOnClickListener(view -> dialog.dismiss());

        dialog.show();

    }

    public int getSize(int i) {
        int s = 0;
        if (i == 0) {
            s = 14;
        } else if (i == 1) {
            s = 18;
        } else if (i == 2) {
            s = 24;
        } else if (i == 3) {
            s = 30;
        }
        return s;
    }

    private void changeLanguage() {
        final Dialog dialog = openDialog(R.layout.layout_dialog_language);
        Switch vietnamese = dialog.findViewById(R.id.switch_vietnamese);
        Switch english = dialog.findViewById(R.id.switch_english);
        Button yes_btn = dialog.findViewById(R.id.yes_btn);
        Button no_btn = dialog.findViewById(R.id.no_btn);

        if (preferenceManager.getString(Constants.KEY_LANGUAGE) == null) {
            preferenceManager.putString(Constants.KEY_LANGUAGE, "EN");
        }

        if (Objects.equals(preferenceManager.getString(Constants.KEY_LANGUAGE), "VI")) {
            vietnamese.setChecked(true);
            english.setChecked(false);
        } else {
            vietnamese.setChecked(false);
            english.setChecked(true);
        }

        vietnamese.setOnCheckedChangeListener((compoundButton, b) -> {
            english.setChecked(!b);
        });

        english.setOnCheckedChangeListener((compoundButton, b) -> {
            vietnamese.setChecked(!b);
        });

        yes_btn.setOnClickListener(view -> {
            if (vietnamese.isChecked()) {
                showToast(getString(R.string.VietNamese));
                preferenceManager.putString(Constants.KEY_LANGUAGE, "VI");
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setCancelable(false);
                builder.setMessage(R.string.dialog_message)
                        .setTitle(R.string.dialog_title);
                builder.setNegativeButton(R.string.Yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent nextIntent = new Intent(ProfileFragment.this.getActivity(),SignInActivity.class);
                        ProcessPhoenix.triggerRebirth(context, nextIntent);
                    }
                });
                AlertDialog noice = builder.create();
                noice.show();
            } else {
                showToast(getString(R.string.English));
                preferenceManager.putString(Constants.KEY_LANGUAGE, "EN");
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setCancelable(false);
                builder.setMessage(R.string.dialog_message)
                        .setTitle(R.string.dialog_title);
                builder.setNegativeButton(R.string.Yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent nextIntent = new Intent(ProfileFragment.this.getActivity(),SignInActivity.class);
                        ProcessPhoenix.triggerRebirth(context, nextIntent);
                    }
                });
                AlertDialog noice = builder.create();
                noice.show();
            }
            dialog.dismiss();

        });
        no_btn.setOnClickListener(view -> dialog.dismiss());


        dialog.show();

    }

    private void logout() {
        final Dialog dialog = openDialog(R.layout.layout_dialog_logout);
        Button yes_btn = dialog.findViewById(R.id.yes_btn);
        Button no_btn = dialog.findViewById(R.id.no_btn);

        yes_btn.setOnClickListener(v -> signOut());
        no_btn.setOnClickListener(view -> dialog.dismiss());
        dialog.show();
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


    private void signOut() {
        showToast(getString(R.string.Signing_out));

        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USER).document(
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        HashMap<String, Object> updates = new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getContext(), SignInActivity.class));
                    getActivity().finish();
                })
                .addOnFailureListener(e -> showToast(getString(R.string.Unable_signout)));
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loadUserDetail() {
        userId = preferenceManager.getString(Constants.KEY_USER_ID);
        user_name.setText(preferenceManager.getString(Constants.KEY_NAME));
//        Log.d("EEE", preferenceManager.getString(Constants.KEY_EMAIL));
        phone_number.setText(preferenceManager.getString(Constants.KEY_EMAIL));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        image = bitmap;
        profile_image.setImageBitmap(bitmap);
    }
}