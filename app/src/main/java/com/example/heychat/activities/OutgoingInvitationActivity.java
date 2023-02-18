package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Base64;
import android.util.Log;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.heychat.R;
import com.example.heychat.models.User;
import com.example.heychat.network.ApiClient;
import com.example.heychat.network.ApiService;
import com.example.heychat.service.SinchService;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.iid.FirebaseInstanceId;
import com.sinch.android.rtc.SinchError;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OutgoingInvitationActivity extends BaseSinchActivity {

    private PreferenceManager preferenceManager;
    private String inviterToken = null;
    private String meetingType = null;
    private int currentProgress = 0;
    private ProgressBar progressBar;
    private CountDownTimer countDownTimer;
    private User receiver;
    private String callId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_outgoing_invitation);

        preferenceManager = new PreferenceManager(getApplicationContext());

        ImageView imageMeetingType = findViewById(R.id.imageMeetingTypeout);
        meetingType = getIntent().getStringExtra("type");

        if (meetingType != null) {
            if (meetingType.equals("video")) {
                imageMeetingType.setImageResource(R.drawable.ic_video_call);
            } else {
                imageMeetingType.setImageResource(R.drawable.ic_call);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA, Manifest.permission.ACCESS_NETWORK_STATE, Manifest.permission.READ_PHONE_STATE}, 100);
        }

        CircleImageView textFirstChar = findViewById(R.id.textFirstChar);
        TextView textUsername = findViewById(R.id.textUsername);
        TextView textEmail = findViewById(R.id.outgoingtextEmail);
        progressBar = findViewById(R.id.call_duration);

        receiver = (User) getIntent().getSerializableExtra("user");
        if (receiver != null) {
            textFirstChar.setImageBitmap(getUserImage(receiver.image));
            textUsername.setText(receiver.name);
            textEmail.setText(receiver.email);
        }

        ImageView imageStopInvitation = findViewById(R.id.imageStopInvatation);
        imageStopInvitation.setOnClickListener(v -> {
            if (receiver != null) {
                cancelInvitation(receiver.token);
            }
        });


        countDownTimer = new CountDownTimer(30 * 1000, 50) {
            @Override
            public void onTick(long l) {
                progressBar.setMax(30000 * 4);
                currentProgress += 200;
                if (currentProgress >= progressBar.getMax()) {
                    cancelInvitation(receiver.token);
                }

                progressBar.setProgress(currentProgress);

            }

            @Override
            public void onFinish() {
                if (receiver != null) {
                    currentProgress = 0;
                }
            }
        };
        countDownTimer.start();
    }

    private void initiateMeeting(String meetingType, String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);
            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION);
            data.put(Constants.REMOTE_MSG_MEETING_TYPE, meetingType);
//            data.put(Constants.KEY_IMAGE, (preferenceManager.getString(Constants.KEY_IMAGE)));
            data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
            data.put(Constants.KEY_EMAIL, preferenceManager.getString(Constants.KEY_EMAIL));
            data.put(Constants.REMOTE_MSG_INVITER_TOKEN, inviterToken);


            data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            data.put(SinchService.CALL_ID, callId);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION);

        } catch (Exception exception) {
            Toast.makeText(this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void sendRemoteMessage(String remoteMesageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendMessage(Constants.getRemoteMsgHeaders(), remoteMesageBody)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful()) {
                            if (type.equals(Constants.REMOTE_MSG_INVITATION)) {
                                Toast.makeText(OutgoingInvitationActivity.this, getString(R.string.Invitation_successful), Toast.LENGTH_SHORT).show();
                            } else if (type.equals(Constants.REMOTE_MSG_INVITATION_RESPONSE)) {
                                Toast.makeText(OutgoingInvitationActivity.this, getString(R.string.Invitation_Cancelled), Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } else {
                            Toast.makeText(OutgoingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        Toast.makeText(OutgoingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    private void cancelInvitation(String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, Constants.REMOTE_MSG_INVITATION_CANCELLED);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), Constants.REMOTE_MSG_INVITATION_RESPONSE);

        } catch (Exception exception) {
            Toast.makeText(OutgoingInvitationActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (meetingType.equals("video")){
            com.sinch.android.rtc.calling.Call call = getSinchServiceInterface().callUserVideo(receiver.id);
            if (call == null){
                finish();
                return;
            }
            callId = call.getCallId();
        } else {
            com.sinch.android.rtc.calling.Call call = getSinchServiceInterface().callUserAudio(receiver.id);
            if (call == null){
                finish();
                return;
            }
            callId = call.getCallId();
        }

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                inviterToken = task.getResult().getToken();
                if (meetingType != null && receiver != null) {
                    initiateMeeting(meetingType, receiver.token);
                }
            }
        });
    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.trim().equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                    try {
                        if (meetingType.equals("video")){
                            Toast.makeText(context, getString(R.string.Accepted) , Toast.LENGTH_SHORT).show();
                            Intent callScreen = new Intent(OutgoingInvitationActivity.this, VideoCallActivity.class);
                            callScreen.putExtra(SinchService.CALL_ID, callId);
                            callScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(callScreen);
                            finish();
                        } else {
                            Toast.makeText(context, getString(R.string.Accepted), Toast.LENGTH_SHORT).show();
                            Intent callScreen = new Intent(OutgoingInvitationActivity.this, AudioCallActivity.class);
                            callScreen.putExtra(SinchService.CALL_ID, callId);
                            callScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(callScreen);
                            finish();
                        }

                    } catch (Exception exception) {
                        Toast.makeText(context, exception.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                } else if (type.trim().equals(Constants.REMOTE_MSG_INVITATION_REJECTED)) {
                    Toast.makeText(context, getString(R.string.Invitation_Rejected), Toast.LENGTH_SHORT).show();
                    finish();
                }
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        countDownTimer.onFinish();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }

    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }


}