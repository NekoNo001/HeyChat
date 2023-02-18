package com.example.heychat.adapters;


import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import android.Manifest;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.heychat.R;
import com.example.heychat.listeners.CallListener;
import com.example.heychat.models.CallModel;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class CallAdapter extends RecyclerView.Adapter<CallAdapter.CallViewHolder> {

    private final List<CallModel> calls;
    private final CallListener callListener;

    public CallAdapter(ArrayList<CallModel> calls, CallListener callListener, Context context) {
        this.calls = calls;
        this.callListener = callListener;
    }

    class CallViewHolder extends RecyclerView.ViewHolder {
        TextView username;
        CircleImageView image_user;
        ImageView image_type_call;
        TextView time, typecall;

        public CallViewHolder(@NonNull View itemView) {
            super(itemView);
            username = itemView.findViewById(R.id.user_name_call);
            image_user = itemView.findViewById(R.id.image_user);
            image_type_call = itemView.findViewById(R.id.image_type_call);
            time = itemView.findViewById(R.id.timeCall);
            typecall = itemView.findViewById(R.id.typeCall);
        }

        void setUserData(CallModel call) {
            PreferenceManager preferenceManager = new PreferenceManager(itemView.getContext());
            username.setTextSize(Integer.parseInt(preferenceManager.getString(Constants.KEY_TEXTSIZE)));

            if (call.user != null) {
                username.setText(call.user.name);
                image_user.setImageBitmap(getUserImage(call.user.image));
                if (call.incoming) {
                    typecall.setText(R.string.Incoming_call);
                } else {
                    typecall.setText(R.string.Outgoing_call);
                }
            }

            if (call.type.equals("audio")) {
                image_type_call.setImageResource(R.drawable.ic_call);
                image_type_call.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callListener.initiateAudioCall(call.user);
                    }
                });
            } else {
                image_type_call.setImageResource(R.drawable.ic_video_call);
                image_type_call.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        callListener.initiateVideoCall(call.user);
                    }
                });
            }
            time.setText(call.datetime);

        }

    }

    private Bitmap getUserImage(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;
    }

    @NonNull
    @Override
    public CallViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new CallViewHolder(
                LayoutInflater.from(parent.getContext()).inflate(R.layout.user_call_layaout, parent, false)
        );
    }

    @Override
    public void onBindViewHolder(@NonNull CallViewHolder holder, int position) {
        holder.setUserData(calls.get(position));
    }

    @Override
    public int getItemCount() {
        return calls.size();
    }

}
