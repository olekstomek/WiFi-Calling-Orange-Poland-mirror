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

import android.view.View;

import com.eftimoff.viewpagertransformers.BaseTransformer;

/**
 * Created by marcin on 24.05.17.
 */

public class CarouselPageTransformer extends BaseTransformer
{
    @Override
    protected void onTransform(View view, float position)
    {
        if(position >= -1.0F || position <= 1.0F) {
            float height = (float)view.getHeight();
            float scaleFactor = Math.max(0.85F, 1.0F - Math.abs(position));
            float vertMargin = height * (1.0F - scaleFactor) / 2.0F;
            float horzMargin = (float)view.getWidth() * (1.0F - scaleFactor) / 2.0F;
            view.setPivotY(0.5F * height);
            if(position < 0.0F) {
                view.setTranslationX(horzMargin - vertMargin / 2.0F);
            } else {
                view.setTranslationX(-horzMargin + vertMargin / 2.0F);
            }
            view.setScaleX(scaleFactor);
            view.setScaleY(scaleFactor);
            view.setAlpha(0.5F + (scaleFactor - 0.85F) / 0.14999998F * 0.5F);
        }
    }
}
