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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.Utils;

/**
 * @author F
 */
public class OLPKeyboard extends LinearLayout
{
    private static final int MAX_FIELD_LENGTH = 200;
    private EditText mNumberField;
    private IOnKeyboardCallback mCallback;
    private View mCallFab;
    private View mCloseBtn;

    public OLPKeyboard(final Context context)
    {
        super(context);
    }

    public OLPKeyboard(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
    }

    public OLPKeyboard(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFinishInflate()
    {
        super.onFinishInflate();
        init();
    }

    private void init()
    {
        mCallFab = findViewById(R.id.ctrl_keyboard_callfab);
        mCloseBtn = findViewById(R.id.ctrl_keyboard_close_btn);
        final View backspaceBtn = findViewById(R.id.ctrl_keyboard_backspace);
        backspaceBtn.setOnClickListener(this::onBackspacePressed);
        backspaceBtn.setOnLongClickListener(this::onBackspaceLongPressed);
        mNumberField = (EditText) findViewById(R.id.ctrl_keyboard_text_field);
        addCallbackForChildrenInternal(this, this::onNumberKeyPressed);
        setOnClickListener(v -> { // catches background click that would otherwise close the keyboard
        });
        handleKeyboardMode();
    }

    private void setSelection(final int pos)
    {
        mNumberField.setSelection(pos > MAX_FIELD_LENGTH ? MAX_FIELD_LENGTH : pos);
    }

    private void handleKeyboardMode()
    {
        if (Utils.hasHardwareKeyboard(getResources()))
        {
            mNumberField.requestFocus();
            mNumberField.setOnEditorActionListener(this::handleHardwareKeyAction);
            mNumberField.addTextChangedListener(new OnHardwareKeyboardTextChanged());
        }
    }

    private boolean handleHardwareKeyAction(final TextView textView, final int actionId, final KeyEvent keyEvent)
    {
        switch (keyEvent.getKeyCode())
        {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                onCallButton(null);
                return true;
        }
        return false;
    }

    private void onNumberKeyPressed(final String key)
    {
        CharSequence value = mNumberField.getText();
        final int selStart = mNumberField.getSelectionStart();
        final int selEnd = mNumberField.getSelectionEnd();
        final CharSequence firstPart = value.subSequence(0, selStart);
        final CharSequence lastPart = value.subSequence(selEnd, value.length());
        final String finalValue = firstPart + key + lastPart;
        mNumberField.setText(finalValue);
        setSelection(selStart + key.length());
        mCallback.onTextChanged(finalValue);
    }

    private void onBackspacePressed(final View view)
    {
        CharSequence value = mNumberField.getText();
        int selStart = mNumberField.getSelectionStart();
        int selEnd = mNumberField.getSelectionEnd();
        if (selStart == selEnd)
        {
            if (selStart == 0)
            {
                return;
            }
            --selStart;
        }

        final CharSequence firstPart = value.subSequence(0, selStart);
        final CharSequence lastPart = value.subSequence(selEnd, value.length());
        final String finalValue = "" + firstPart + lastPart;
        mNumberField.setText(finalValue);
        setSelection(selStart);
        mCallback.onTextChanged(finalValue);
    }

    // clear field on longPressing backspace
    private boolean onBackspaceLongPressed(final View view)
    {
        mNumberField.setText("");
        mCallback.onTextChanged("");
        return true;
    }

    public void addCallback(final IOnKeyboardCallback callback)
    {
        mCallback = callback;
        if (mCallFab != null)
        {
            mCallFab.setOnClickListener(this::onCallButton);
        }
        if (mCloseBtn != null)
        {
            mCloseBtn.setOnClickListener(v -> mCallback.onCloseRequested());
        }
    }

    private void onCallButton(final View view)
    {
        mCallback.onCallPressed(mNumberField.getText().toString());
    }
    public void setNumberFieldVisible(boolean show)
    {
        if (show)
        {

            mNumberField.animate()
                    .alpha(1.0f)
                    .setDuration(500)
                    .start();

        }
        else
        {


            mNumberField.animate()
                    .alpha(0.0f)
                    .setDuration(500)
                    .start();


        }
    }

    private void addCallbackForChildrenInternal(final ViewGroup parent, final OLPKeyboardKey.IOnKeyPressed onKeyPressed)
    {
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; ++i)
        {
            final View v = parent.getChildAt(i);
            if (v instanceof OLPKeyboardKey)
            {
                ((OLPKeyboardKey) v).addCallback(onKeyPressed);
            }
            else if (v instanceof ViewGroup)
            {
                addCallbackForChildrenInternal((ViewGroup) v, onKeyPressed);
            }
        }
    }

    public interface IOnKeyboardCallback
    {
        void onTextChanged(final String text);

        void onCallPressed(final String text);

        void onCloseRequested();
    }

    private class OnHardwareKeyboardTextChanged implements TextWatcher
    {
        @Override
        public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after)
        {

        }

        @Override
        public void onTextChanged(final CharSequence s, final int start, final int before, final int count)
        {

        }

        @Override
        public void afterTextChanged(final Editable s)
        {
            mCallback.onTextChanged(s.toString());
        }
    }
}
