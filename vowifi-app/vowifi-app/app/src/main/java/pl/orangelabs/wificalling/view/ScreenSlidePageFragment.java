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

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import pl.orangelabs.wificalling.R;

/**
 * Created by kozlovsky on 10/17/2016.
 */

public class ScreenSlidePageFragment extends Fragment
{


    public static final String DRAWABLE_ID = "drawableId";
    public static final String STRING_ID = "stringId";
    public static final String SLIDE_NUMBER = "slideNumber";

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        View view = inflater.inflate(R.layout.fragment_slide, container, false);
        ImageView imageView = (ImageView) view.findViewById(R.id.fragment_pager_image);
        TextView textView = (TextView) view.findViewById(R.id.fragment_pager_text);

        imageView.setImageResource(getArguments().getInt(DRAWABLE_ID));
        textView.setText(getArguments().getInt(STRING_ID));

        ImageView indicator0 = (ImageView) view.findViewById(R.id.pager_indicator_0);
        ImageView indicator1 = (ImageView) view.findViewById(R.id.pager_indicator_1);
        ImageView indicator2 = (ImageView) view.findViewById(R.id.pager_indicator_2);


        int color = R.color.colorAccent;
        int colorWhite = R.color.white;


        int position = getArguments().getInt(SLIDE_NUMBER);
        switch (position)
        {
            case 0:
                changeBackgroundColor(indicator0, color);
                changeBackgroundColor(indicator1, colorWhite);
                changeBackgroundColor(indicator2, colorWhite);
                break;

            case 1:
                changeBackgroundColor(indicator0, colorWhite);
                changeBackgroundColor(indicator1, color);
                changeBackgroundColor(indicator2, colorWhite);

                break;

            case 2:
                changeBackgroundColor(indicator0, colorWhite);
                changeBackgroundColor(indicator1, colorWhite);
                changeBackgroundColor(indicator2, color);
                break;
        }


        return view;


    }

    private void changeBackgroundColor(ImageView imageView, int colorId)
    {

        Drawable background = imageView.getBackground();
        if (background instanceof ShapeDrawable)
        {
            ((ShapeDrawable) background).getPaint().setColor(ContextCompat.getColor(getContext(), colorId));
        }
        else if (background instanceof GradientDrawable)
        {
            ((GradientDrawable) background).setColor(ContextCompat.getColor(getContext(), colorId));
        }
        else if (background instanceof ColorDrawable)
        {
            ((ColorDrawable) background).setColor(ContextCompat.getColor(getContext(), colorId));
        }

    }

    public static ScreenSlidePageFragment newInstance(int drawableId, int stringId, int slideNumber)
    {


        ScreenSlidePageFragment fragment = new ScreenSlidePageFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(DRAWABLE_ID, drawableId);
        bundle.putInt(STRING_ID, stringId);
        bundle.putInt(SLIDE_NUMBER, slideNumber);
        fragment.setArguments(bundle);
        return fragment;
    }
}


