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
import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.Utils;

/**
 * @author F
 */
public class AnimatedHeader extends FrameLayout
{
    private TextView mHeaderTop;
    private TextView mHeaderTopAnim;
    private TextView mHeaderBottom;
    private TextView mHeaderBottomAnim;

    public AnimatedHeader(final Context context)
    {
        super(context);
        init(context);
    }

    public AnimatedHeader(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public AnimatedHeader(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(final Context context)
    {
        final View inflated = LayoutInflater.from(context).inflate(R.layout.ctrl_animated_header, this, false);
        mHeaderTop = (TextView) inflated.findViewById(R.id.animated_header_top);
        mHeaderTopAnim = (TextView) inflated.findViewById(R.id.animated_header_top_anim);
        mHeaderBottom = (TextView) inflated.findViewById(R.id.animated_header_bottom);
        mHeaderBottomAnim = (TextView) inflated.findViewById(R.id.animated_header_bottom_anim);
        addView(inflated);
    }

    public void updateText(final String textTop, String textBottom, @Nullable final NavDirection direction)
    {
        if (direction == null) // no anim
        {
            mHeaderTop.setText(textTop);
            mHeaderBottom.setText(textBottom);
            return;
        }

        float transDist = Utils.convertDpToPixels(getResources(), 200.0f) * direction.toMultiplier();
        mHeaderBottomAnim.setText(textBottom);
        mHeaderBottomAnim.setAlpha(0.0f);
        mHeaderBottomAnim.setTranslationX(transDist);
        mHeaderTopAnim.setText(textTop);
        mHeaderTopAnim.setAlpha(0.0f);
        mHeaderTopAnim.setTranslationX(transDist);

        mHeaderBottom.setAlpha(1.0f);
        mHeaderBottom.setTranslationX(0.0f);
        mHeaderBottom.animate().alpha(0.0f).translationX(-transDist).setDuration(500L).setStartDelay(250L)
            .setInterpolator(new DecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter(textBottom, mHeaderBottom, mHeaderBottomAnim)).start();

        mHeaderTop.setAlpha(1.0f);
        mHeaderTop.setTranslationX(0.0f);
        mHeaderTop.animate().alpha(0.0f).translationX(-transDist).setDuration(550L).setStartDelay(50L)
            .setInterpolator(new DecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter(textTop, mHeaderTop, mHeaderTopAnim)).start();

        mHeaderTopAnim.setVisibility(VISIBLE);
        mHeaderBottomAnim.setVisibility(VISIBLE);
        mHeaderTopAnim.animate().alpha(1.0f).translationX(0.0f).setDuration(500L).setInterpolator(new DecelerateInterpolator()).start();
        mHeaderBottomAnim.animate().alpha(1.0f).translationX(0.0f).setDuration(500L).setStartDelay(100L).setInterpolator(new DecelerateInterpolator())
            .start();

    }

    public enum NavDirection
    {
        LEFT,
        RIGHT;

        public float toMultiplier()
        {
            return this == LEFT ? -1.0f : 1.0f;
        }
    }

    private class AnimatorListenerAdapter extends android.animation.AnimatorListenerAdapter
    {
        private final String mText;
        private final TextView mHeader;
        private final View mHeaderAnim;

        public AnimatorListenerAdapter(final String text, final TextView header, final View headerAnim)
        {
            mText = text;
            mHeader = header;
            mHeaderAnim = headerAnim;
        }

        @Override
        public void onAnimationEnd(final Animator animation)
        {
            mHeader.setText(mText);
            mHeader.setTranslationX(0.0f);
            mHeader.setAlpha(1.0f);
            mHeaderAnim.setVisibility(INVISIBLE);
        }

        @Override
        public void onAnimationCancel(final Animator animation)
        {
            mHeader.setText(mText);
            mHeader.setTranslationX(0.0f);
            mHeader.setAlpha(1.0f);
            mHeaderAnim.setVisibility(INVISIBLE);
        }
    }
}
