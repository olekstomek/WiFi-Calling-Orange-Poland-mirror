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
import android.content.res.TypedArray;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.RelativeLayout;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;

/**
 * @author F
 */
public class OLPKeyboardKey extends RelativeLayout
{
    private TextView mMainKey;
    private TextView mAdditionalKey;
    private boolean mHandleLongPress;
    private int mTextColor;

    public OLPKeyboardKey(final Context context)
    {
        super(context);
        init(context, null);
    }

    public OLPKeyboardKey(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        init(context, attrs);
    }

    public OLPKeyboardKey(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(final Context context, final AttributeSet attrs)
    {
        LayoutInflater.from(context).inflate(R.layout.ctrl_keyboard_key, this, true);

        mMainKey = (TextView) findViewById(R.id.ctrl_key_main);
        mAdditionalKey = (TextView) findViewById(R.id.ctrl_key_additional);
        if (attrs != null)
        {
            final TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.OLPKeyboardKey);

            mMainKey.setText(ta.getText(R.styleable.OLPKeyboardKey_kkMainText));
            mAdditionalKey.setText(ta.getText(R.styleable.OLPKeyboardKey_kkAdditionalText));
            mHandleLongPress = ta.getBoolean(R.styleable.OLPKeyboardKey_kkHandleLongpress, false);
            mTextColor = ta.getColor(R.styleable.OLPKeyboardKey_kkColor, ResourcesCompat.getColor(getResources(), R.color.black, null));
            ta.recycle();
        }
        mMainKey.setTextColor(mTextColor);
        mAdditionalKey.setTextColor(mTextColor);
    }

    public void addCallback(final IOnKeyPressed callback)
    {
        setOnClickListener(v -> callback.onNumberKeyPressed(mMainKey.getText().toString()));
        if (mHandleLongPress)
        {
            setOnLongClickListener(v -> {
                callback.onNumberKeyPressed(mAdditionalKey.getText().toString());
                return true;
            });
        }
    }

    public interface IOnKeyPressed
    {
        void onNumberKeyPressed(final String mainKey);

//        void onSpecialKeyPressed(final int whatever); // TODO do we need that?
    }
}
