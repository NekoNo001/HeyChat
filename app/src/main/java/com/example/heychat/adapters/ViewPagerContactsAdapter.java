package com.example.heychat.adapters;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.heychat.fragments.ConfigFragment;
import com.example.heychat.fragments.ContactsFragment;
import com.example.heychat.fragments.GroupsFragment;
import com.example.heychat.fragments.HomeFragment;
import com.example.heychat.fragments.VideoCallFragment;

public class ViewPagerContactsAdapter extends FragmentStateAdapter {

    public ViewPagerContactsAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {
            case 0:
                return new ConfigFragment();
            case 1:
                return new GroupsFragment();
            default:
                return new ConfigFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
