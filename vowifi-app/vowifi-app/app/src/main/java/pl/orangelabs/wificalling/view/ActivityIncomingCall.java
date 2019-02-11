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

package pl.orangelabs.wificalling.view;

import android.graphics.Color;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.provider.CallLog;
import android.support.annotation.Nullable;
import android.view.Display;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.widget.ImageView;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.sip.SipServiceCommand;


/**
 * @author Cookie
 */
public class ActivityIncomingCall extends ActivityBaseCall
{
    public int screenWidth, screeHeight;
    private float dX, dY;
    //Starting position of the phone answer button:
    private float startingX, startingY;
    private int greenRED = 50, greenGREEN = 200, greenBLUE = 50;
    private int redRED = 205, redGREEN = 60, redBLUE = 20;
    private ImageView phoneAnswerImageView;
    private View mShadows;
    private View mDots;

//    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
//    public static Transition makeEnterTransition()
//    {
//
//        return new Fade().setDuration(0);
//    }

    @Override
    void onInternalCreate(@Nullable final Bundle savedInstanceState)
    {
        setLayout(R.layout.activity_incoming_call);
        setViews();
    }


    private void setViews()
    {
        phoneAnswerImageView = (ImageView) findViewById(R.id.view_call_hung_up);
        mShadows = findViewById(R.id.view_call_hung_up_shadows);
        mDots = findViewById(R.id.view_call_hung_up_dots);
        setStartingXY(phoneAnswerImageView);
        getScreenSize();

        phoneAnswerImageView.setOnTouchListener(new halfCircleDraggableButtonTouchListener(new PhoneViewCallback()));
    }
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN
                || event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP)
        {
            SipServiceCommand.muteRing(getApplicationContext());
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
        private float[] circleLimit(float x, float y)
    {
        float buttonCircleRadius = screenWidth / 3;
        float[] coordinates = new float[2];


        double distance = Math.sqrt(Math.pow(startingX - x, 2)
                                    + Math.pow(startingY - y, 2));
        if (distance <= buttonCircleRadius)
        {
            coordinates[0] = x;
            coordinates[1] = y;

        }
        else
        {
            x = x - startingX;
            y = y - startingY;

            double radians = Math.atan2(y, x);

            coordinates[0] = (float) (Math.cos(radians) * buttonCircleRadius + startingX);
            coordinates[1] = (float) (Math.sin(radians) * buttonCircleRadius + startingY);

        }
        return coordinates;

    }

    private void setStartingXY(final View phoneAnswerImageView)
    {
        phoneAnswerImageView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                phoneAnswerImageView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int location[] = new int[2];
                phoneAnswerImageView.getLocationOnScreen(location);
                startingX = location[0];
                startingY = location[1];
            }
        });
    }

    private void getScreenSize()
    {
        WindowManager vm = (WindowManager) getSystemService(WINDOW_SERVICE);
        Display display = vm.getDefaultDisplay();

        Point size = new Point();
        display.getSize(size);
        screeHeight = size.y;
        screenWidth = size.x;
    }

    interface IPhoneButtonCallback
    {
        void OnAnswer();

        void OnReject();
    }

    private class halfCircleDraggableButtonTouchListener implements View.OnTouchListener
    {

        IPhoneButtonCallback mCallback;

        halfCircleDraggableButtonTouchListener(final IPhoneButtonCallback callback)
        {
            mCallback = callback;
        }

        @Override
        public boolean onTouch(View view, MotionEvent motionEvent)
        {
            ImageView phoneAnswerImageView = (ImageView) view;
            float circleRadius = screenWidth / 3;
            int color;
            switch (motionEvent.getAction())
            {

                case MotionEvent.ACTION_DOWN:
                    // Read change of position
                    dX = view.getX() - motionEvent.getRawX();
                    dY = view.getY() - motionEvent.getRawY();
                    AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
                    fadeOut.setFillAfter(true);
                    fadeOut.setDuration(500);
                    mShadows.startAnimation(fadeOut);
                    mDots.startAnimation(fadeOut);
//                    mShadows.setVisibility(View.GONE);
//                    mDots.setVisibility(View.GONE);
                    break;

                case MotionEvent.ACTION_MOVE:

                    float currentX = motionEvent.getRawX() + dX;
                    float currentY = motionEvent.getRawY() + dY;
                    //Make it only half-circle :
                    if (currentY > startingY)
                    {
                        currentY = startingY;
                    }
                    float distanceX;

                    if (currentX < startingX)
                    {
                        distanceX = startingX - currentX;
                    }
                    else
                    {
                        distanceX = currentX - startingX;
                    }
                    //Determine alpha in percents, so it will change dynamically:
                    int alpha = (int) (distanceX / circleRadius * 2.55 * 100);
                    if (alpha > 255)
                    {
                        alpha = 255;
                    }

                    float circleLimitCoordinates[] = circleLimit(currentX, currentY);

                    moveViewToPosition(view, circleLimitCoordinates[0], circleLimitCoordinates[1]);
                    if (currentX < startingX)
                    {
                       // color = Color.argb(alpha, greenRED, greenGREEN, greenBLUE);
                        color = Color.argb(alpha, redRED, redGREEN, redBLUE);
                        phoneAnswerImageView.setRotation(0.53f * alpha);
                    }
                    else if (currentX > startingX)
                    {
                      //  color = Color.argb(alpha, redRED, redGREEN, redBLUE);
                        color = Color.argb(alpha, greenRED, greenGREEN, greenBLUE);
                    }
                    else
                    {
                        color = Color.WHITE;
                    }
                    phoneAnswerImageView.setColorFilter(color);
                    break;

                case MotionEvent.ACTION_UP:
                    currentX = motionEvent.getRawX() + dX;
                    double screenRightSide = screenWidth * 0.68 + dX;
                    double screenLeftSide = screenWidth * 0.28 + dY;

                    if (currentX >= screenRightSide)
                    {
                        mCallback.OnAnswer();
                    }
                    else
                    {
                        if (currentX <= screenLeftSide)
                        {
                            mCallback.OnReject();
                        }
                    }
                    moveViewToPosition(view, startingX, startingY);
                    phoneAnswerImageView.setColorFilter(Color.WHITE);
                    phoneAnswerImageView.setRotation(0);
                    AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
                    fadeIn.setFillAfter(true);
                    fadeIn.setDuration(500);
                    mShadows.startAnimation(fadeIn);
                    mDots.startAnimation(fadeIn);

                    break;
                default:
                    return false;

            }


            return true;
        }

        private void moveViewToPosition(View view, float x, float y)
        {
            view.animate()
                .x(x)
                .y(y)
                .setDuration(0)
                .start();
        }
    }


    private class PhoneViewCallback implements IPhoneButtonCallback
    {
        @Override
        public void OnAnswer()
        {
            Log.d(this, "OnAnswer");
            SipServiceCommand.answerCall(ActivityIncomingCall.this, mCallId);
            OpenActivityCall();
            //finish();
        }

        @Override
        public void OnReject()
        {
            Log.d(this, "OnReject");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {
                mCallType = CallLog.Calls.REJECTED_TYPE;
            }
            else
            {
                mCallType = CallLog.Calls.INCOMING_TYPE;
            }

            SipServiceCommand.rejectCall(ActivityIncomingCall.this, mCallId);
            finish();
        }
    }
}
