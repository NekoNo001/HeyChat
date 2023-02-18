package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.heychat.R;
import com.example.heychat.adapters.GroupAdapter;
import com.example.heychat.databinding.ActivityGroupBinding;
import com.example.heychat.listeners.GroupListener;
import com.example.heychat.models.Group;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupActivity extends BaseActivity implements GroupListener {

    private ActivityGroupBinding binding;
    private PreferenceManager preferenceManager;
    private GroupAdapter groupAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        getUGroups();
        setListeners();
    }

    private void setListeners() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
    }


    private void getUGroups() {
        loading(true);
        List<Group> groups = new ArrayList<>();
        FirebaseFirestore database = FirebaseFirestore.getInstance();

        database.collection(Constants.KEY_COLLECTION_USER).document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_GROUP_ID).get()
                .addOnCompleteListener(task -> {
                    loading(false);
                    if (task.isSuccessful() && task.getResult() != null){
                        for(QueryDocumentSnapshot queryDocumentSnapshot: task.getResult()){
                            String groupId = queryDocumentSnapshot.getId();
                            database.collection(Constants.KEY_COLLECTION_GROUP).document(groupId).get()
                                    .addOnSuccessListener(documentSnapshot -> {

                                        Group group = new Group();
                                        group.id = groupId;
                                        group.name = documentSnapshot.getString(Constants.KEY_GROUP_NAME);
                                        group.image = documentSnapshot.getString(Constants.KEY_GROUP_IMAGE);
                                        group.member = new ArrayList<>();
                                        database.collection(Constants.KEY_COLLECTION_GROUP).document(groupId)
                                                .collection(Constants.KEY_GROUP_MEMBER).get()
                                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                                    for (QueryDocumentSnapshot qr: queryDocumentSnapshots){
                                                        group.member.add(qr.getId());
                                                    }
                                                });
                                        groups.add(group);
                                        groupAdapter = new GroupAdapter(groups, this);
                                        binding.userRecyclerView.setAdapter(groupAdapter);
                                        binding.userRecyclerView.setVisibility(View.VISIBLE);

                                    });


                        }


                    }
                });
    }

    private void showErrorMessage() {
        binding.textErrorMessage.setText(String.format("%s", R.string.No_user));
        binding.textErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public void onGroupClicker(Group group) {
        Intent intent = new Intent(getApplicationContext(), ChatGroupActivity.class);
        intent.putExtra(Constants.KEY_GROUP, group);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}