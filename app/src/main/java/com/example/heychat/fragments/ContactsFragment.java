package com.example.heychat.fragments;

import static com.gun0912.tedpermission.provider.TedPermissionProvider.context;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import com.example.heychat.R;
import com.example.heychat.adapters.ViewPagerContactsAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class ContactsFragment extends Fragment {

    private TabLayout tabLayout;
    private ViewPager2 viewPager2;
    private ViewPagerContactsAdapter viewPagerContactsAdapter;

    public ContactsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_contacts, container, false);
        tabLayout = view.findViewById(R.id.tablayout);
        viewPager2 = view.findViewById(R.id.viewpager);
        setupTabLayout();
        return view;
    }

    private void setupTabLayout() {
        viewPagerContactsAdapter = new ViewPagerContactsAdapter(this);
        viewPager2.setAdapter(viewPagerContactsAdapter);
        TabLayoutMediator tabLayoutMediator = new TabLayoutMediator(tabLayout, viewPager2, new TabLayoutMediator.TabConfigurationStrategy() {
            @Override
            public void onConfigureTab(@NonNull TabLayout.Tab tab, int position) {
                switch (position) {
                    case 0: {
                        tab.setText(getString(R.string.Friends));
                        break;
                    }
                    case 1: {
                        tab.setText(getString(R.string.Groups));
                        break;
                    }
                }
            }
        });

        tabLayoutMediator.attach();
    }

}