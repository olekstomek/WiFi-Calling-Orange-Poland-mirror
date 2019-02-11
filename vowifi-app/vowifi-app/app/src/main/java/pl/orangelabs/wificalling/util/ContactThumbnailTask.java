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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.widget.ImageView;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.ref.WeakReference;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;

public class ContactThumbnailTask extends AsyncTask<Void, Void, Bitmap>
{

    private final WeakReference<ImageView> imageViewWeakReference;
    private final Uri uri;
    private final String path;
    private final Context context;
    private int mDefaultThumbResId = R.drawable.ic_avatar_thumbnail;


    public ContactThumbnailTask(final ImageView imageView, final Uri uri, final Context context, int defaultThumbResId)
    {
        this.uri = uri;
        this.imageViewWeakReference = new WeakReference<>(imageView);
        imageView.setTag(uri.toString());
        this.path = (String) imageViewWeakReference.get().getTag(); // to make sure we don't put the wrong image on callback
        this.context = context;
        mDefaultThumbResId = defaultThumbResId;
    }

    public ContactThumbnailTask(final ImageView imageView, final String string, final Context context)
    {
        this.uri = Uri.parse(string);
        this.imageViewWeakReference = new WeakReference<>(imageView);
        imageView.setTag(uri.toString());
        this.path = (String) imageViewWeakReference.get().getTag(); // to make sure we don't put the wrong image on callback
        this.context = context;
    }


    @Override
    protected Bitmap doInBackground(final Void... params)
    {
        InputStream is = null;
        try
        {
            is = context.getContentResolver().openInputStream(uri);
        }
        catch (FileNotFoundException e)
        {
            Log.w(this, "", e);
        }

        Bitmap image = null;
        if (null != is)
        {
            image = BitmapFactory.decodeStream(is);
        }

        return image;
    }

    @Override
    protected void onPostExecute(final Bitmap bitmap)
    {
        if (imageViewWeakReference != null && imageViewWeakReference.get() != null)
        {
            if ((imageViewWeakReference.get().getTag()).equals(path))
            {
                if (bitmap != null)
                {
                    RoundedBitmapDrawable drawable = RoundedBitmapDrawableFactory.create(context.getResources(), bitmap);
                    drawable.setCircular(true);
                    imageViewWeakReference.get().setImageDrawable(drawable);
                    return;
                }
            }
            imageViewWeakReference.get().setImageResource(mDefaultThumbResId);
        }
    }
}