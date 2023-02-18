package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.heychat.R;
import com.example.heychat.adapters.AddGroupSelectionAdapter;
import com.example.heychat.adapters.ChangeTeamLeaderAdapter;
import com.example.heychat.adapters.ChatBottomSheetFragment;
import com.example.heychat.adapters.DeleteGroupSelectionAdapter;
import com.example.heychat.adapters.UsersAdapter;
import com.example.heychat.listeners.GroupMemberListener;
import com.example.heychat.listeners.UserListener;
import com.example.heychat.models.Group;
import com.example.heychat.models.User;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;

public class InfoGroupActivity extends AppCompatActivity implements UserListener, GroupMemberListener {

    private LinearLayout layoutSeeMember, layoutAddMember, layoutDeleteMember, layoutChangeTeamLeader, layoutExitGroup, layoutDisbandingGroup;
    private AppCompatImageView imageBack;
    private TextView textName;
    private CircleImageView imageProfile;
    private Group group;
    private FirebaseFirestore database;
    private PreferenceManager preferenceManager;
    private UsersAdapter usersAdapter;
    private DeleteGroupSelectionAdapter deleteGroupSelectionAdapter;
    private AddGroupSelectionAdapter addGroupSelectionAdapter;
    private ChangeTeamLeaderAdapter changeTeamLeaderAdapter;
    private RecyclerView userRecyclerView;
    private List<User> users;
    private List<String> groupMember;
    private final ArrayList<String> contactList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_group);
        init();
        setListener();

    }

    public void init() {

        database = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(getApplicationContext());

        layoutSeeMember = findViewById(R.id.layoutSeeMember);
        layoutAddMember = findViewById(R.id.layoutAddMember);
        layoutDeleteMember = findViewById(R.id.layoutDeleteMember);
        layoutChangeTeamLeader = findViewById(R.id.layoutChangeTeamLeader);
        layoutExitGroup = findViewById(R.id.layoutExitGroup);
        layoutDisbandingGroup = findViewById(R.id.layoutDisbandingGroup);
        imageBack = findViewById(R.id.imageBack);
        textName = findViewById(R.id.textName);
        imageProfile = findViewById(R.id.imageProfile);

        textName.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE)) + 2);

        group = (Group) getIntent().getSerializableExtra(Constants.KEY_GROUP);

        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .get()
                .addOnCompleteListener(task -> {
                    DocumentSnapshot documentSnapshot = task.getResult();
                    if (Objects.equals(documentSnapshot.getString(Constants.KEY_GROUP_OWNER), preferenceManager.getString(Constants.KEY_USER_ID))) {
                        layoutChangeTeamLeader.setVisibility(View.VISIBLE);
                        layoutDisbandingGroup.setVisibility(View.VISIBLE);
                        layoutDeleteMember.setVisibility(View.VISIBLE);
                    }
                });

        groupMember = new ArrayList<>();
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .collection(Constants.KEY_GROUP_MEMBER)
                .get()
                .addOnCompleteListener(task -> {
                    for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                        groupMember.add(queryDocumentSnapshot.getId());
                    }
                });

        textName.setText(group.name);

    }

    @SuppressLint("NotifyDataSetChanged")
    private void setListener() {

        imageBack.setOnClickListener(view -> {
            finish();
        });

        layoutDisbandingGroup.setOnClickListener(view -> disbandingGroup());

        layoutExitGroup.setOnClickListener(view -> exitGroup());

        layoutSeeMember.setOnClickListener(view -> {

            final Dialog dialog = openDialog(R.layout.layout_dialog_group_member);
            userRecyclerView = dialog.findViewById(R.id.userRecyclerView);
            Button no_btn = dialog.findViewById(R.id.no_btn);

            users = new ArrayList<>();
            usersAdapter = new UsersAdapter(users, this);
            userRecyclerView.setAdapter(usersAdapter);
            userRecyclerView.setVisibility(View.VISIBLE);
            getUsers("list");
            userRecyclerView.setHasFixedSize(true);
            no_btn.setOnClickListener(view1 -> dialog.dismiss());
            dialog.show();

        });

        layoutDeleteMember.setOnClickListener(view -> {
            final Dialog dialog = openDialog(R.layout.layout_dialog_delete_group_member);
            userRecyclerView = dialog.findViewById(R.id.userRecyclerView);
            Button no_btn = dialog.findViewById(R.id.no_btn);
            Button btnDelete = dialog.findViewById(R.id.btnDelete);

            users = new ArrayList<>();
            deleteGroupSelectionAdapter = new DeleteGroupSelectionAdapter(users, this);
            userRecyclerView.setAdapter(deleteGroupSelectionAdapter);
            userRecyclerView.setVisibility(View.VISIBLE);
            getUsers("delete");
            userRecyclerView.setHasFixedSize(true);
            no_btn.setOnClickListener(view1 -> dialog.dismiss());

            btnDelete.setOnClickListener(view12 -> {
                List<User> selectedUser = deleteGroupSelectionAdapter.getSelectedUser();
                for (int i = 0; i < selectedUser.size(); i++) {
                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .collection(Constants.KEY_GROUP_MEMBER)
                            .document(selectedUser.get(i).id)
                            .delete();
                    database.collection(Constants.KEY_COLLECTION_USER)
                            .document(selectedUser.get(i).id)
                            .collection(Constants.KEY_GROUP_ID)
                            .document(group.id)
                            .delete();
                    groupMember.remove(selectedUser.get(i).id);
                }
                dialog.dismiss();
                showToast(getString(R.string.Delete_Member));
            });

            dialog.show();
        });


        layoutAddMember.setOnClickListener(view -> {
            final Dialog dialog = openDialog(R.layout.layout_dialog_add_group_member);
            assert dialog != null;
            userRecyclerView = dialog.findViewById(R.id.userRecyclerView);
            Button no_btn = dialog.findViewById(R.id.no_btn);
            Button btnAdd = dialog.findViewById(R.id.btnAdd);

            users = new ArrayList<>();
            users.clear();
            addGroupSelectionAdapter = new AddGroupSelectionAdapter(users, this);
            userRecyclerView.setAdapter(addGroupSelectionAdapter);
            userRecyclerView.setVisibility(View.VISIBLE);

            getContactList();
            getUsers("add");

            btnAdd.setOnClickListener(view13 -> {
                List<User> selectedUser = addGroupSelectionAdapter.getSelectedUser();
                for (int i = 0; i < selectedUser.size(); i++) {
                    HashMap<String, Object> id = new HashMap<>();
                    id.put("Owner", true);
                    String userID = selectedUser.get(i).id;
                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .collection(Constants.KEY_GROUP_MEMBER)
                            .get()
                            .addOnCompleteListener(task -> {
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                    if (!userID.equals(queryDocumentSnapshot.getId())) {
                                        database.collection(Constants.KEY_COLLECTION_GROUP)
                                                .document(group.id)
                                                .collection(Constants.KEY_GROUP_MEMBER)
                                                .document(userID)
                                                .set(id)
                                                .addOnSuccessListener(unused -> {
                                                    database.collection(Constants.KEY_COLLECTION_USER)
                                                            .document(userID)
                                                            .collection(Constants.KEY_GROUP_ID)
                                                            .document(group.id)
                                                            .set(id);
                                                    groupMember.add(userID);
                                                    dialog.dismiss();

                                                });
                                    }
                                }
                            });

                }
                showToast(getString(R.string.Add_Member));
            });
            no_btn.setOnClickListener(view1 -> dialog.dismiss());
            dialog.show();
            users.clear();
            addGroupSelectionAdapter.notifyDataSetChanged();
        });

        layoutChangeTeamLeader.setOnClickListener(view -> {

            final Dialog changeTeamLeaderDialog = openDialog(R.layout.layout_dialog_change_team_leader);
            users = new ArrayList<>();
            changeTeamLeaderAdapter = new ChangeTeamLeaderAdapter(users, this);
            assert changeTeamLeaderDialog != null;
            userRecyclerView = changeTeamLeaderDialog.findViewById(R.id.userRecyclerView);
            Button no_btn = changeTeamLeaderDialog.findViewById(R.id.no_btn);

            userRecyclerView.setAdapter(changeTeamLeaderAdapter);
            userRecyclerView.setVisibility(View.VISIBLE);

            getUsers("change");

            no_btn.setOnClickListener(view1 -> changeTeamLeaderDialog.dismiss());
            changeTeamLeaderDialog.show();

        });

    }

    @SuppressLint("NotifyDataSetChanged")
    private void getUsers(String type) {
        if (type.equals("add")) {
            database.collection(Constants.KEY_COLLECTION_GROUP).document(group.id).collection(Constants.KEY_GROUP_MEMBER)
                    .get().addOnSuccessListener(queryDocumentSnapshots -> {
                        ArrayList<String> member = new ArrayList<>();
                        for (QueryDocumentSnapshot qr: queryDocumentSnapshots){
                            member.add(qr.getId());
                        }
                        database.collection(Constants.KEY_COLLECTION_USER)
                                .get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && task.getResult() != null) {
                                        users.clear();
                                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                            for (int i = 0; i < contactList.size(); i++) {
                                                if (contactList.get(i).equals(queryDocumentSnapshot.getString(Constants.KEY_EMAIL)) && !member.contains(queryDocumentSnapshot.getId())) {
                                                    User user = new User();
                                                    user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                                    user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                                    user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                                    user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                                    user.id = queryDocumentSnapshot.getId();
                                                    users.add(user);
                                                }
                                                addGroupSelectionAdapter.notifyDataSetChanged();
                                            }
                                        }

                                    }
                                });
                    });

        } else {
            for (int i = 0; i < groupMember.size(); i++) {
//                Log.d("CCC", groupMember.get(i));
                users.clear();
                database.collection(Constants.KEY_COLLECTION_USER)
                        .document(groupMember.get(i))
                        .get()
                        .addOnCompleteListener(task1 -> {
                            DocumentSnapshot documentSnapshot = task1.getResult();
                            if (type.equals("list")) {
                                User user = new User();
                                user.name = documentSnapshot.getString(Constants.KEY_NAME);
                                user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                                user.image = documentSnapshot.getString(Constants.KEY_IMAGE);
                                user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                user.id = documentSnapshot.getId();
                                users.add(user);
                            } else if (type.equals("delete") || type.equals("change")) {
                                if (!documentSnapshot.getId().equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                                    User user = new User();
                                    user.name = documentSnapshot.getString(Constants.KEY_NAME);
                                    user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                                    user.image = documentSnapshot.getString(Constants.KEY_IMAGE);
                                    user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                    user.id = documentSnapshot.getId();
                                    users.add(user);
                                }
                            }

                            switch (type) {
                                case "list":
                                    usersAdapter.notifyDataSetChanged();
                                    break;
                                case "delete":
                                    deleteGroupSelectionAdapter.notifyDataSetChanged();
                                    break;
                                case "change":
                                    changeTeamLeaderAdapter.notifyDataSetChanged();
                                    break;
                            }

                        });
            }
        }

    }

    private void changeTeamLeader(User user) {
        HashMap<String, Object> owner = new HashMap<>();
        owner.put(Constants.KEY_GROUP_OWNER, user.id);
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .update(owner)
                .addOnSuccessListener(unused -> {
                    showToast(user.name + R.string.isleader);
                    Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
    }

    private void disbandingGroup() {
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .delete()
                .addOnSuccessListener(unused -> {

                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .collection(Constants.KEY_GROUP_MEMBER)
                            .get()
                            .addOnCompleteListener(task -> {
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                    database.collection(Constants.KEY_COLLECTION_USER)
                                            .document(queryDocumentSnapshot.getId())
                                            .collection(Constants.KEY_GROUP_ID)
                                            .document(group.id)
                                            .delete();
                                }
                            });

                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .collection(Constants.KEY_GROUP_MEMBER)
                            .get()
                            .addOnCompleteListener(task -> {
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                    database.collection(Constants.KEY_COLLECTION_GROUP)
                                            .document(group.id)
                                            .collection(Constants.KEY_GROUP_MEMBER)
                                            .document(queryDocumentSnapshot.getId())
                                            .delete();
                                }
                            });

                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                            .whereEqualTo(Constants.KEY_RECEIVER_ID, group.id)
                            .get().addOnCompleteListener(task -> {
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult())
                                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                            .document(queryDocumentSnapshot.getId())
                                            .delete();
                            });

                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .whereEqualTo(Constants.KEY_RECEIVER_ID, group.id)
                            .get().addOnCompleteListener(task -> {
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                            .document(queryDocumentSnapshot.getId())
                                            .delete();
                                }
                            });

                    showToast(getString(R.string.DeleteGroup));
                    Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                });
    }

    private void exitGroup() {
        database.collection(Constants.KEY_COLLECTION_GROUP)
                .document(group.id)
                .collection(Constants.KEY_GROUP_MEMBER)
                .document(preferenceManager.getString(Constants.KEY_USER_ID))
                .delete()
                .addOnCompleteListener(task -> {
                    database.collection(Constants.KEY_COLLECTION_GROUP)
                            .document(group.id)
                            .collection(Constants.KEY_GROUP_MEMBER)
                            .get()
                            .addOnCompleteListener(task12 -> {
                                int count = 0;
                                for (QueryDocumentSnapshot ignored : task12.getResult()) {
                                    count++;
                                }
                                if (count < 2) {
                                    database.collection(Constants.KEY_COLLECTION_GROUP)
                                            .document(group.id)
                                            .delete();

                                    database.collection(Constants.KEY_COLLECTION_USER)
                                            .document(preferenceManager.getString(Constants.KEY_USER_ID))
                                            .collection(Constants.KEY_GROUP_ID)
                                            .document(group.id)
                                            .delete();

                                    database.collection(Constants.KEY_COLLECTION_GROUP)
                                            .document(group.id)
                                            .collection(Constants.KEY_GROUP_MEMBER)
                                            .get()
                                            .addOnCompleteListener(task121 -> {
                                                for (QueryDocumentSnapshot queryDocumentSnapshot : task121.getResult()) {
                                                    database.collection(Constants.KEY_COLLECTION_USER)
                                                            .document(queryDocumentSnapshot.getId())
                                                            .collection(Constants.KEY_GROUP_ID)
                                                            .document(group.id)
                                                            .delete();
                                                }
                                            });

                                    database.collection(Constants.KEY_COLLECTION_GROUP)
                                            .document(group.id)
                                            .collection(Constants.KEY_GROUP_MEMBER)
                                            .get()
                                            .addOnCompleteListener(task1212 -> {
                                                for (QueryDocumentSnapshot queryDocumentSnapshot : task1212.getResult()) {
                                                    database.collection(Constants.KEY_COLLECTION_GROUP)
                                                            .document(group.id)
                                                            .collection(Constants.KEY_GROUP_MEMBER)
                                                            .document(queryDocumentSnapshot.getId())
                                                            .delete();
                                                }
                                            });

                                    showToast(getString(R.string.ExitGroupSuccessfully));
                                    Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    database.collection(Constants.KEY_COLLECTION_USER)
                                            .document(preferenceManager.getString(Constants.KEY_USER_ID))
                                            .collection(Constants.KEY_GROUP_ID)
                                            .document(group.id)
                                            .delete();

                                    database.collection(Constants.KEY_COLLECTION_GROUP)
                                            .document(group.id)
                                            .get()
                                            .addOnCompleteListener(task1 -> {
                                                DocumentSnapshot documentSnapshot = task1.getResult();
                                                if (Objects.requireNonNull(documentSnapshot.getString(Constants.KEY_GROUP_OWNER)).equals(preferenceManager.getString(Constants.KEY_USER_ID))) {
                                                    database.collection(Constants.KEY_COLLECTION_GROUP)
                                                            .document(group.id)
                                                            .collection(Constants.KEY_GROUP_MEMBER)
                                                            .get()
                                                            .addOnCompleteListener(task11 -> {
                                                                QuerySnapshot querySnapshot = task11.getResult();
                                                                HashMap<String, Object> user = new HashMap<>();
                                                                user.put(Constants.KEY_GROUP_OWNER, querySnapshot.getDocuments().get(0).getId());
                                                                database.collection(Constants.KEY_COLLECTION_GROUP)
                                                                        .document(group.id)
                                                                        .update(user);
                                                            });
                                                }
                                                showToast(getString(R.string.ExitGroupSuccessfully));
                                                Intent intent = new Intent(getApplicationContext(), MainActivity2.class);
                                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                                startActivity(intent);
                                            });
                                }
                            });
                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                            .get().addOnCompleteListener(task1 -> {
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task1.getResult())
                                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                            .document(queryDocumentSnapshot.getId())
                                            .delete();
                            });

                });
    }

    private void getContactList() {
        contactList.clear();
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

    private Dialog openDialog(int layout) {
        final Dialog dialog = new Dialog(InfoGroupActivity.this);
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


    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onUserClicker(User user) {
        ChatBottomSheetFragment bottomSheetDialog = ChatBottomSheetFragment.newInstance(user);
        bottomSheetDialog.show(this.getSupportFragmentManager(), bottomSheetDialog.getTag());
    }

    @Override
    public void onGroupMemberSelection(Boolean isSelected) {

    }

    @Override
    public void onChangeTeamLeadClicker(User user) {
        changeTeamLeader(user);
    }
}