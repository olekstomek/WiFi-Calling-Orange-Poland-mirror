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

package pl.orangelabs.wificalling.net.basic;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Simple json parser wrapper to allow cleaner parsing of deeply nested structures
 * <p/>
 * TODO needs some error handling
 *
 * @author F
 */
/*package*/ class OLPJsonParser
{
    OLPJsonContext parse(final String raw) throws JSONException
    {
        return new OLPJsonContext(raw);
    }

    @Nullable
    <T> T value(@NonNull final IOLPJsonContext ctx, @NonNull final String path)
    {
        return ctx.value(transformPath(path));
    }

    void moveContext(@NonNull final OLPJsonContext ctx, @NonNull final String path) throws JSONException
    {
        ctx.move(transformPath(path));
    }

    private String[] transformPath(final String rawPath)
    {
        return rawPath.split("/");
    }

    interface IOLPJsonContext
    {
        <T> T value(final String... path);

        OLPJsonArrayContext createArrayContext(final String... path);

        boolean exists(final String... path);
    }

    /*package*/ static class OLPJsonContext implements IOLPJsonContext
    {
        protected JSONObject mRoot;
        protected JSONObject mCurrent;

        private OLPJsonContext(final String raw) throws JSONException
        {
            this(new JSONObject(raw));
        }

        private OLPJsonContext(final JSONObject obj)
        {
            mRoot = obj;
            mCurrent = mRoot;
        }

        @Nullable
        @Override
        public <T> T value(@NonNull final String... path)
        {
            JSONObject base = path[0].isEmpty() ? mRoot : mCurrent;
            if (base == null)
            {
                return null;
            }
            for (int i = 0; i < path.length; ++i)
            {
                if (path[i].isEmpty())
                {
                    continue;
                }
                if (base.has(path[i]))
                {
                    final JSONObject newObj = base.optJSONObject(path[i]);
                    if (newObj == null && i == path.length - 1)
                    {
                        return (T) base.opt(path[i]);
                    }
                    else
                    {
                        base = newObj;
                    }
                }
                else
                {
                    return null;
                }
            }

            return null;
        }

        protected void move(@NonNull final String... path) throws JSONException
        {
            JSONObject base = path.length == 0 || path[0].isEmpty() ? mRoot : mCurrent;
            if (base == null)
            {
                return;
            }

            for (final String pathPart : path)
            {
                if (pathPart.isEmpty())
                {
                    continue;
                }
                if (base.has(pathPart))
                {
                    base = base.getJSONObject(pathPart);
                    if (base == null)
                    {
                        return;
                    }
                }
                else
                {
                    return;
                }
            }
            mCurrent = base;
        }

        @Nullable
        @Override
        public OLPJsonArrayContext createArrayContext(@NonNull final String... path)
        {
            final JSONArray array = value(path);
            if (array == null)
            {
                return null;
            }
            return new OLPJsonArrayContext(array);
        }

        @Override
        public boolean exists(@NonNull final String... path)
        {
            return value(path) != null;
        }
    }

    /*package*/ static final class OLPJsonArrayContext implements IOLPJsonContext
    {
        private final JSONArray mArray;
        private int mIndex;

        private OLPJsonArrayContext(final JSONArray array)
        {
            mArray = array;
            mIndex = 0;
        }

        int length()
        {
            return mArray.length();
        }

        @Override
        public <T> T value(@NonNull final String... path)
        {
            if (path.length == 0 || path[0].isEmpty())
            {
                throw new IllegalStateException("Moving context/rooted path not supported in array context");
            }

            JSONObject base = mArray.optJSONObject(mIndex);
            if (base == null)
            {
                return null;
            }

            for (int i = 0; i < path.length; ++i)
            {
                if (path[i].isEmpty())
                {
                    continue;
                }
                if (base.has(path[i]))
                {
                    final JSONObject newObj = base.optJSONObject(path[i]);
                    if (newObj == null && i == path.length - 1)
                    {
                        return (T) base.opt(path[i]);
                    }
                    else if (newObj != null)
                    {
                        base = newObj;
                    }
                    else
                    {
                        return null;
                    }
                }
                else
                {
                    return null;
                }
            }
            return null;
        }

        @Nullable
        @Override
        public OLPJsonArrayContext createArrayContext(final String... path)
        {
            final JSONArray array = value(path);
            if (array == null)
            {
                return null;
            }
            return new OLPJsonArrayContext(array);
        }

        @Override
        public boolean exists(final String... path)
        {
            return value(path) != null;
        }

        public void setIndex(int index)
        {
            mIndex = index;
        }
    }
}
