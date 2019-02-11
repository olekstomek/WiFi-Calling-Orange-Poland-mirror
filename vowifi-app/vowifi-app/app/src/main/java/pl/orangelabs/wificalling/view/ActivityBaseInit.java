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

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import java.util.concurrent.atomic.AtomicBoolean;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.Utils;

/**
 * Created by marcin on 09.12.16.
 */

public abstract class ActivityBaseInit extends ActivityBase
{
    private static final long MINIMAL_SPLASH_TIME = 2000L;
    private View mViewAppLogo;
    private View mViewBottomFrame;
    private View mViewSplashImage;
    private TextView mViewTitle;
    private final AtomicBoolean mUserControlsRequested = new AtomicBoolean(false);
    protected WaitingThread mWaitingThread;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        mViewAppLogo = findViewById(R.id.activity_init_app_logo);
        mViewAppLogo.setVisibility(View.INVISIBLE); // invisible to make sure they're already measured when we need to animate them
        mViewBottomFrame = findViewById(R.id.activity_init_bottom_frame);
        mViewSplashImage = findViewById(R.id.activity_init_splash_image);
        mViewTitle = (TextView) findViewById(R.id.activity_init_title);
        createTextViewDefaultTitle(mViewTitle);
        mViewBottomFrame.setVisibility(View.INVISIBLE);
        mWaitingThread = new WaitingThread();
        mWaitingThread.start();
    }
    protected void transitionUserControlsDelayed()
    {
        if (mUserControlsRequested.getAndSet(true))
        {
            return;
        }

        final Handler handler = new Handler();
        new Thread()
        {
            @Override
            public void run()
            {
                try
                {
                    mWaitingThread.join();
                }
                catch (final InterruptedException ignored)
                {
                }
                handler.post(ActivityBaseInit.this::transitionUserControls);
            }
        }.start();
    }
    private void transitionUserControls()
    {
        final WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(displayMetrics);

        final View rootView = findViewById(R.id.root);
        final float screenHeight = rootView.getMeasuredHeight();

        final long duration = 800L;

        final float bottomFrameHeight = Math.min(screenHeight / 2, Utils.convertDpToPixels(displayMetrics, 300));
        final float availableBackgroundSpace = screenHeight - bottomFrameHeight;
        final float appLogoPos = availableBackgroundSpace - mViewAppLogo.getMeasuredHeight() / 2.0f;
        final ViewGroup.LayoutParams bottomFrameLp = mViewBottomFrame.getLayoutParams();
        bottomFrameLp.height = (int) bottomFrameHeight;
        mViewBottomFrame.setLayoutParams(bottomFrameLp);

        int[] titlePos = new int[2];
        int[] splashPos = new int[2];
        mViewTitle.getLocationOnScreen(titlePos);
        mViewSplashImage.getLocationOnScreen(splashPos);

        mViewTitle.animate().translationY(-titlePos[1] - mViewTitle.getMeasuredHeight()).setInterpolator(new AccelerateInterpolator(2.0f))
                .setDuration(duration / 2L).start();

        mViewSplashImage.animate().translationY(-splashPos[1] + availableBackgroundSpace / 2 - mViewSplashImage.getMeasuredHeight() / 2)
                .setInterpolator(new AccelerateDecelerateInterpolator()).setDuration(duration / 2L).start();

        mViewBottomFrame.setVisibility(View.VISIBLE);
        mViewBottomFrame.setTranslationY(displayMetrics.heightPixels - bottomFrameHeight);
        mViewBottomFrame.animate().translationY(0.0f).setInterpolator(new DecelerateInterpolator(2.0f)).setDuration(duration).start();

        mViewAppLogo.setVisibility(View.VISIBLE);
        mViewAppLogo.setTranslationY(displayMetrics.heightPixels);
        mViewAppLogo.animate().translationY(appLogoPos).setInterpolator(new DecelerateInterpolator(2.0f)).setDuration(duration + 200L).start();
    }
    protected static class WaitingThread extends Thread
    {
        @Override
        public void run()
        {
            try
            {
                Thread.sleep(MINIMAL_SPLASH_TIME);
            }
            catch (InterruptedException ignored)
            {
            }
        }
    }

}
