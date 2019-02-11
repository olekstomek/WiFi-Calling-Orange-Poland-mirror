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

package pl.orangelabs.wificalling.view.activation_client;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.LinkedList;
import java.util.List;

import pl.orangelabs.wificalling.R;

public class ActivityActivationTutorial extends AppCompatActivity implements FragmentTutorial.FragmentTutorialInteractionInterface
{
    public static final String EXTRA_TUTORIAL = "IS_FROM_MENU";

    private ViewPager viewPager;
    private View btnEnd;
    private View btnNext;
    private boolean mIsFromMenu = false;

    private tutorialPage[] textID = new tutorialPage[] {new tutorialPage(R.drawable.tutorial_1),
        new tutorialPage(R.drawable.tutorial_2, true),
        new tutorialPage(R.drawable.tutorial_3)};

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        this.overridePendingTransition(R.anim.anim_slide_in_up,
            R.anim.anim_slide_out_up);
        setContentView(R.layout.activity_activation_tutorial);
        PagerAdapter pagerAdapter = new TutorialSlidePagerAdapter(getSupportFragmentManager());
        viewPager = (ViewPager) findViewById(R.id.view_pager_pages);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
            }

            @Override
            public void onPageSelected(int position)
            {
                updateButtonVisibility();
                addDots();
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {

            }
        });
        //viewPager.setOnTouchListener((v, event) -> true);

        if (getIntent().getExtras() != null)
        {
            mIsFromMenu = getIntent().getExtras().getBoolean(EXTRA_TUTORIAL, false);
        }
        setButtons();
        addDots();
    }

    private void setButtons()
    {
        btnNext = findViewById(R.id.btn_next);
        btnEnd = findViewById(R.id.btn_end);
        btnEnd.setOnClickListener(v -> onEndSelected());
        btnNext.setOnClickListener(v -> onNextSelected());
    }

    public void addDots()
    {
        LinearLayout dotsLayout = (LinearLayout) findViewById(R.id.ll_dots_container);
        dotsLayout.removeAllViews();
        for (int i = 0; i < textID.length; i++)
        {
            ImageView dot = new ImageView(getApplicationContext());
            if (i != viewPager.getCurrentItem())
            {
                dot.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.dot_unselected));
            }
            else
            {
                dot.setImageDrawable(ContextCompat.getDrawable(getApplicationContext(), R.drawable.dot_selected));
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            int dp = (int) (getResources().getDimension(R.dimen.dot_margin) / getResources().getDisplayMetrics().density);
            params.setMargins(dp, dp, dp, dp);
            dotsLayout.addView(dot, params);
        }
    }

    private void updateButtonVisibility()
    {
        if ((textID.length - 1) == viewPager.getCurrentItem())
        {
            btnEnd.setVisibility(View.VISIBLE);
            btnNext.setVisibility(View.GONE);
        }
        else
        {
            btnEnd.setVisibility(View.GONE);
            btnNext.setVisibility(View.VISIBLE);
        }
    }

    private class TutorialSlidePagerAdapter extends FragmentStatePagerAdapter
    {
        private List<FragmentTutorial> listFragments;

        public TutorialSlidePagerAdapter(FragmentManager fm)
        {
            super(fm);
            listFragments = new LinkedList<>();
            for (int i = 0; i < textID.length; i++)
            {
                listFragments.add(FragmentTutorial.newInstance(textID[i].drawableId, i, textID.length, textID[i].mDetailsIsVisible));
            }
        }

        @Override
        public Fragment getItem(int position)
        {
            return listFragments.get(position);
        }

        @Override
        public int getCount()
        {
            return listFragments.size();
        }

    }

    @Override
    public void onNextSelected()
    {
        viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
    }

    @Override
    public void onEndSelected()
    {

        if (mIsFromMenu)
        {
            finish();
        }
        else
        {
            Intent intent
                = ActivityActivationSmsWaiter.getInstance(getApplicationContext());
            startActivity(intent);
        }
    }

    private class tutorialPage
    {
        final int drawableId;
        boolean mDetailsIsVisible;

        tutorialPage(int drawableId)
        {
            this.drawableId = drawableId;
            mDetailsIsVisible = false;
        }

        tutorialPage(int drawableId, boolean detailsIsVisible)
        {
            this(drawableId);
            mDetailsIsVisible = detailsIsVisible;
        }
    }

    @Override
    public void finish()
    {
        super.finish();
    }
}
