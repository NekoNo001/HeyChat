package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import com.example.heychat.R;
import com.example.heychat.ultilities.Constants;
import com.google.firebase.FirebaseException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.concurrent.TimeUnit;

public class SendOTPActivity extends AppCompatActivity {

    private TextView txtMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_otpactivity);

        final EditText inputMobile = findViewById(R.id.inputMobile);
        Button btnGetOTP = findViewById(R.id.buttonGetOTP);
        txtMessage = findViewById(R.id.txtMessage);
        final ProgressBar progressBar = findViewById(R.id.progressBar);

        btnGetOTP.setOnClickListener(view -> {
            if (inputMobile.getText().toString().trim().isEmpty()){
                Toast.makeText(SendOTPActivity.this, getString(R.string.Phone), Toast.LENGTH_SHORT).show();
                return;
            }
            if(inputMobile.getText().toString().trim().length() != 9){
                Toast.makeText(SendOTPActivity.this, getString(R.string.Phone_9_numbers), Toast.LENGTH_SHORT).show();
                return;
            }
            String newUser = "0" + inputMobile.getText().toString().trim();
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            database.collection(Constants.KEY_COLLECTION_USER)
                    .get()
                    .addOnCompleteListener(task -> {
                        if(task.isSuccessful() && task.getResult() != null) {
                            for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                if(newUser.equals(queryDocumentSnapshot.getString(Constants.KEY_EMAIL))){
                                    Toast.makeText(getApplicationContext(), getString(R.string.User_already), Toast.LENGTH_SHORT).show();
                                    progressBar.setVisibility(View.INVISIBLE);
                                    btnGetOTP.setVisibility(View.VISIBLE);
                                    return;
                                }
                            }
                            progressBar.setVisibility(View.VISIBLE);
                            btnGetOTP.setVisibility(View.INVISIBLE);

                            PhoneAuthProvider.getInstance().verifyPhoneNumber(
                                    "+84"
                                            + inputMobile.getText().toString(),
                                    60,
                                    TimeUnit.SECONDS,
                                    SendOTPActivity.this,
                                    new PhoneAuthProvider.OnVerificationStateChangedCallbacks(){
                                        @Override
                                        public void onVerificationCompleted(@NonNull PhoneAuthCredential phoneAuthCredential) {
                                            progressBar.setVisibility(View.GONE);
                                            btnGetOTP.setVisibility(View.VISIBLE);

                                        }

                                        @Override
                                        public void onVerificationFailed(@NonNull FirebaseException e) {
                                            progressBar.setVisibility(View.GONE);
                                            btnGetOTP.setVisibility(View.VISIBLE);
                                            txtMessage.setText(e.getMessage());
                                            Toast.makeText(SendOTPActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                                        }

                                        @Override
                                        public void onCodeSent(@NonNull String verificationId, @NonNull PhoneAuthProvider.ForceResendingToken forceResendingToken) {
                                            progressBar.setVisibility(View.GONE);
                                            btnGetOTP.setVisibility(View.VISIBLE);
                                            Intent intent = new Intent(getApplicationContext(), VerifyOTPActivity.class);
                                            intent.putExtra("mobile", inputMobile.getText().toString());
                                            intent.putExtra("verificationId", verificationId);
                                            startActivity(intent);
                                        }
                                    }
                            );
                        }
                    });


        });

    }

}