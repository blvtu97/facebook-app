package nhom10.com.socialproject.adapters;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import java.util.ArrayList;
import java.util.List;

public class ViewPagerAdapter extends FragmentStatePagerAdapter {

    private final List<Fragment> fragmentList = new ArrayList<>();

    private final List<String> fragmentNameList = new ArrayList<>();

    public ViewPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public void addFragment(Fragment fragment, String name){
        fragmentList.add(fragment);
        fragmentNameList.add(name);
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return fragmentNameList.get(position);
    }

    @Override
    public Fragment getItem(int i) {
        return fragmentList.get(i);
    }

    @Override
    public int getCount() {
        return fragmentList.size();
    }
}
