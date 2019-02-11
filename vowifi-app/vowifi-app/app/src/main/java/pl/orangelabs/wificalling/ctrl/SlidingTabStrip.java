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

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

import pl.orangelabs.wificalling.R;

class SlidingTabStrip extends LinearLayout
{

    private static final float DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS = 0.0f;
    private static final float SELECTED_INDICATOR_THICKNESS_DIPS = 2.0f;

    private static final float DEFAULT_DIVIDER_THICKNESS_DIPS = 1.0f;
    private static final float DEFAULT_DIVIDER_HEIGHT = 0.5f;
    private static final float SEPARATOR_DIVIDER = 2.0f;
    private final float mBottomBorderThickness;
    private final Paint mBottomBorderPaint;
    private final float mSelectedIndicatorThickness;
    private final Paint mSelectedIndicatorPaint;
    private final Paint mDividerPaint;
    private final Paint mBitmapPaint;
    private final float mDividerHeight;
    private final SimpleTabColorizer mDefaultTabColorizer;
    private boolean mSeparatorDrawn;
    private int mSelectedPosition;
    private float mSelectionOffset;
    private SlidingTabLayout.ITabColorizer mCustomTabColorizer;
    private Canvas mCanvasCompositioning;
    private Bitmap mCanvasCompositioningBitmap;

    SlidingTabStrip(final Context context)
    {
        this(context, null);
    }

    SlidingTabStrip(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        setWillNotDraw(false);
        setId(R.id.sliding_tab_strip_control_id);

        final Resources resources = getResources();
        if (resources == null)
        {
            throw new RuntimeException("Could not retrieve resources on init");
        }

        final int defaultBottomBorderColor;

        final float density = resources.getDisplayMetrics().density;
        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SlidingTabLayout);
        if (a != null)
        {
            final int themeIndicatorColor = a.getColor(R.styleable.SlidingTabLayout_moTabIndicatorColor, 0);
            final int themeSeparatorColor = a.getColor(R.styleable.SlidingTabLayout_moTabSeparatorColor, 0);
            defaultBottomBorderColor = a.getColor(R.styleable.SlidingTabLayout_moTabUnderlineColor, 0);

            mDefaultTabColorizer = new SimpleTabColorizer();
            mDefaultTabColorizer.IndicatorColors(themeIndicatorColor);
            mDefaultTabColorizer.DividerColors(themeSeparatorColor);

            a.recycle();
        }
        else
        {
            defaultBottomBorderColor = 0;
            mDefaultTabColorizer = null;
        }

        if (defaultBottomBorderColor == 0)
        {
            throw new RuntimeException("SlidingTabLayout_tabUnderlineColor needs to be defined in styles");
        }

        mBottomBorderThickness = DEFAULT_BOTTOM_BORDER_THICKNESS_DIPS * density;
        mBottomBorderPaint = new Paint();
        mBottomBorderPaint.setColor(defaultBottomBorderColor);

