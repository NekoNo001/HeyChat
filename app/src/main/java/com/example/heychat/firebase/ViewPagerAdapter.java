package com.example.heychat.firebase;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.example.heychat.fragments.ConfigFragment;
import com.example.heychat.fragments.ContactsFragment;
import com.example.heychat.fragments.HomeFragment;
import com.example.heychat.fragments.ProfileFragment;
import com.example.heychat.fragments.VideoCallFragment;

public class ViewPagerAdapter extends FragmentStateAdapter {


    public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }


    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position){
            case 0: return new HomeFragment();
            case 1: return new VideoCallFragment();
            case 2: return new ContactsFragment();
            case 3: return new ProfileFragment();
            default: return new HomeFragment();
        }
    }

    @Override
    public int getItemCount() {
        return 4;
    }

}
