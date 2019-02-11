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

package pl.orangelabs.wificalling.view.activation_client;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.ctrl.CustomFontButton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link FragmentTutorial#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentTutorial extends Fragment
{
    private static final String ARG_DESCRIPTION = "description";
    private static final String ARG_DRAWABLE = "drawable";
    private static final String ARG_PAGE_INDEX = "index";
    private static final String ARG_PAGE_COUNT = "page_count";
    private static final String ARG_PAGE_BTN_VISIBILITY = "details_visibility";

    //  private int description;
    private int drawable = 0;
    private int pageIndex;
    private int pageCount;
    private boolean mBtnVisibility;
    private ImageView tutorialImageView;
    private CustomFontButton mButton;
    private FragmentTutorialInteractionInterface fragmentTutorialInteractionInterface;


    public FragmentTutorial()
    {
    }

    public static FragmentTutorial newInstance(int drawable, int pageIndex, int pageCount, boolean isBtnVisible)
    {
        FragmentTutorial fragment = new FragmentTutorial();
        Bundle args = new Bundle();
        // args.putInt(ARG_DESCRIPTION, description);
        args.putInt(ARG_DRAWABLE, drawable);
        args.putInt(ARG_PAGE_INDEX, pageIndex);
        args.putInt(ARG_PAGE_COUNT, pageCount);
        args.putBoolean(ARG_PAGE_BTN_VISIBILITY, isBtnVisible);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        if (getArguments() != null)
        {
            //   description = getArguments().getInt(ARG_DESCRIPTION);
            drawable = getArguments().getInt(ARG_DRAWABLE);
            pageIndex = getArguments().getInt(ARG_PAGE_INDEX);
            pageCount = getArguments().getInt(ARG_PAGE_COUNT);
            mBtnVisibility = getArguments().getBoolean(ARG_PAGE_BTN_VISIBILITY);
        }
    }

    @Override
    public void onAttach(Context context)
    {
        super.onAttach(context);
        if (context instanceof FragmentTutorialInteractionInterface)
        {
            fragmentTutorialInteractionInterface = (FragmentTutorialInteractionInterface) context;
        }
        else
        {
            throw new RuntimeException(context.toString()
                                       + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onResume()
    {
        super.onResume();
        tutorialImageView.setImageResource(drawable);
        if(mBtnVisibility)
        {
            mButton.setVisibility(View.VISIBLE);
        }
        else
        {
            mButton.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (tutorialImageView != null && tutorialImageView.getDrawable() != null)
        {
            tutorialImageView.setImageDrawable(null);
        }
        mButton.setVisibility(View.GONE);
    }

    @Override
    public void onDetach()
    {
        super.onDetach();
        fragmentTutorialInteractionInterface = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_fragment_tutorial, container, false);
        tutorialImageView = ((ImageView) view.findViewById(R.id.iv_tutorial));
        mButton = ((CustomFontButton) view.findViewById(R.id.iv_tutorial_details));


        return view;
    }

    @Override
    public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);
        if(mButton!=null)
        {
            mButton.setOnClickListener(v ->
            {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.settings_wifi_url)));
                startActivity(browserIntent);
            });
        }
    }

    public interface FragmentTutorialInteractionInterface
    {
        void onNextSelected();

        void onEndSelected();
    }
}
