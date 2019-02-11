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

package pl.orangelabs.wificalling.ctrl;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.Utils;

/**
 * our impl of FAB that allows morphing into menu sheet as per material guidelines; uses reveal anim on 5.0+, but skips it on older devices; it
 * currently uses hardcoded menu, but it could probably be easily extended to hold anything in the menu
 *
 * @author F
 */
public class MOFAB extends FrameLayout
{
    private static final boolean USE_BETTER_ANIMS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
    private static final int COLOR_FRAME_DIM = 0x60000000;
    private boolean mSheetDisplayed;
    private View mSheet;
    private FloatingActionButton mFab;
    private OnClickListener mBackgroundPressedListener;
    private OnClickListener mFabPressedListener;
    private IMOFABCallback mCallback;
    private OLPKeyboard mKeyboard;
    private FloatingActionButton mCallFab;

    public MOFAB(final Context context)
    {
        super(context);
    }

    public MOFAB(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
    }

    public MOFAB(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    private void Init()
    {
        mBackgroundPressedListener = new OnBackgroundPressedListener();
        mFabPressedListener = new OnFabPressedListener();
        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFab.setOnClickListener(mFabPressedListener);
        mSheet = findViewById(R.id.mofab_sheet);
        mSheetDisplayed = false;

        mKeyboard = (OLPKeyboard) findViewById(R.id.mofab_keyboard);
        mCallFab = (FloatingActionButton) mKeyboard.findViewById(R.id.ctrl_keyboard_callfab);
//        mCallFab.hide();
    }

    public void AddCallbacks(final IMOFABCallback callback, final OLPKeyboard.IOnKeyboardCallback keyboardCallback)
    {
        mCallback = callback;
        mKeyboard.addCallback(keyboardCallback);
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        Init();
    }

    public void Show()
    {
        if (mFab != null)
        {
            mFab.show();
        }
    }

    public void Hide()
    {
        if (mFab != null)
        {
            mFab.hide();
        }
    }

    @NonNull
    private ObjectAnimator CreateAlphaAnimator(final View obj, final float startAlpha, final float endAlpha)
    {
        final ObjectAnimator animSheetAlpha = ObjectAnimator.ofFloat(obj, "alpha", startAlpha, endAlpha);
        animSheetAlpha.setDuration(30L);
        return animSheetAlpha;
    }

    @NonNull
    private ObjectAnimator CreateTransXAnimator(final View obj, final float startAlpha, final float endAlpha, final TimeInterpolator interpolator)
    {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(obj, "translationX", startAlpha, endAlpha);
        anim.setDuration(80L);
        anim.setInterpolator(interpolator);
        return anim;
    }

    @NonNull
    private ObjectAnimator CreateTransYAnimator(final View obj, final float startAlpha, final float endAlpha, final TimeInterpolator interpolator)
    {
        final ObjectAnimator anim = ObjectAnimator.ofFloat(obj, "translationY", startAlpha, endAlpha);
        anim.setDuration(80L);
        anim.setInterpolator(interpolator);
        return anim;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private Animator CreateConcealAnimator()
    {
        if (USE_BETTER_ANIMS)
        {
            final int[] fabLoc = new int[2];
            final int[] sheetLoc = new int[2];
            final float sheetDiagSize = (float) Math.sqrt(mSheet.getWidth() * mSheet.getWidth() + mSheet.getHeight() * mSheet.getHeight());
            mFab.getLocationOnScreen(fabLoc);
            mSheet.getLocationOnScreen(sheetLoc);
            return ViewAnimationUtils.createCircularReveal(mSheet,
                fabLoc[0] + mFab.getWidth() / 2 - sheetLoc[0],
                fabLoc[1] + mFab.getHeight() / 2 - sheetLoc[1],
                sheetDiagSize,
                (float) mFab.getWidth() / 2.0f);
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Nullable
    private Animator CreateRevealAnimator(final int fabTransX, final int fabTransY)
    {
        if (USE_BETTER_ANIMS)
        {
            final int[] fabLoc = new int[2];
            final int[] sheetLoc = new int[2];
            final float sheetDiagSize = (float) Math.sqrt(mSheet.getWidth() * mSheet.getWidth() + mSheet.getHeight() * mSheet.getHeight());
            mFab.getLocationOnScreen(fabLoc);
            mSheet.getLocationOnScreen(sheetLoc);
            return ViewAnimationUtils.createCircularReveal(mSheet,
                fabLoc[0] + fabTransX + mFab.getWidth() / 2 - sheetLoc[0],
                fabLoc[1] + fabTransY + mFab.getHeight() / 2 - sheetLoc[1],
                (float) mFab.getWidth() / 2.0f,
                sheetDiagSize);
        }
        return null;
    }

    private void AnimateFabToSheet()
    {
        final float fabTransX = Utils.convertDpToPixels(getResources(), -100.0f);
        final float fabTransY = Utils.convertDpToPixels(getResources(), -60.0f);

        mFab.setOnClickListener(null);

        final Animator animX = CreateTransXAnimator(mFab, 0.0f, fabTransX, new LinearInterpolator());
        final Animator animY = CreateTransYAnimator(mFab, 0.0f, fabTransY, new AccelerateInterpolator());
        final Animator animSheetAlpha = CreateAlphaAnimator(mSheet, 0.0f, 1.0f);
        final Animator animFabAlpha = CreateAlphaAnimator(mFab, 1.0f, 0.0f);

        animX.addListener(new AnimatorListenerAdapter()
        {
            @Override
            public void onAnimationEnd(final Animator animation)
            {
                mCallFab.show();
                mFab.setVisibility(GONE);
            }
        });

        mFab.setTranslationX(0.0f);
        mFab.setTranslationY(0.0f);
        mFab.setAlpha(1.0f);
        mSheet.setAlpha(0.0f);
        mSheet.setVisibility(VISIBLE);
        final Animator reveal;
        reveal = CreateRevealAnimator((int) fabTransX, (int) fabTransY);
        final AnimatorSet anims = new AnimatorSet();
        anims.playTogether(animX, animY);
        if (reveal != null)
        {
            anims.play(reveal).after(67L);
        }
        anims.play(animSheetAlpha).after(67L);
        anims.play(animFabAlpha).after(50L);

        anims.start();
        mSheetDisplayed = true;
    }

    private void AnimateSheetToFab()
    {
        final float fabTransX = Utils.convertDpToPixels(getResources(), -100.0f);
        final float fabTransY = Utils.convertDpToPixels(getResources(), -60.0f);

        setClickable(false);
        setFocusable(false);
        mFab.setOnClickListener(mFabPressedListener);

        final Animator animX = CreateTransXAnimator(mFab, fabTransX, 0.0f, new LinearInterpolator());
        final Animator animY = CreateTransYAnimator(mFab, fabTransY, 0.0f, new DecelerateInterpolator());
        final Animator animSheetAlpha = CreateAlphaAnimator(mSheet, 1.0f, 0.0f);
        final Animator animFabAlpha = CreateAlphaAnimator(mFab, 0.0f, 1.0f);

        mFab.setTranslationX(fabTransX);
        mFab.setTranslationY(fabTransY);
        mFab.setAlpha(0.0f);
        mSheet.setAlpha(1.0f);
        final Animator reveal;
        reveal = CreateConcealAnimator();

        final AnimatorSet anims = new AnimatorSet();
        anims.addListener(new OnSheetToFabAnimationFinished());
        final long revealTime;
        if (reveal != null)
        {
            revealTime = reveal.getDuration();
            anims.play(reveal);
        }
        else
        {
            revealTime = 0L;
        }
//        anims.play(animFrameShadow);
        anims.play(animSheetAlpha).after(revealTime * 3L / 4L);
        anims.play(animX).after(revealTime);
        anims.play(animY).after(revealTime);
        anims.play(animFabAlpha).after(revealTime);

        anims.start();
        mSheetDisplayed = false;
    }

    public void ShowSheet()
    {
        if (!mSheetDisplayed)
        {
            AnimateFabToSheet();
        }
    }

    public void HideSheet()
    {
        if (mSheetDisplayed)
        {
            mFab.setVisibility(VISIBLE);
            AnimateSheetToFab();
            mCallFab.hide();
        }
    }

    public boolean IsSheetVisible()
    {
        return mSheetDisplayed;
    }

    public boolean OnBackPressed()
    {
        if (mSheetDisplayed)
        {
            HideSheet();
            return true;
        }
        return false;
    }

    public interface IOnFABEntryPressed
    {
        void OnFABEntryPressed();
    }

    public interface IMOFABCallback
    {

        void OnFabExpanded();
    }

    private class OnFabPressedListener implements OnClickListener
    {
        @Override
        public void onClick(final View v)
        {
            Log.v(this, "Pressed fab->sheet listener");
            mCallback.OnFabExpanded();
            AnimateFabToSheet();
        }
    }

    private class OnBackgroundPressedListener implements OnClickListener
    {
        @Override
        public void onClick(final View v)
        {
            Log.v(this, "Pressed sheet->fab listener");
            if (mSheetDisplayed)
            {
                AnimateSheetToFab();
                mCallFab.hide();
            }
        }
    }

    private class OnSheetToFabAnimationFinished extends AnimatorListenerAdapter
    {
        @Override
        public void onAnimationEnd(final Animator animation)
        {
            mSheet.setVisibility(GONE);
        }

        @Override
        public void onAnimationCancel(final Animator animation)
        {
            mSheet.setVisibility(GONE);
        }
    }

    private class OnEntryPressedInternal implements OnClickListener
    {
        private final IOnFABEntryPressed mListener;

        public OnEntryPressedInternal(final IOnFABEntryPressed listener)
        {
            mListener = listener;
        }

        @Override
        public void onClick(final View v)
        {
            mListener.OnFABEntryPressed();
        }
    }
}
