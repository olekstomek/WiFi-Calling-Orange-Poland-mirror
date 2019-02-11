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
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView;

import pl.orangelabs.wificalling.content.VHBase;

/**
 * @author F
 */
public class ParallaxRecyclerView extends FastScrollRecyclerView
{
    private OnScrollListener mExternalScrollListener;


    public ParallaxRecyclerView(final Context context)
    {
        super(context);
        init();
    }

    public ParallaxRecyclerView(final Context context, @Nullable final AttributeSet attrs)
    {
        super(context, attrs);
        init();
    }

    public ParallaxRecyclerView(final Context context, @Nullable final AttributeSet attrs, final int defStyle)
    {
        super(context, attrs, defStyle);
        init();
    }

    public void addScrollCallback(final OnScrollListener scrollListener)
    {
        mExternalScrollListener = scrollListener;
    }

    private void init()
    {
        if (isInEditMode())
        {
            return; // it looks like preview can't handle this scroll listener
        }
        addOnScrollListener(new InternalScrollListener());
    }

    @Override
    protected void onLayout(final boolean changed, final int l, final int t, final int r, final int b)
    {
        super.onLayout(changed, l, t, r, b);
        if (!changed)
        {
            // we need to send fake "scroll" message to correctly recalculate parallax after layout pass (e.g. during fast scroll dragging)
            handleScroll(this);
        }
    }

    private void handleScroll(final RecyclerView recyclerView)
    {
        for (int i = 0; i < recyclerView.getChildCount(); i++)
        {
            ((VHBase) recyclerView.getChildViewHolder(recyclerView.getChildAt(i))).handleParallaxScroll();
        }
    }

    public void handleExternalScroll(final int offset)
    {
        handleScroll(this);
    }

    private class InternalScrollListener extends OnScrollListener
    {
        @Override
        public void onScrollStateChanged(final RecyclerView recyclerView, final int newState)
        {
            super.onScrollStateChanged(recyclerView, newState);
            if (mExternalScrollListener != null)
            {
                mExternalScrollListener.onScrollStateChanged(recyclerView, newState);
            }
        }

        @Override
        public void onScrolled(final RecyclerView recyclerView, final int dx, final int dy)
        {
            super.onScrolled(recyclerView, dx, dy);
            handleScroll(recyclerView);

            if (mExternalScrollListener != null)
            {
                mExternalScrollListener.onScrolled(recyclerView, dx, dy);
            }
        }
    }
}
