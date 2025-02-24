package com.example.cab_approval_system;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class History_ViewPager extends FragmentStateAdapter {
    private final boolean isFH;
    private final String requesterEmail;
    private final String requesterTeam;

    public History_ViewPager(@NonNull FragmentActivity fragmentActivity, boolean isFH, String requesterEmail, String requesterTeam) {
        super(fragmentActivity);
        this.isFH = isFH;
        this.requesterEmail = requesterEmail;
        this.requesterTeam = requesterTeam;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (isFH) {
            if (position == 0) {
                return PersonalHistoryFragment.newInstance(requesterEmail);
            } else {
                return DepartmentHistoryFragment.newInstance(requesterTeam);
            }
        } else {
            return PersonalHistoryFragment.newInstance(requesterEmail);
        }
    }

    @Override
    public int getItemCount() {
        return isFH ? 2 : 1; // FH gets 2 tabs, others get 1
    }
}
