package com.example.heychat.fragments;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.R;
import com.example.heychat.activities.ChatActivity;
import com.example.heychat.activities.SearchActivity;
import com.example.heychat.activities.UsersActivity;
import com.example.heychat.adapters.ChatBottomSheetFragment;
import com.example.heychat.adapters.UsersAdapter;
import com.example.heychat.listeners.UserListener;
import com.example.heychat.models.ChatMessage;
import com.example.heychat.models.User;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConfigFragment extends Fragment implements UserListener{

    private PreferenceManager preferenceManager;
    private final ArrayList<String> contactList = new ArrayList<>();
    private UsersAdapter usersAdapter;
    private RecyclerView userRecyclerView;
    private ProgressBar progressBar;
    private TextView textErrorMessage;
    private FirebaseFirestore database;

    public ConfigFragment() {
        // Required empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_config, container, false);
        database = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getContext());
        userRecyclerView = view.findViewById(R.id.config_RecyclerView);
        progressBar = view.findViewById(R.id.config_progressBar);
        textErrorMessage = view.findViewById(R.id.textError);
        requestPermission();

        return view;
    }

    private void getUsers(){
        loading(true);
        getContactList();
        database.collection(Constants.KEY_COLLECTION_USER)
                .get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if(task.isSuccessful() && task.getResult() != null){
                        List<User> users = new ArrayList<>();
                        for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                            if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                continue;
                            }
                            for (int i = 0; i < contactList.size(); i++){
                                if (contactList.get(i).equals(queryDocumentSnapshot.getString(Constants.KEY_EMAIL))){
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
                        if (users.size() > 0){
                            usersAdapter = new UsersAdapter(users, this);
                            userRecyclerView.setAdapter(usersAdapter);
                            userRecyclerView.setVisibility(View.VISIBLE);
                        }
                    }
                });
        database.collection(Constants.KEY_COLLECTION_USER).addSnapshotListener(eventListener);
    }

    private void requestPermission(){
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                getUsers();
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(getContext(), getString(R.string.PermissionDenied) + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage(getString(R.string.PermissionDeniedNoice))
                .setPermissions(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                .check();
    }


    private void getContactList() {
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        try (Cursor cursor = getActivity().getContentResolver().query(uri, null, null, null, null)) {
            if (cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    @SuppressLint("Range") String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                    Uri uriPhone = ContactsContract.CommonDataKinds.Phone.CONTENT_URI;
                    String selection = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " =?";
                    try (Cursor phoneCursor = getActivity().getContentResolver().query(uriPhone, null, selection, new String[]{id}, null)) {

                        if (phoneCursor.moveToNext()) {
                            @SuppressLint("Range") String number = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            number = number.replaceAll("\\s+","");
                            contactList.add(number.trim());
                        }
                    }
                }
            }
        }

    }



    private void showErrorMessage(){
        textErrorMessage.setText(String.format("%s", getString(R.string.No_user)));
        textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading){
        if(isLoading){
            progressBar.setVisibility(View.VISIBLE);
        } else{
            progressBar.setVisibility(View.INVISIBLE);
        }
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if(error != null){
            return;
        }
        if (value != null){

            database.collection(Constants.KEY_COLLECTION_USER)
                    .get()
                    .addOnCompleteListener(task -> {
                        loading(false);
                        String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                        if(task.isSuccessful() && task.getResult() != null){
                            List<User> users = new ArrayList<>();
                            for(QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()){
                                if(currentUserId.equals(queryDocumentSnapshot.getId())){
                                    continue;
                                }
                                for (int i = 0; i < contactList.size(); i++){
                                    if (contactList.get(i).equals(queryDocumentSnapshot.getString(Constants.KEY_EMAIL))){
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
                            if (users.size() > 0){
                                usersAdapter = new UsersAdapter(users, this);
                                userRecyclerView.setAdapter(usersAdapter);
                                userRecyclerView.setVisibility(View.VISIBLE);
                            }
                        }
                    });
        }
    };

    @Override
    public void onUserClicker(User user) {
//        Intent intent = new Intent(getContext(), ChatActivity.class);
//        intent.putExtra(Constants.KEY_USER, user);
//        startActivity(intent);

        ChatBottomSheetFragment bottomSheetDialog = ChatBottomSheetFragment.newInstance(user);
        bottomSheetDialog.show(getActivity().getSupportFragmentManager(), bottomSheetDialog.getTag());
    }
}