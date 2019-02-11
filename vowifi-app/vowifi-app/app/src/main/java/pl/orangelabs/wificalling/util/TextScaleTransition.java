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

package pl.orangelabs.wificalling.util;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Path;
import android.os.Build;
import android.transition.Transition;
import android.transition.TransitionValues;
import android.util.AttributeSet;
import android.util.Property;
import android.view.View;
import android.view.ViewGroup;

/**
 * originally from https://stackoverflow.com/a/26813670 , logic heavily changed
 */
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class TextScaleTransition extends Transition
{
    private static final String PROP_NAME_SCALE_X = "vw:transition:scaleX";
    private static final String PROP_NAME_SCALE_Y = "vw:transition:scaleY";
    private static final String[] TRANSITION_PROPERTIES = {
            PROP_NAME_SCALE_X,
            PROP_NAME_SCALE_Y
    };

    private static final Property<View, Float> SCALE_X_PROPERTY =
        new Property<View, Float>(Float.class, "scaleX")
        {
            @Override
            public Float get(View v)
            {
                return v.getScaleX();
            }

            @Override
            public void set(View v, Float scale)
            {
                v.setScaleX(scale);
            }
        };
    private static final Property<View, Float> SCALE_Y_PROPERTY =
        new Property<View, Float>(Float.class, "scaleY")
        {
            @Override
            public Float get(View v)
            {
                return v.getScaleY();
            }

            @Override
            public void set(View v, Float scale)
            {
                v.setScaleY(scale);
            }
        };

    public TextScaleTransition()
    {
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public TextScaleTransition(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    @Override
    public String[] getTransitionProperties()
    {
        return TRANSITION_PROPERTIES;
    }

    @Override
    public void captureStartValues(TransitionValues transitionValues)
    {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues)
    {
        captureValues(transitionValues);
    }

    private void captureValues(TransitionValues transitionValues)
    {
        transitionValues.values.put(PROP_NAME_SCALE_X, transitionValues.view.getScaleX());
        transitionValues.values.put(PROP_NAME_SCALE_Y, transitionValues.view.getScaleY());
    }

    @Override
    public Animator createAnimator(ViewGroup sceneRoot, TransitionValues startValues,
                                   TransitionValues endValues)
    {
        if (startValues == null || endValues == null)
        {
            return null;
        }

        Float startSize = (Float) startValues.values.get(PROP_NAME_SCALE_X);
        Float endSize = (Float) endValues.values.get(PROP_NAME_SCALE_Y);
        if (startSize == null || endSize == null || startSize.floatValue() == endSize.floatValue())
        {
            return null;
        }

        endValues.view.setScaleX(startSize);
        endValues.view.setScaleY(startSize);

        Path p = new Path();
        p.moveTo(startSize, startSize);
        p.lineTo(endSize, endSize);
        return ObjectAnimator.ofFloat(endValues.view, SCALE_X_PROPERTY, SCALE_Y_PROPERTY, p);
    }
}