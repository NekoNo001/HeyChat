package com.example.heychat.fragments;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.R;
import com.example.heychat.activities.OutgoingInvitationActivity;
import com.example.heychat.adapters.CallAdapter;
import com.example.heychat.listeners.CallListener;
import com.example.heychat.models.CallModel;
import com.example.heychat.models.User;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class VideoCallFragment extends Fragment implements CallListener {

    private PreferenceManager preferenceManager;
    private RecyclerView recyclerView;
    private CallAdapter callAdapter;
    private ArrayList<CallModel> mCalls;
    private ProgressBar progressBar;
    private final ArrayList<String> contactList = new ArrayList<>();
    private FirebaseFirestore database;

    public VideoCallFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_video_call, container, false);
        recyclerView = view.findViewById(R.id.videocall_recyclerview);
        progressBar = view.findViewById(R.id.call_progressbar);
        preferenceManager = new PreferenceManager(getContext());
        database = FirebaseFirestore.getInstance();
        mCalls = new ArrayList<>();
        callAdapter = new CallAdapter(mCalls, this, this.getContext());
        recyclerView.setAdapter(callAdapter);
        listenerCall();
        return view;
    }

    private void listenerCall() {
        String userid = preferenceManager.getString(Constants.KEY_USER_ID);
        database.collection(Constants.KEY_COLLECTION_CALL)
                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.DESCENDING)
                .addSnapshotListener(eventListener);
//        database.collection(Constants.KEY_COLLECTION_CALL)
//                .orderBy(Constants.KEY_TIMESTAMP, Query.Direction.ASCENDING)
//                .whereEqualTo(Constants.KEY_USER_RECEIVER, userid)
//                .addSnapshotListener(eventListener);
    }

    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    CallModel call = new CallModel();

                    String callUser = documentChange.getDocument().getString(Constants.KEY_USER_CALL);
                    String callreceiver = documentChange.getDocument().getString(Constants.KEY_USER_RECEIVER);
                    User user = new User();
                    if (preferenceManager.getString(Constants.KEY_USER_ID).equals(callreceiver)) {
                        String calltype = documentChange.getDocument().getString(Constants.KEY_CALL_TYPE);
                        String cause = documentChange.getDocument().getString(Constants.KEY_CALL_CAUSE);
                        String duration = documentChange.getDocument().getString(Constants.KEY_CALL_DURATION);
                        Date date = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        String datetime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));

                        call.type = calltype;
                        call.duration = duration;
                        call.cause = cause;
                        call.dataObject = date;
                        call.datetime = datetime;
                        call.incoming = true;
                        database.collection(Constants.KEY_COLLECTION_USER).document(callUser).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        user.id = callUser;
                                        user.name = documentSnapshot.getString(Constants.KEY_NAME);
                                        user.image = documentSnapshot.getString(Constants.KEY_IMAGE);
                                        user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                                        user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                        call.user = user;
                                        mCalls.add(call);
                                        Collections.sort(mCalls, (obj1, obj2) -> obj2.dataObject.compareTo(obj1.dataObject));
                                        callAdapter.notifyDataSetChanged();
                                    }
                                });
                    } else if (preferenceManager.getString(Constants.KEY_USER_ID).equals(callUser)) {

                        String calltype = documentChange.getDocument().getString(Constants.KEY_CALL_TYPE);
                        String cause = documentChange.getDocument().getString(Constants.KEY_CALL_CAUSE);
                        String duration = documentChange.getDocument().getString(Constants.KEY_CALL_DURATION);
                        Date date = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        String datetime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));

                        call.type = calltype;
                        call.duration = duration;
                        call.cause = cause;
                        call.dataObject = date;
                        call.datetime = datetime;

                        call.incoming = false;
                        database.collection(Constants.KEY_COLLECTION_USER).document(callreceiver).get()
                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                    @Override
                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                        user.id = callreceiver;
                                        user.name = documentSnapshot.getString(Constants.KEY_NAME);
                                        user.image = documentSnapshot.getString(Constants.KEY_IMAGE);
                                        user.email = documentSnapshot.getString(Constants.KEY_EMAIL);
                                        user.token = documentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                        call.user = user;
                                        mCalls.add(call);
                                        Collections.sort(mCalls, (obj1, obj2) -> obj2.dataObject.compareTo(obj1.dataObject));
                                        callAdapter.notifyDataSetChanged();
                                    }
                                });
                    }
                }
            }
            callAdapter.notifyDataSetChanged();
        }
    };

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("dd MMMM, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.INVISIBLE);
        }
    }


    @Override
    public void initiateVideoCall(User user) {

        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                if (user.token == null || user.token.trim().isEmpty()) {
                    Toast.makeText(getContext(), user.name + getString(R.string.not_available_video_call), Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(getContext(), OutgoingInvitationActivity.class);
                    intent.putExtra("user", user);
                    intent.putExtra("type", "video");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(getContext(), getString(R.string.PermissionDenied) + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage(getString(R.string.PermissionDeniedNoice))
                .setPermissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE)
                .check();


    }

    @Override
    public void initiateAudioCall(User user) {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                if (user.token == null || user.token.trim().isEmpty()) {
                    Toast.makeText(getContext(), user.name + getString(R.string.not_available_audio_call), Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(getContext(), OutgoingInvitationActivity.class);
                    intent.putExtra("user", user);
                    intent.putExtra("type", "audio");
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
                            | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(getContext(), getString(R.string.PermissionDenied) + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage(getString(R.string.PermissionDeniedNoice))
                .setPermissions(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE)
                .check();

    }

}