        mSelectedIndicatorThickness = SELECTED_INDICATOR_THICKNESS_DIPS * density;
        mSelectedIndicatorPaint = new Paint();
        mSelectedIndicatorPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP));

        mDividerHeight = DEFAULT_DIVIDER_HEIGHT;
        mDividerPaint = new Paint();
        mDividerPaint.setStrokeWidth(DEFAULT_DIVIDER_THICKNESS_DIPS * density);

        mBitmapPaint = new Paint();
    }

    /**
     * Set the alpha value of the {@code color} to be the given {@code alpha} value.
     */
    private static int setColorAlpha(final int color, final int alpha)
    {
        return Color.argb(alpha, Color.red(color), Color.green(color), Color.blue(color));
    }

    /**
     * Blend {@code color1} and {@code color2} using the given ratio.
     *
     * @param ratio
     *     of which to blend. 1.0 will return {@code color1}, 0.5 will give an even blend, 0.0 will return {@code color2}.
     */
    private static int blendColors(final int color1, final int color2, final float ratio)
    {
        final float inverseRation = 1.0f - ratio;
        final float r = ((float) Color.red(color1) * ratio) + ((float) Color.red(color2) * inverseRation);
        final float g = ((float) Color.green(color1) * ratio) + ((float) Color.green(color2) * inverseRation);
        final float b = ((float) Color.blue(color1) * ratio) + ((float) Color.blue(color2) * inverseRation);
        return Color.rgb((int) r, (int) g, (int) b);
    }

    void CustomTabColorizer(final SlidingTabLayout.ITabColorizer customTabColorizer)
    {
        mCustomTabColorizer = customTabColorizer;
        invalidate();
    }

    void SelectedIndicatorColors(final int... colors)
    {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null;
        mDefaultTabColorizer.IndicatorColors(colors);
        invalidate();
    }

    void DividerColors(final int... colors)
    {
        // Make sure that the custom colorizer is removed
        mCustomTabColorizer = null;
        mDefaultTabColorizer.DividerColors(colors);
        invalidate();
    }

    void OnViewPagerPageChanged(final int position, final float positionOffset)
    {
        mSelectedPosition = position;
        mSelectionOffset = positionOffset;
        invalidate();
    }

    void UpdateBorderPaint(final int color)
    {
        mBottomBorderPaint.setColor(color);
    }

    void DrawSeparator(final boolean draw)
    {
        mSeparatorDrawn = draw;
    }

    @Override
    protected void dispatchDraw(final Canvas canvas)
    {
        final int width = getWidth();
        final int height = getHeight();
        if (width <= 0 || height <= 0)
        {
            return;
        }

        if (mCanvasCompositioning == null)
        {
            mCanvasCompositioningBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            mCanvasCompositioning = new Canvas(mCanvasCompositioningBitmap);
        }
        mCanvasCompositioning.drawColor(0, PorterDuff.Mode.CLEAR);

        super.dispatchDraw(mCanvasCompositioning);
        selfDraw(canvas); // draw indicators after drawing children (to apply compositioning correctly
    }

    protected void selfDraw(final Canvas canvas)
    {
//        super.onDraw(canvas);
        final float height = (float) getHeight();
        final int childCount = getChildCount();
        final float dividerHeightPx = Math.min(Math.max(0.0f, mDividerHeight), 1.0f) * height;
        final SlidingTabLayout.ITabColorizer tabColorizer = mCustomTabColorizer != null
                                                            ? mCustomTabColorizer
                                                            : mDefaultTabColorizer;

//        canvas.drawRect(0, 0, canvas.getWidth(), height - mBottomBorderThickness, mBackgroundPaint);

        // Thick colored underline below the current selection
        if (childCount > 0)
        {
            final View selectedEntry = getChildAt(mSelectedPosition);
            if (selectedEntry != null)
            {
                float left = (float) selectedEntry.getLeft();
                float right = (float) selectedEntry.getRight();
                int color = tabColorizer.IndicatorColor(mSelectedPosition);

                if (mSelectionOffset > 0.0f && mSelectedPosition < (getChildCount() - 1))
                {
                    final int nextColor = tabColorizer.IndicatorColor(mSelectedPosition + 1);
                    if (color != nextColor)
                    {
                        color = blendColors(nextColor, color, mSelectionOffset);
                    }

                    // Draw the selection partway between the tabs
                    final View nextTitle = getChildAt(mSelectedPosition + 1);
                    if (nextTitle != null)
                    {
                        left = (mSelectionOffset * (float) nextTitle.getLeft() + (1.0f - mSelectionOffset) * left);
                        right = (mSelectionOffset * (float) nextTitle.getRight() + (1.0f - mSelectionOffset) * right);
                    }
                }

                mSelectedIndicatorPaint.setColor(color);
                mCanvasCompositioning.drawRect(left, 0.0f, right, height, mSelectedIndicatorPaint);
                canvas.drawBitmap(mCanvasCompositioningBitmap, 0, 0, mBitmapPaint);
            }
        }

        // Thin underline along the entire bottom edge
//        canvas.drawRect(0.0f, height - mBottomBorderThickness, (float) getWidth(), height, mBottomBorderPaint);

        // Vertical separators between the titles
        if (mSeparatorDrawn)
        {
            final float separatorTop = (height - dividerHeightPx) / SEPARATOR_DIVIDER;
            for (int i = 0; i < childCount - 1; i++)
            {
                final View child = getChildAt(i);
                mDividerPaint.setColor(tabColorizer.DividerColor(i));
                if (child != null)
                {
                    canvas.drawLine((float) child.getRight(), separatorTop, (float) child.getRight(), separatorTop + dividerHeightPx, mDividerPaint);
                }
            }
        }
    }

    private static class SimpleTabColorizer implements SlidingTabLayout.ITabColorizer
    {
        private int[] mIndicatorColors;
        private int[] mDividerColors;

        @Override
        public final int IndicatorColor(final int position)
        {
            return mIndicatorColors[position % mIndicatorColors.length];
        }

        @Override
        public final int DividerColor(final int position)
        {
            return mDividerColors[position % mDividerColors.length];
        }

        void IndicatorColors(final int... colors)
        {
            mIndicatorColors = colors;
        }

        void DividerColors(final int... colors)
        {
            mDividerColors = colors;
        }
    }
}