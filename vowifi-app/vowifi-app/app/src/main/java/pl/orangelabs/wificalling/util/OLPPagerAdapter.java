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

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;


public class OLPPagerAdapter<T extends OLPPagerAdapter.PagerEntry> extends PagerAdapter
{
    private final List<T> mObjects;

    public OLPPagerAdapter()
    {
        mObjects = new ArrayList<>();
    }

    @Override
    public int getCount()
    {
        return mObjects.size();
    }

    @Override
    public int getItemPosition(final Object object)
    {
        for (final T entry : mObjects)
        {
            if (entry.mView.equals(object))
            {
                return mObjects.indexOf(entry);
            }
        }
        return POSITION_NONE;
    }

    @Override
    public Object instantiateItem(final ViewGroup container, final int position)
    {
        if (HasItemAt(position))
        {
            final T entry = mObjects.get(position);
            container.addView(entry.mView);
            return entry.mView;
        }
        return null;
    }

    @Override
    public void destroyItem(final ViewGroup container, final int position, final Object object)
    {
        if (HasItemAt(position))
        {
            container.removeView(mObjects.get(position).mView);
        }
    }

    @Override
    public boolean isViewFromObject(final View view, final Object o)
    {
        return view == o;
    }

    public void Add(final T entry)
    {
        mObjects.add(entry);
    }

    public View View(final int position)
    {
        return HasItemAt(position)
               ? mObjects.get(position).mView
               : null;
    }

    public T Item(final int position)
    {
        return HasItemAt(position)
               ? mObjects.get(position)
               : null;
    }

    public int getIcon(final int position)
    {
        return HasItemAt(position)
               ? mObjects.get(position).mIconResId
               : 0;
    }

    @Override
    public CharSequence getPageTitle(final int position)
    {
        return HasItemAt(position)
               ? mObjects.get(position).mTitle
               : "";
    }

    private boolean HasItemAt(final int position)
    {
        return mObjects.size() > position;
    }

    public static class PagerEntry
    {
        public View mView;
        public String mTitle;
        public int mIconResId;

        public PagerEntry(final View view, final String title)
        {
            this(view, title, 0);
        }

        public PagerEntry(final View view, final String title, final int iconResId)
        {
            mView = view;
            mTitle = title;
            mIconResId = iconResId;
        }
    }
}
