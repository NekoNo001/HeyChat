package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.heychat.R;
import com.example.heychat.databinding.ActivitySetInfoGroupBinding;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class SetInfoGroupActivity extends AppCompatActivity {

    private ActivitySetInfoGroupBinding binding;
    private PreferenceManager preferenceManager;
    private String encodedImage;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySetInfoGroupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        setListeners();
        db = FirebaseFirestore.getInstance();
    }

    private void setListeners() {

        binding.btnCreateGroup.setOnClickListener(view -> {
            if (isValidSignUpDetails()) {
                createGroup();
            }
        });

        binding.layoutImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            pickImage.launch(intent);
        });
    }

    private void createGroup() {
        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        HashMap<String, Object> user = new HashMap<>();
        user.put(Constants.KEY_GROUP_NAME, binding.inputGroupName.getText().toString());
        user.put(Constants.KEY_GROUP_IMAGE, encodedImage);
        user.put(Constants.KEY_GROUP_OWNER, preferenceManager.getString(Constants.KEY_USER_ID));
        //user.put(Constants.KEY_GROUP_MEMBER, getIntent().getStringExtra(Constants.KEY_GROUP_MEMBER));

        String[] userIds = getIntent().getStringExtra(Constants.KEY_GROUP_MEMBER).toString().trim().split(",");

        database.collection(Constants.KEY_COLLECTION_GROUP)
                .add(user).addOnSuccessListener(documentReference -> {
                    for (String userId : userIds) {
                        HashMap<String, Object> id = new HashMap<>();
                        id.put("Owner", true);
                        database.collection(Constants.KEY_COLLECTION_GROUP).document(documentReference.getId())
                                .collection(Constants.KEY_GROUP_MEMBER).document(userId)
                                .set(id).addOnSuccessListener(aVoid ->
                                        database.collection(Constants.KEY_COLLECTION_USER)
                                                .document(userId)
                                                .collection(Constants.KEY_GROUP_ID)
                                                .document(documentReference.getId())
                                                .set(id)
                                                .addOnSuccessListener(aVoid1 -> {
                                                    startActivity(new Intent(this, MainActivity2.class));
                                                })
                                                .addOnFailureListener(e -> {

                                                }))
                                .addOnFailureListener(e -> {

                                });

                    }
                });
    }

    private void updateData(String member, String groupID) {
        Map<String, Object> userDetail = new HashMap<>();
        userDetail.put(Constants.KEY_GROUP_ID, groupID);
        db.collection(Constants.KEY_COLLECTION_USER)
                .whereEqualTo(Constants.KEY_EMAIL, member)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        String documentID = documentSnapshot.getId();
                        db.collection(Constants.KEY_COLLECTION_USER)
                                .document(documentID)
                                .update(userDetail)
                                .addOnSuccessListener(unused -> showToast("Successful"))
                                .addOnFailureListener(e -> showToast("Failed"));
                    } else {
                        showToast("Failed");
                    }
                });
    }

    private final ActivityResultLauncher pickImage = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    if (result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                            binding.imageProfile.setImageBitmap(bitmap);
                            binding.textAddImage.setVisibility(View.GONE);
                            encodedImage = encodeImage(bitmap);
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
    );

    private String encodeImage(Bitmap bitmap) {
        int previewWidth = 150;
        int previewHeight = bitmap.getHeight() * previewWidth / bitmap.getWidth();
        Bitmap previewBitmap = Bitmap.createScaledBitmap(bitmap, previewWidth, previewHeight, false);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        previewBitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] bytes = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(bytes, Base64.DEFAULT);
    }

    private Boolean isValidSignUpDetails() {
        if (encodedImage == null) {
            showToast(getString(R.string.Select_image));
            return false;
        } else if (binding.inputGroupName.getText().toString().trim().isEmpty()) {
            showToast(getString(R.string.Enter_name));
            return false;
        } else {
            return true;
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.btnCreateGroup.setVisibility(View.INVISIBLE);
            binding.progreeBar.setVisibility(View.VISIBLE);
        } else {
            binding.btnCreateGroup.setVisibility(View.VISIBLE);
            binding.progreeBar.setVisibility(View.INVISIBLE);
        }
    }

}