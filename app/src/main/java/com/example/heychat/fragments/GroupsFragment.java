package com.example.heychat.fragments;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.heychat.R;
import com.example.heychat.activities.ChatGroupActivity;
import com.example.heychat.adapters.ChatGroupBottomSheetFragment;
import com.example.heychat.adapters.GroupAdapter;
import com.example.heychat.listeners.GroupListener;
import com.example.heychat.models.Group;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class GroupsFragment extends Fragment implements GroupListener {

    private GroupAdapter groupAdapter;
    private PreferenceManager preferenceManager;
    private RecyclerView recyclerView;
    FirebaseFirestore database;

    public GroupsFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_groups, container, false);
        preferenceManager = new PreferenceManager(getContext());
        database = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.userRecyclerView);
        getUGroups();
        return view;
    }

    private void getUGroups() {
        List<Group> groups = new ArrayList<>();
        database.collection(Constants.KEY_COLLECTION_USER).document(preferenceManager.getString(Constants.KEY_USER_ID))
                .collection(Constants.KEY_GROUP_ID).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
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
                                                    for (QueryDocumentSnapshot qr : queryDocumentSnapshots) {
                                                        group.member.add(qr.getId());
                                                    }
                                                });
                                        groups.add(group);
                                        groupAdapter = new GroupAdapter(groups, this);
                                        recyclerView.setAdapter(groupAdapter);
                                    });
                        }


                    }
                });
    }

    @Override
    public void onGroupClicker(Group group) {
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