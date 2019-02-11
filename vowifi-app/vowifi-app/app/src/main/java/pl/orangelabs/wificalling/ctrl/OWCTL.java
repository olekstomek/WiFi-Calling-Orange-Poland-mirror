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

import android.content.Context;
import android.os.Build;
import android.support.design.widget.AppBarLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewParent;
import android.widget.FrameLayout;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.Utils;

/**
 * @author F
 *         <p>
 *         custom implementation of collapsing toolbar layout with 2-stage collapsing (toolbar + tabs); it's mostly hardcoded for our use-case, so not
 *         really portable somewhere else
 */
public class OWCTL extends FrameLayout
{
    //    private SharedPrefs mPrefs;
    private AppBarLayout.OnOffsetChangedListener mOnOffsetChangedListener;
    private AppBarLayout.OnOffsetChangedListener mExternalOffsetChangedListener;
    private View mHeader;
    private View mToolbar;
    private View mTabs;
    private float mActionBarSize;
    private boolean mHeaderVisible;
    private boolean mNoTabsMode;

    public OWCTL(final Context context)
    {
        super(context);
        init(context, null);
    }

    public OWCTL(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public OWCTL(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private static ViewOffsetHelper getViewOffsetHelper(View view)
    {
        ViewOffsetHelper offsetHelper = (ViewOffsetHelper) view.getTag(android.support.design.R.id.view_offset_helper);
        if (offsetHelper == null)
        {
            offsetHelper = new ViewOffsetHelper(view);
            view.setTag(android.support.design.R.id.view_offset_helper, offsetHelper);
        }

        return offsetHelper;
    }

    private void init(final Context context, final AttributeSet attrs)
    {
//        mPrefs = new SharedPrefs(context);
        mHeaderVisible = true;
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        mHeader = findViewById(R.id.main_header);
        mToolbar = findViewById(R.id.toolbar);
        mTabs = findViewById(R.id.tabs);

        mActionBarSize = Utils.getThemedDimension(getContext(), R.attr.actionBarSize);
        setMinimumHeight((int) mActionBarSize);
        setBackgroundColor(ResourcesCompat.getColor(getResources(), R.color.bgAccented, null));

        ViewParent parent = getParent();
        mOnOffsetChangedListener = new OnOffsetChangedListener();
        ((AppBarLayout) parent).addOnOffsetChangedListener(mOnOffsetChangedListener);
    }

    private void handleScroll(final AppBarLayout appBarLayout, final int offset)
    {
        final ViewOffsetHelper headerOH = mHeader == null ? null : getViewOffsetHelper(mHeader);
        final ViewOffsetHelper toolbarOH = getViewOffsetHelper(mToolbar);
        final int appbarHeight = appBarLayout.getMeasuredHeight();
        if (headerOH != null)
        {
            headerOH.setTopAndBottomOffset(-offset / 2);
        }

        if (mNoTabsMode)
        {
            toolbarOH.setTopAndBottomOffset(-offset);
        }
        else
        {
            if (-offset >= appbarHeight - mActionBarSize * 2)
            {
                int diff = (int) (-offset - (appbarHeight - mActionBarSize * 2));
                toolbarOH.setTopAndBottomOffset(-offset - diff);
            }
            else
            {
                toolbarOH.setTopAndBottomOffset(-offset);
            }
        }

        if (mHeader != null) // fade in/out header -- this is equal to CTL's scrim showing/hiding
        {
            if (mHeaderVisible && -offset >= mActionBarSize)
            {
                mHeaderVisible = false;
                mHeader.animate().alpha(0.0f).setDuration(200L).start();
            }
            else if (!mHeaderVisible && -offset < mActionBarSize)
            {
                mHeaderVisible = true;
                mHeader.animate().alpha(1.0f).setDuration(200L).start();
            }
        }
    }

    public void addExternalOffsetListener(final AppBarLayout.OnOffsetChangedListener listener)
    {
        mExternalOffsetChangedListener = listener;
    }

    public void changeNoTabsMode(final boolean noTabs)
    {
        mNoTabsMode = noTabs;
//        postInvalidate();
    }

    protected void onDetachedFromWindow()
    {
        ViewParent parent = getParent();
        if (mOnOffsetChangedListener != null && parent instanceof AppBarLayout)
        {
            ((AppBarLayout) parent).removeOnOffsetChangedListener(mOnOffsetChangedListener);
        }

        super.onDetachedFromWindow();
    }

    // taken from private android class
    private static class ViewOffsetHelper
    {
        private final View mView;
        private int mLayoutTop;
        private int mLayoutLeft;
        private int mOffsetTop;
        private int mOffsetLeft;

        public ViewOffsetHelper(View view)
        {
            this.mView = view;
        }

        private static void tickleInvalidationFlag(View view)
        {
            float x = ViewCompat.getTranslationX(view);
            ViewCompat.setTranslationX(view, x + 1.0F);
            ViewCompat.setTranslationX(view, x);
        }

        public void onViewLayout()
        {
            this.mLayoutTop = this.mView.getTop();
            this.mLayoutLeft = this.mView.getLeft();
            this.updateOffsets();
        }

        private void updateOffsets()
        {
            ViewCompat.offsetTopAndBottom(this.mView, this.mOffsetTop - (this.mView.getTop() - this.mLayoutTop));
            ViewCompat.offsetLeftAndRight(this.mView, this.mOffsetLeft - (this.mView.getLeft() - this.mLayoutLeft));
            if (Build.VERSION.SDK_INT < 23)
            {
                tickleInvalidationFlag(this.mView);
                ViewParent vp = this.mView.getParent();
                if (vp instanceof View)
                {
                    tickleInvalidationFlag((View) vp);
                }
            }

        }

        public boolean setTopAndBottomOffset(int offset)
        {
            if (this.mOffsetTop != offset)
            {
                this.mOffsetTop = offset;
                this.updateOffsets();
                return true;
            }
            else
            {
                return false;
            }
        }

        public int getTopAndBottomOffset()
        {
            return this.mOffsetTop;
        }
    }

    private class OnOffsetChangedListener implements AppBarLayout.OnOffsetChangedListener
    {
        @Override
        public void onOffsetChanged(final AppBarLayout appBarLayout, final int verticalOffset)
        {
            handleScroll(appBarLayout, verticalOffset);
            if (mExternalOffsetChangedListener != null)
            {
                mExternalOffsetChangedListener.onOffsetChanged(appBarLayout, verticalOffset);
            }
        }
    }
}
