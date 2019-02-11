/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pl.orangelabs.wificalling.ctrl;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Build;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.OLPPagerAdapter;
import pl.orangelabs.wificalling.util.Utils;

/**
 * To be used with ViewPager to provide a tab indicator component which give constant feedback as to the user's scroll progress.
 * <p>
 * To use the component, simply add it to your view hierarchy. Then in your {@link android.app.Activity} or {@link android.support.v4.app.Fragment}
 * call {@link #ViewPager(android.support.v4.view.ViewPager)} providing it the ViewPager this layout is being used for.
 * <p>
 * The colors can be customized in two ways. The first and simplest is to provide an array of colors via {@link #SelectedIndicatorColors(int...)} and
 * {@link #DividerColors(int...)}. The alternative is via the {@link SlidingTabLayout.ITabColorizer} interface which provides you complete control
 * over which color is used for any individual position.
 * <p>
 * The views used as tabs can be customized by calling {@link #CustomTabView(int, int)}, providing the layout ID of your custom layout.
 */
public class SlidingTabLayout extends HorizontalScrollView
{
    private static final float TITLE_OFFSET_DIPS = 24.0f;
    private static final float TAB_VIEW_PADDING_DIPS = 16.0f;
    private static final float TAB_VIEW_TEXT_SIZE_SP = 12.0f;
    private final SlidingTabStrip mTabStrip;
    private final int mTitleOffset;
    private final int mTabBackgroundResourceId;
    private final ColorStateList mTabTextColor;
    private int mTabViewLayoutId;
    private int mTabViewTextViewId;
    private ViewPager mViewPager;
    private ViewPager.OnPageChangeListener mViewPagerPageChangeListener;
    private boolean mDistributeEvenly;

    public SlidingTabLayout(final Context context)
    {
        this(context, null);
    }

