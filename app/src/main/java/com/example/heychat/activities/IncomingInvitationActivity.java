package com.example.heychat.activities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.heychat.R;
import com.example.heychat.network.ApiClient;
import com.example.heychat.network.ApiService;
import com.example.heychat.service.SinchService;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallListener;
import com.sinch.android.rtc.video.VideoCallListener;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class IncomingInvitationActivity extends BaseSinchActivity {

    private MediaPlayer music;
    private String meetingType = null;
    private PreferenceManager preferenceManager;
    private String callId;
    ImageView imageAcceptInvitation;
    ImageView imageRejectInvitation;
    private FirebaseFirestore database;
    private String receiverId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_incoming_invitation);
        database = FirebaseFirestore.getInstance();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        preferenceManager = new PreferenceManager(this);
        String userName = preferenceManager.getString(Constants.KEY_USER_ID);

        ImageView imageMeetingType = findViewById(R.id.imageMeetingType);
        meetingType = getIntent().getStringExtra(Constants.REMOTE_MSG_MEETING_TYPE);

        if (meetingType != null) {
            if (meetingType.equals("video")) {
                imageMeetingType.setImageResource(R.drawable.ic_video_call);
            } else {
                imageMeetingType.setImageResource(R.drawable.ic_call);
            }
        }

        if (getSinchServiceInterface() != null && !getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(userName);
        }

        CircleImageView textFirstChar = findViewById(R.id.image_user_incoming);
        TextView textUsername = findViewById(R.id.textUsername);
        TextView textEmail = findViewById(R.id.incomingtextEmail);

        textUsername.setText(
                getIntent().getStringExtra(Constants.KEY_NAME)
        );

        textEmail.setText(
                getIntent().getStringExtra(Constants.KEY_EMAIL)
        );

        receiverId = getIntent().getStringExtra(Constants.KEY_USER_ID);

        database.collection(Constants.KEY_COLLECTION_USER).document(receiverId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                String image = documentSnapshot.getString(Constants.KEY_IMAGE);
                textFirstChar.setImageBitmap(getUserImage(image));
            }
        });

        imageAcceptInvitation = findViewById(R.id.imageAcceptInvitaion);
        imageRejectInvitation = findViewById(R.id.imageRejectInvitaion);
        imageRejectInvitation.setEnabled(false);
        imageAcceptInvitation.setEnabled(false);

        callId = getIntent().getStringExtra(SinchService.CALL_ID);
    }


    private void sendInvitationResponse(String type, String receiverToken) {
        try {
            JSONArray tokens = new JSONArray();
            tokens.put(receiverToken);

            JSONObject body = new JSONObject();
            JSONObject data = new JSONObject();

            data.put(Constants.REMOTE_MSG_TYPE, Constants.REMOTE_MSG_INVITATION_RESPONSE);
            data.put(Constants.REMOTE_MSG_INVITATION_RESPONSE, type);

            body.put(Constants.REMOTE_MSG_DATA, data);
            body.put(Constants.REMOTE_MSG_REGISTRATION_IDS, tokens);

            sendRemoteMessage(body.toString(), type);
        } catch (Exception exception) {
            Toast.makeText(IncomingInvitationActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }

    }

    private void sendRemoteMessage(String remoteMesageBody, String type) {
        ApiClient.getClient().create(ApiService.class).sendMessage(Constants.getRemoteMsgHeaders(), remoteMesageBody)
                .enqueue(new Callback<String>() {
                    @Override
                    public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                        if (response.isSuccessful()) {
                            if (type.equals(Constants.REMOTE_MSG_INVITATION_ACCEPTED)) {
                                try {
                                    if (meetingType.equals("video")) {
                                        Toast.makeText(IncomingInvitationActivity.this, "Accepted: " + callId, Toast.LENGTH_SHORT).show();
                                        com.sinch.android.rtc.calling.Call sinchCall = getSinchServiceInterface().getCall(callId);

                                        Log.d("serviceapp", "BaseSinchActivity sinchCall" + callId);
                                        Toast.makeText(getApplicationContext(), "sinchCall " + sinchCall.getCallId(), Toast.LENGTH_SHORT).show();

                                        if (sinchCall != null) {
                                            sinchCall.addCallListener(new SinchCallListener());
                                            sinchCall.answer();
                                            Intent callIntent = new Intent(IncomingInvitationActivity.this, VideoCallActivity.class);
                                            callIntent.putExtra(SinchService.CALL_ID, callId);
                                            callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            Log.d("serviceapp", "BaseSinchActivity sinchCall" + sinchCall.getCallId());
                                            startActivity(callIntent);
//                                        music.stop();
                                            finish();
                                        } else {
                                            Log.d("serviceapp", "BaseSinchActivity sinchCall null");
                                        }
                                    } else {
                                        Toast.makeText(IncomingInvitationActivity.this, "Accepted: " + callId, Toast.LENGTH_SHORT).show();
                                        com.sinch.android.rtc.calling.Call sinchCall = getSinchServiceInterface().getCall(callId);

                                        Log.d("serviceapp", "BaseSinchActivity sinchCall" + callId);
                                        Toast.makeText(getApplicationContext(), "sinchCall " + sinchCall.getCallId(), Toast.LENGTH_SHORT).show();

                                        if (sinchCall != null) {

                                            sinchCall.addCallListener(new SinchCallListener());
                                            sinchCall.answer();
                                            Intent callIntent = new Intent(IncomingInvitationActivity.this, AudioCallActivity.class);
                                            callIntent.putExtra(SinchService.CALL_ID, callId);
                                            Log.d("serviceapp", "BaseSinchActivity sinchCall" + sinchCall.getCallId());
                                            startActivity(callIntent);
//
                                            finish();
                                        } else {
                                            Log.d("serviceapp", "BaseSinchActivity sinchCall null");
                                        }
                                    }


                                } catch (Exception exception) {
                                    Toast.makeText(IncomingInvitationActivity.this, exception.getMessage(), Toast.LENGTH_SHORT).show();
                                    finish();
                                }

                            } else {
                                Toast.makeText(IncomingInvitationActivity.this, "Invitation Rejected", Toast.LENGTH_SHORT).show();
                                com.sinch.android.rtc.calling.Call sinchCall = getSinchServiceInterface().getCall(callId);
                                sinchCall.hangup();
                                finish();
                            }
                        } else {
                            Toast.makeText(IncomingInvitationActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                            finish();
                        }

                    }

                    @Override
                    public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                        Toast.makeText(IncomingInvitationActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        if (!getSinchServiceInterface().isStarted()) {
            Log.d("serviceapp", "BaseSinchActivity getSinchServiceInterface startClient");
            getSinchServiceInterface().startClient(preferenceManager.getString(Constants.KEY_USER_ID));
        }

        imageAcceptInvitation.setEnabled(true);
        imageRejectInvitation.setEnabled(true);

        imageAcceptInvitation.setOnClickListener(v -> sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_ACCEPTED,
                getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
        ));


        imageRejectInvitation.setOnClickListener(v -> sendInvitationResponse(
                Constants.REMOTE_MSG_INVITATION_REJECTED,
                getIntent().getStringExtra(Constants.REMOTE_MSG_INVITER_TOKEN)
        ));


    }

    private BroadcastReceiver invitationResponseReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            preferenceManager = new PreferenceManager(getApplicationContext());

            String userName = preferenceManager.getString(Constants.KEY_USER_ID);
//            SinchClient mSinchClient;
//            mSinchClient = Sinch.getSinchClientBuilder().context(getApplicationContext()).userId(userName)
//                    .applicationKey(SinchService.APP_KEY)
//                    .applicationSecret(SinchService.APP_SECRET)
//                    .environmentHost(SinchService.ENVIRONMENT).build();
//
//            mSinchClient.setSupportCalling(true);
//            mSinchClient.startListeningOnActiveConnection();
//            mSinchClient.start();


            String type = intent.getStringExtra(Constants.REMOTE_MSG_INVITATION_RESPONSE);
            if (type != null) {
                if (type.equals(Constants.REMOTE_MSG_INVITATION_CANCELLED)) {
                    Toast.makeText(IncomingInvitationActivity.this, "Invitation Cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("BBB", "onStart");
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(
                invitationResponseReceiver,
                new IntentFilter(Constants.REMOTE_MSG_INVITATION_RESPONSE)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        music = MediaPlayer.create(getApplicationContext(), R.raw.notification);
        music.setLooping(true);
        music.start();
    }

    @Override
    protected void onStop() {
        super.onStop();
        music.stop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(
                invitationResponseReceiver
        );
    }


    private Bitmap getUserImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private class SinchCallListener implements CallListener {

        @Override
        public void onCallEnded(@NonNull com.sinch.android.rtc.calling.Call call) {
            CallEndCause cause = call.getDetails().getEndCause();

            Log.d("callDetail", "LD: "+ cause.toString());
            String duration = formatTimespan(call.getDetails().getDuration());
            Log.d("callDetail", "Duration: " + duration);
            HashMap<String, Object> callDetail = new HashMap<>();
            callDetail.put(Constants.KEY_CALL_TYPE, meetingType);
            callDetail.put(Constants.KEY_USER_CALL, receiverId);
            callDetail.put(Constants.KEY_USER_RECEIVER, preferenceManager.getString(Constants.KEY_USER_ID));
            callDetail.put(Constants.KEY_CALL_DURATION, duration);
            callDetail.put(Constants.KEY_CALL_CAUSE, cause);
            callDetail.put(Constants.KEY_TIMESTAMP, new Date());
            database.collection(Constants.KEY_COLLECTION_CALL).add(callDetail);
            call.hangup();
            finish();
        }

        @Override
        public void onCallEstablished(com.sinch.android.rtc.calling.Call call) {
//            Log.d(TAG, "Call established");
        }

        @Override
        public void onCallProgressing(com.sinch.android.rtc.calling.Call call) {
//            Log.d(TAG, "Call progressing");
        }

        @Override
        public void onShouldSendPushNotification(com.sinch.android.rtc.calling.Call call, List<PushPair> pushPairs) {
            // Send a push through your push provider here, e.g. GCM
        }

        private String formatTimespan(int timespan) {
            int hour = timespan/3600;
            int minutes = timespan%3600/60;
            int seconds = timespan%3600%60;
            return String.format(Locale.US, "%d:%02d:%02d", hour, minutes, seconds);
        }

    }
}