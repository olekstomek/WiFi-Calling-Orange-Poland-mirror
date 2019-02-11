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

package pl.orangelabs.wificalling.content.holders;

import android.graphics.Matrix;
import android.util.DisplayMetrics;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.VHBase;

/**
 * @author F
 */
public class DividerVH extends VHBase
{
    private static final float PARALLAX = 2.5f;
    public final TextView mHeader;
    public final ImageView mImage;
    private final DisplayMetrics mCurrentDisplayMetrics;

    public DividerVH(final ViewGroup v, final DisplayMetrics currentDisplayMetrics)
    {
        super(v, R.layout.recspec_divider);
        mCurrentDisplayMetrics = currentDisplayMetrics;
        mHeader = (TextView) itemView.findViewById(R.id.recspec_divider_text);
        mImage = (ImageView) itemView.findViewById(R.id.recspec_divider_icon);
    }

    @Override
    public void handleParallaxScroll()
    {
        calculateAndMove();
    }

    private void calculateAndMove()
    {

        int[] itemPosition = new int[2];
        itemView.getLocationOnScreen(itemPosition);

        float distanceFromCenter = mCurrentDisplayMetrics.heightPixels / 2 - itemPosition[1];
        int imageHeight = mImage.getDrawable().getIntrinsicHeight();

        float scale = recomputeImageMatrix();
        imageHeight *= scale;
        float difference = imageHeight - itemView.getMeasuredHeight();
        float move = (distanceFromCenter / mCurrentDisplayMetrics.heightPixels) * difference * PARALLAX;
        moveTo((move / 2) - (difference / 2), scale);
    }

    private float recomputeImageMatrix()
    {
        float scale;
        final int viewWidth = mImage.getWidth() - mImage.getPaddingLeft() - mImage.getPaddingRight();
        final int viewHeight = mImage.getHeight() - mImage.getPaddingTop() - mImage.getPaddingBottom();
        final int drawableWidth = mImage.getDrawable().getIntrinsicWidth();
        final int drawableHeight = mImage.getDrawable().getIntrinsicHeight();
        if (drawableWidth * viewHeight > drawableHeight * viewWidth)
        {
            scale = (float) viewHeight / (float) drawableHeight;
        }
        else
        {
            scale = (float) viewWidth / (float) drawableWidth;
        }
        return scale;
    }

    private void moveTo(float move, final float scale)
    {
        Matrix imageMatrix = mImage.getImageMatrix();
        if (Float.compare(scale, 1.0f) != 0)
        {
            imageMatrix.setScale(scale, scale);
        }
        float[] matrixValues = new float[9];
        imageMatrix.getValues(matrixValues);
        float current = matrixValues[Matrix.MTRANS_Y];
        imageMatrix.postTranslate(0, move - current);
        mImage.setImageMatrix(imageMatrix);
        mImage.invalidate();
    }
}
