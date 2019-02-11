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

package pl.orangelabs.wificalling.view.customview;

import android.content.Context;
import android.support.v4.view.PagerAdapter;
import android.util.AttributeSet;

/**
 * Created by marcin on 24.05.17.
 */

public class CarouselViewPager extends android.support.v4.view.ViewPager
{
    public CarouselViewPager(Context context)
    {
        super(context);
        setChildrenDrawingCacheEnabled(true);
    }

    public CarouselViewPager(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }
    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int currentItem = this.getCurrentItem();
        if (i == currentItem)
        {
            return childCount -1;
        }
        else if (i == childCount-1)
        {
            return currentItem;
        }
        else {
            return i;
        }
    }

    @Override
    public void setAdapter(PagerAdapter adapter)
    {
        super.setAdapter(adapter);
        setPageTransformer(true, new CarouselPageTransformer());
        setPageMargin(-550);
        setOffscreenPageLimit(adapter.getCount());
    }
}
