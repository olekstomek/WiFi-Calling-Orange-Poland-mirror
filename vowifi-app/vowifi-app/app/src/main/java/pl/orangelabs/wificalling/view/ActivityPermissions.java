/*
 * Copyright (C) 2017 Orange Polska SA
 *
 * This file is part of WiFi Calling.
 *
 * WiFi Calling is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  WiFi Calling is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty o
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Foobar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

package pl.orangelabs.wificalling.view;

import android.app.Activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import pl.orangelabs.wificalling.LauncherActivity;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.model.PermissionItem;
import pl.orangelabs.wificalling.model.PermissionList;

public class ActivityPermissions extends ActivityBase
{
    private PermissionPagesAdapter mPermissionPagesAdapter;
    private ViewPager mViewPager;
    private PermissionList permissionList;
    private LinearLayout llPermissionDescription;
    private TextView tvPermissionName;
    private TextView tvPermissionDescription;
    private Button buttonNext;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);
        permissionList = new PermissionList();
        permissionList.addMissingPermisions(getApplicationContext());
        llPermissionDescription = (LinearLayout) findViewById(R.id.ll_permission_info);
        tvPermissionName = (TextView) findViewById(R.id.tv_permission_name);
        tvPermissionDescription = (TextView) findViewById(R.id.tv_permission_description);
        mPermissionPagesAdapter = new PermissionPagesAdapter(getSupportFragmentManager(), permissionList);
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mPermissionPagesAdapter);
        buttonNext = (Button) findViewById(R.id.btn_next);
        buttonNext.setOnClickListener(v -> LauncherActivity.showInitActivity(getApplicationContext()));
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {

            }

            @Override
            public void onPageSelected(int position)
            {
                changePermissionTextWithAnimation(position);
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
        TextView textView = (TextView) findViewById(R.id.tv_title);
        createTextViewEnterDefaultTitle(textView);
        changePermissionTextViews(0);
    }

    private void changePermissionTextWithAnimation(final int position)
    {
        runAnimation(ActivityPermissions.this, llPermissionDescription,android.R.anim.slide_out_right).setAnimationListener(new Animation.AnimationListener()
        {
            @Override
            public void onAnimationStart(Animation animation)
            {

            }

            @Override
            public void onAnimationEnd(Animation animation)
            {
                changePermissionTextViews(position);
                runAnimation(ActivityPermissions.this, llPermissionDescription, android.R.anim.slide_in_left);
            }

            @Override
            public void onAnimationRepeat(Animation animation)
            {

            }
        });
    }

    private void changePermissionTextViews(int position)
    {
        PermissionItem permissionItem = permissionList.getPermissionItems().get(position);
        tvPermissionName.setText(permissionItem.getNameId());
        tvPermissionDescription.setText(permissionItem.getTextId());
    }


    public static class PermissionPictureFragment extends Fragment
    {
        PermissionItem permissionItem;
        public static String PERMISSION_ITEM = "PermissionItem";
        public PermissionPictureFragment()
        {
        }

        public static PermissionPictureFragment newInstance(PermissionItem permissionItem)
        {
            PermissionPictureFragment fragment = new PermissionPictureFragment();
            Bundle args = new Bundle();
            args.putSerializable(PERMISSION_ITEM,permissionItem);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState)
        {
            View rootView = inflater.inflate(R.layout.fragment_permission, container, false);
            permissionItem = (PermissionItem) getArguments().getSerializable(PERMISSION_ITEM);
            ((ImageView)rootView.findViewById(R.id.iv_permission)).setImageResource(permissionItem.getDrawable());
            return rootView;
        }
    }

    public static Animation runAnimation(Activity ctx, View target, int id) {
        Animation animation = AnimationUtils.loadAnimation(ctx, id);
        target.startAnimation(animation);
        return animation;
    }

    public class PermissionPagesAdapter extends FragmentPagerAdapter
    {
        PermissionList permissionList;
        public PermissionPagesAdapter(FragmentManager fm, PermissionList permissionList)
        {
            super(fm);
            this.permissionList = permissionList;
        }

        @Override
        public Fragment getItem(int position)
        {
            return PermissionPictureFragment.newInstance(permissionList.getPermissionItems().get(position));
        }

        @Override
        public int getCount()
        {
            return permissionList.getPermissionItems().size();
        }
    }
}
