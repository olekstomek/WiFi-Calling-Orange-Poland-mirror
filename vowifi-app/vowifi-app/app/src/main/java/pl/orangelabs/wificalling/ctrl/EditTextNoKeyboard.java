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

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.v7.widget.AppCompatEditText;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.util.flavoured.FontHandler;


/**
 * Created by Cookie on 2015-01-28.
 */
public class EditTextNoKeyboard extends AppCompatEditText
{

    public EditTextNoKeyboard(Context context)
    {
        super(context);
        init();
    }


    public EditTextNoKeyboard(Context context, AttributeSet attrs, int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }


    public EditTextNoKeyboard(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    private void init()
    {
        FontHandler.setFont(this, true);
    }

    @Override
    protected void onFocusChanged(final boolean focused, final int direction, final Rect previouslyFocusedRect)
    {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        ((InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    public void setOnClickListener(final OnClickListener l)
    {
        super.setOnClickListener(l);
        ((InputMethodManager) getContext().getSystemService(Activity.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(this.getWindowToken(), 0);
    }

    @Override
    public boolean onTouchEvent(@NonNull final MotionEvent event)
    {
        final boolean ret = super.onTouchEvent(event);
        final InputMethodManager imm = ((InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
        try
        {
            imm.hideSoftInputFromWindow(this.getWindowToken(), 0);
        }
        catch (Exception e)
        {
            Log.e(this, "", e);
        }

        return ret;
    }
}