    public SlidingTabLayout(final Context context, final AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    @SuppressLint("SetTextI18n")
    public SlidingTabLayout(final Context context, final AttributeSet attrs, final int defStyle)
    {
        super(context, attrs, defStyle);

        // Disable the Scroll Bar
        setHorizontalScrollBarEnabled(false);
        // Make sure that the Tab Strips fills this View
        setFillViewport(true);

        final Resources resources = getResources();
        if (resources != null)
        {
            mTitleOffset = (int) (TITLE_OFFSET_DIPS * resources.getDisplayMetrics().density);
        }
        else
        {
            mTitleOffset = 0;
        }


        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingTabLayout);
        if (a != null)
        {
            mTabBackgroundResourceId = a.getResourceId(R.styleable.SlidingTabLayout_moTabBackgroundColor, 0);
            mTabTextColor = a.getColorStateList(R.styleable.SlidingTabLayout_moTabTextColor);

            a.recycle();
        }
        else
        {
            mTabBackgroundResourceId = 0;
            mTabTextColor = null;
        }

        if (mTabBackgroundResourceId == 0)
        {
            throw new RuntimeException("SlidingTabLayout_tabBackgroundColor has to be defined in styles");
        }

        if (mTabTextColor == null)
        {
            throw new RuntimeException("SlidingTabLayout_tabTextColor has to be defined in styles");
        }

        mTabStrip = new SlidingTabStrip(context, attrs);
        addView(mTabStrip, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        if (isInEditMode())
        {
            final TextView textView = CreateDefaultTabView(context);
            textView.setText("dbg tab");
            mTabStrip.addView(textView);
            final TextView textView2 = CreateDefaultTabView(context);
            textView2.setText("dbg tab 2");
            mTabStrip.addView(textView2);
        }
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void CreateTabTextViewStyle(final TextView textView)
    {
        textView.setAllCaps(true);
    }

    /**
     * Set the custom {@link SlidingTabLayout.ITabColorizer} to be used.
     * <p>
     * If you only require simple custmisation then you can use {@link #SelectedIndicatorColors(int...)} and {@link #DividerColors(int...)} to achieve
     * similar effects.
     */
    public void CustomTabColorizer(final ITabColorizer tabColorizer)
    {
        mTabStrip.CustomTabColorizer(tabColorizer);
    }

    /**
     * Sets the colors to be used for indicating the selected tab. These colors are treated as a circular array. Providing one color will mean that
     * all tabs are indicated with the same color.
     */
    public void SelectedIndicatorColors(final int... colors)
    {
        mTabStrip.SelectedIndicatorColors(colors);
    }

    /**
     * Sets the colors to be used for tab dividers. These colors are treated as a circular array. Providing one color will mean that all tabs are
     * indicated with the same color.
     */
    public void DividerColors(final int... colors)
    {
        mTabStrip.DividerColors(colors);
    }

    public void UpdateBorderPaint(final int color)
    {
        mTabStrip.UpdateBorderPaint(color);
    }

    public void DrawSeparator(final boolean draw)
    {
        mTabStrip.DrawSeparator(draw);
    }

    public void DistributeEvenly()
    {
        mDistributeEvenly = true;
    }

    /**
     * Set the {@link android.support.v4.view.ViewPager.OnPageChangeListener}. When using {@link SlidingTabLayout} you are required to set any {@link
     * android.support.v4.view.ViewPager.OnPageChangeListener} through this method. This is so that the layout can update it's scroll position
     * correctly.
     *
     * @see android.support.v4.view.ViewPager#setOnPageChangeListener(android.support.v4.view.ViewPager.OnPageChangeListener)
     */
    public void SetOnPageChangeListener(final ViewPager.OnPageChangeListener listener)
    {
        mViewPagerPageChangeListener = listener;
    }

    /**
     * Set the custom layout to be inflated for the tab views.
     *
     * @param layoutResId
     *     Layout id to be inflated
     * @param textViewId
     *     id of the {@link TextView} in the inflated view
     */
    public void CustomTabView(final int layoutResId, final int textViewId)
    {
        mTabViewLayoutId = layoutResId;
        mTabViewTextViewId = textViewId;
    }

    /**
     * Sets the associated view pager. Note that the assumption here is that the pager content (number of tabs and tab titles) does not change after
     * this call has been made.
     */
    public void ViewPager(final ViewPager viewPager)
    {
        mTabStrip.removeAllViews();

        mViewPager = viewPager;
        if (viewPager != null)
        {
            viewPager.addOnPageChangeListener(new InternalViewPagerListener());
            PopulateTabStrip();
        }
    }

    /**
     * Create a default view to be used for tabs. This is called if a custom tab view is not set via {@link #CustomTabView(int, int)}.
     */
    protected TextView CreateDefaultTabView(final Context context)
    {
        final TextView textView = new CustomFontTextView(context);
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
        textView.setTypeface(Typeface.DEFAULT_BOLD);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        textView.setMinHeight((int) Utils.convertDpToPixels(getResources(), 50.0f));
        textView.setLayoutParams(layoutParams);

//        ColoringUtil.Me().SetSelectableViewBackground(context, textView);
        textView.setBackgroundResource(mTabBackgroundResourceId);

        final Resources resources = getResources();
        if (resources != null)
        {
            final int padding = (int) (TAB_VIEW_PADDING_DIPS * resources.getDisplayMetrics().density);
            textView.setPadding(padding, 0, padding, 0);
        }

        textView.setTextColor(mTabTextColor);

        return textView;
    }

    protected View CreateDefaultTabIconView(final Context context)
    {
        final ImageView v = new ImageView(context);
        final int padding = (int) Utils.convertDpToPixels(context.getResources(), 10);
        v.setPadding(padding, padding, padding, padding);
//        v.setGravity(Gravity.CENTER);
//        v.setTextSize(TypedValue.COMPLEX_UNIT_SP, TAB_VIEW_TEXT_SIZE_SP);
//        v.setTypeface(Typeface.DEFAULT_BOLD);
        final LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        v.setMinimumHeight((int) Utils.convertDpToPixels(getResources(), 50.0f));
        v.setLayoutParams(layoutParams);

//        ColoringUtil.Me().SetSelectableViewBackground(context, textView);
//        v.setBackgroundResource(mTabBackgroundResourceId);

//        v.setTextColor(mTabTextColor);

        return v;
    }

    private void PopulateTabStrip()
    {
        final OLPPagerAdapter adapter = (OLPPagerAdapter) mViewPager.getAdapter();
        final OnClickListener tabClickListener = new TabClickListener();

        for (int i = 0; i < adapter.getCount(); i++)
        {
            View tabView = null;
            TextView tabTitleView = null;

            if (mTabViewLayoutId != 0 && getContext() != null)
            {
                // If there is a custom tab view layout id set, try and inflate it
                tabView = LayoutInflater.from(getContext()).inflate(mTabViewLayoutId, mTabStrip, false);
                if (tabView != null)
                {
                    tabTitleView = (TextView) tabView.findViewById(mTabViewTextViewId);
                }
            }

            if (tabView == null)
            {
                tabView = CreateDefaultTabIconView(getContext());
            }

            if (tabTitleView == null && TextView.class.isInstance(tabView))
            {
                tabTitleView = (TextView) tabView;
            }

            if (mDistributeEvenly)
            {
                final LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) tabView.getLayoutParams();
                lp.width = 0;
                lp.weight = 1.0f;
            }

            if (tabView instanceof ImageView)
            {
                ((ImageView) tabView).setImageResource(adapter.getIcon(i));
            }
            else if (tabTitleView != null)
            {
//                tabTitleView.set(adapter.getIcon(i));
                CreateTabTextViewStyle(tabTitleView);
            }

            if (tabView != null)
            {
                tabView.setOnClickListener(tabClickListener);
                mTabStrip.addView(tabView);
            }
        }
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if (mViewPager != null)
        {
            ScrollToTab(mViewPager.getCurrentItem(), 0);
        }
    }

    private void ScrollToTab(final int tabIndex, final int positionOffset)
    {
        final int tabStripChildCount = mTabStrip.getChildCount();
        if (tabStripChildCount == 0 || tabIndex < 0 || tabIndex >= tabStripChildCount)
        {
            return;
        }

        final View selectedChild = mTabStrip.getChildAt(tabIndex);
        if (selectedChild != null)
        {
            int targetScrollX = selectedChild.getLeft() + positionOffset;

            if (tabIndex > 0 || positionOffset > 0)
            {
                // If we're not at the first child and are mid-scroll, make sure we obey the offset
                targetScrollX -= mTitleOffset;
            }

            scrollTo(targetScrollX, 0);
        }
    }

    public void Recolor(final int color)
    {
//        for (int i = 0; i < mTabStrip.getChildCount(); ++i)
//        {
//            final View child = mTabStrip.getChildAt(i);
//            child.setBackgroundColor(color);
//        }
        setBackgroundColor(color);
    }

    /**
     * Allows complete control over the colors drawn in the tab layout.
     * <p>
     * Set with {@link #CustomTabColorizer(SlidingTabLayout.ITabColorizer)}.
     */
    public interface ITabColorizer
    {

        /**
         * @return return the color of the indicator used when {@code position} is selected.
         */
        int IndicatorColor(int position);

        /**
         * @return return the color of the divider drawn to the right of {@code position}.
         */
        int DividerColor(int position);

    }

    private class InternalViewPagerListener implements ViewPager.OnPageChangeListener
    {
        private int mScrollState;
        private View mPrevSelection;

        @Override
        public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels)
        {
            final int tabStripChildCount = mTabStrip.getChildCount();
            if ((tabStripChildCount == 0) || (position < 0) || (position >= tabStripChildCount))
            {
                return;
            }

            mTabStrip.OnViewPagerPageChanged(position, positionOffset);

            final View selectedTitle = mTabStrip.getChildAt(position);
            final int extraOffset = (selectedTitle != null)
                                    ? (int) (positionOffset * (float) selectedTitle.getWidth())
                                    : 0;
            ScrollToTab(position, extraOffset);

            FixElemActivation(positionOffset, selectedTitle);

            if (mViewPagerPageChangeListener != null)
            {
                mViewPagerPageChangeListener.onPageScrolled(position, positionOffset,
                    positionOffsetPixels);
            }
        }

        private void FixElemActivation(final float positionOffset, final View selectedTitle)
        {
            if (Float.compare(positionOffset, 0.0f) == 0 && selectedTitle != null && selectedTitle != mPrevSelection)
            {
                selectedTitle.setActivated(true);
                if (mPrevSelection != null)
                {
                    mPrevSelection.setActivated(false);
                }
                mPrevSelection = selectedTitle;
            }
        }

        @Override
        public void onPageScrollStateChanged(final int state)
        {
            mScrollState = state;

            if (mViewPagerPageChangeListener != null)
            {
                mViewPagerPageChangeListener.onPageScrollStateChanged(state);
            }
        }

        @Override
        public void onPageSelected(final int position)
        {
            if (mScrollState == ViewPager.SCROLL_STATE_IDLE)
            {
                mTabStrip.OnViewPagerPageChanged(position, 0.0f);
                ScrollToTab(position, 0);
                FixElemActivation(0.0f, mTabStrip.getChildAt(position));
            }

            if (mViewPagerPageChangeListener != null)
            {
                mViewPagerPageChangeListener.onPageSelected(position);
            }
        }

    }

    private class TabClickListener implements OnClickListener
    {
        @Override
        public void onClick(final View v)
        {
            if (!isEnabled())
            {
                return;
            }
            for (int i = 0; i < mTabStrip.getChildCount(); i++)
            {
                if (v == mTabStrip.getChildAt(i))
                {
                    mViewPager.setCurrentItem(i);
                    return;
                }
            }
        }
    }

}
