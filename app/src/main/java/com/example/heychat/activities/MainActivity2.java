package com.example.heychat.activities;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.recyclerview.widget.LinearLayoutManager;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;

import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import com.example.heychat.R;

import com.example.heychat.adapters.ChatBottomSheetFragment;
import com.example.heychat.adapters.LanguageManager;
import com.example.heychat.adapters.UserOnlineAdapter;
import com.example.heychat.databinding.ActivityMain2Binding;
import com.example.heychat.firebase.ViewPagerAdapter;

import com.example.heychat.fragments.ProfileFragment;
import com.example.heychat.listeners.UserListener;

import com.example.heychat.models.User;
import com.example.heychat.service.SinchService;
import com.example.heychat.ultilities.Constants;
import com.example.heychat.ultilities.PreferenceManager;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.normal.TedPermission;
import com.sinch.android.rtc.SinchError;

import net.opacapp.multilinecollapsingtoolbar.CollapsingToolbarLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity2 extends BaseSinchActivity implements UserListener, SinchService.StartFailedListener {

    private ActivityMain2Binding binding;
    private CollapsingToolbarLayout collapsingToolbarLayout;
    private ViewPagerAdapter viewPagerAdapter;
    private UserOnlineAdapter userOnlineAdapter;
    private ArrayList<User> users;
    private PreferenceManager preferenceManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        requestPermission();
        collapsingToolbarLayout = findViewById(R.id.collapsing_toolbar);
        setUpTablayout();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        binding.recyclerviewUserOnline.setLayoutManager(linearLayoutManager);
        users = new ArrayList<>();
        userOnlineAdapter = new UserOnlineAdapter(users, this);
        binding.recyclerviewUserOnline.setAdapter(userOnlineAdapter);
        getUsersOnline();

        if (preferenceManager.getString(Constants.KEY_TEXTSIZE) == null)
            preferenceManager.putString(Constants.KEY_TEXTSIZE, "18");

        if (preferenceManager.getString(Constants.KEY_BLOCK_SCREENSHOT) == null)
            preferenceManager.putString(Constants.KEY_BLOCK_SCREENSHOT, "unlock");

        binding.searchButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        });

        binding.groupButton.setOnClickListener(view -> {
            Intent intent = new Intent(getApplicationContext(), CreateGroupActivity.class);
            startActivity(intent);
        });


    }

    private void getUsersOnline() {
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USER).get()
                .addOnCompleteListener(task -> {
                            String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                            if (task.isSuccessful() && task.getResult() != null) {
                                users.clear();
                                for (QueryDocumentSnapshot queryDocumentSnapshot : task.getResult()) {
                                    if (currentUserId.equals(queryDocumentSnapshot.getId())) {
                                        continue;
                                    }
                                    if (queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN) != null && !queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN).isEmpty()) {
                                        User user = new User();
                                        user.name = queryDocumentSnapshot.getString(Constants.KEY_NAME);
                                        user.email = queryDocumentSnapshot.getString(Constants.KEY_EMAIL);
                                        user.image = queryDocumentSnapshot.getString(Constants.KEY_IMAGE);
                                        user.token = queryDocumentSnapshot.getString(Constants.KEY_FCM_TOKEN);
                                        user.id = queryDocumentSnapshot.getId();
                                        users.add(user);
                                    }

                                }
                                userOnlineAdapter.notifyDataSetChanged();
                            }
                        }
                );
    }

    private void requestPermission() {
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {

            }

            @Override
            public void onPermissionDenied(List<String> deniedPermissions) {
                Toast.makeText(getApplicationContext(),  context.getString(R.string.PermissionDenied) + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        TedPermission.create()
                .setPermissionListener(permissionlistener)
                .setDeniedMessage(getString(R.string.PermissionDeniedNoice))
                .setPermissions(Manifest.permission.SYSTEM_ALERT_WINDOW)
                .check();
    }

    private void setUpTablayout() {
        viewPagerAdapter = new ViewPagerAdapter(this);
        binding.viewPager.setAdapter(viewPagerAdapter);
        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(binding.tablayout, binding.viewPager, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0: {
                        tab.setText(R.string.Chat);
                        break;
                    }
                    case 1: {
                        tab.setText(R.string.Call);
                        break;
                    }
                    case 2: {
                        tab.setText(R.string.Contacts);
                        break;
                    }
                    case 3: {
                        tab.setText(R.string.Profile);
                        break;
                    }
                }
            }
        });
        tabLayoutMediator.attach();

        for (int i = 0; i < 4; i++) {
            TextView textView = (TextView) LayoutInflater.from(this).inflate(R.layout.tab_title, null, false);
            binding.tablayout.getTabAt(i).setCustomView(textView);
        }

        binding.tablayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                switch (tab.getPosition()) {
                    case 0: {
                        collapsingToolbarLayout.setTitle(getString(R.string.ChatDescribe));
                        break;
                    }
                    case 1: {
                        collapsingToolbarLayout.setTitle(getString(R.string.CallDescribe));
                        break;
                    }
                    case 2: {
                        collapsingToolbarLayout.setTitle(getString(R.string.Contacts));
                        break;
                    }
                    case 3: {
                        collapsingToolbarLayout.setTitle(getString(R.string.Profile));
                        break;
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
    }

    @Override
    public void onUserClicker(User user) {
        ChatBottomSheetFragment bottomSheetDialog = ChatBottomSheetFragment.newInstance(user);
        bottomSheetDialog.show(this.getSupportFragmentManager(), bottomSheetDialog.getTag());
    }

    @Override
    protected void onServiceConnected() {
        Log.d("serviceapp", "MainActivity  onServiceConnected");
        if (!getSinchServiceInterface().isStarted()) {
            getSinchServiceInterface().startClient(preferenceManager.getString(Constants.KEY_USER_ID));
        }
        getSinchServiceInterface().setStartListener(this);
    }

    @Override
    public void onStartFailed(SinchError error) {
        Log.d("serviceapp", "MainActivity  onStartFailed");
        Toast.makeText(this, error.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onStarted() {

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    public void onDestroy() {
        if (getSinchServiceInterface() != null) {
            getSinchServiceInterface().stopClient();
        }
        super.onDestroy();
    }

    @Override
    protected void attachBaseContext(Context newBase) {

        final Configuration override = new Configuration(newBase.getResources().getConfiguration());
        override.fontScale = 1.0f;
        applyOverrideConfiguration(override);

        super.attachBaseContext(newBase);
    }



}