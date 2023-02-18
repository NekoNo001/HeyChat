package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.heychat.R;
import com.example.heychat.adapters.UserSelectionAdapter;
import com.example.heychat.listeners.UserSelectionListener;
import com.example.heychat.models.User;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity implements UserSelectionListener {
    private PreferenceManager preferenceManager;
    private final ArrayList<String> contactList = new ArrayList<>();
    private UserSelectionAdapter usersAdapter;
    private RecyclerView userRecyclerView;
    private ProgressBar progressBar;
    private TextView textErrorMessage;
    private Button btnSelection;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        preferenceManager = new PreferenceManager(getApplicationContext());
        userRecyclerView = findViewById(R.id.create_group_RecyclerView);
        progressBar = findViewById(R.id.create_group_progressBar);
        textErrorMessage = findViewById(R.id.textError);
        btnSelection = findViewById(R.id.btnSelection);

        requestPermission();

        btnSelection.setOnClickListener(view -> {
            List<User> selectedUser = usersAdapter.getSelectedUser();
            StringBuilder userPhones = new StringBuilder();
            for (int i = 0; i < selectedUser.size(); i++) {
                userPhones.append(selectedUser.get(i).id).append(",");
            }
            userPhones.append(preferenceManager.getString(Constants.KEY_USER_ID));
            Intent intent = new Intent(getApplicationContext(), SetInfoGroupActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra(Constants.KEY_GROUP_MEMBER, userPhones.toString());
            startActivity(intent);
        });

    }

    private void requestPermission() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                getUsers();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(getApplicationContext(), "Permission Denied\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage("If you reject permission,you can not use this service\n\nPlease turn on permissions at [Setting] > [Permission]")
                .setPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                .check();
    }

    private void getUsers() {
        loading(true);
        getContactList();
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<User> users = new ArrayList<>();
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                            if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                continue;
                            }
                            for (int i = 0; i < contactList.size(); i++) {
                                System.out.println(contactList.get(i));
                                if (contactList.get(i).equals(queryDocumentSnapshot.getString(Constants.KEY_EMAIL))) {
                                    User user = new User();
                                    user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                    user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                    user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                    user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                    user.id = queryDocumentSnapshot.getId();
                                    users.add(user);
                                }
                            }
                        }
                        if (users.size() > 0) {
                            usersAdapter = new UserSelectionAdapter(users, new UserSelectionListener() {
                                @Override
                                public void onUserSelection(Boolean isSelected) {
                                    if (isSelected) {
                                        btnSelection.setVisibility(View.VISIBLE);
                                    } else {
                                        btnSelection.setVisibility(View.GONE);
                                    }
                                }

                            });
                            userRecyclerView.setAdapter(usersAdapter);
                            userRecyclerView.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });

    }

    private void getContactList() {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    Uri uriPhone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                    String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " =?";
                    try (Cursor phoneCursor = getContentResolver().query(uriPhone, null, selection, new String[]{id}, null)) {

                        if (phoneCursor.moveToNext()) {
                            @SuppressLint("Range") String number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            number = number.replaceAll("\\s+", "");
                            contactList.add(number.trim());
                        }
                    }
                }
            }
        }

    }


    private void showErrorMessage() {
        textErrorMessage.setText(String.format("%s",R.string.No_user));
        textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onUserSelection(Boolean isSelected) {
        if (isSelected) {
            btnSelection.setVisibility(View.VISIBLE);
        } else {
            btnSelection.setVisibility(View.GONE);
        }
    }

}