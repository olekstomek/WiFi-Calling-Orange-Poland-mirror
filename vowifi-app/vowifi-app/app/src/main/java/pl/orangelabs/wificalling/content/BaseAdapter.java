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

package pl.orangelabs.wificalling.content;

import android.support.v7.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author F
 */
public abstract class BaseAdapter<T extends RecyclerItemBase, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH>
{
    public List<T> mDataSet = new ArrayList<>();

    @Override
    public final void onBindViewHolder(final VH holder, final int position)
    {
        final T item = item(position);
        bind(holder, item, getItemViewType(position));
    }

    protected abstract void bind(final VH holder, final T item, final int viewType);

    public T item(int position)
    {
        return mDataSet.get(position);
    }

    @Override
    public int getItemCount()
    {
        return mDataSet.size();
    }

    @Override
    public int getItemViewType(final int position)
    {
        return item(position).mType;
    }

    public void replaceDataset(final List<T> cachedEntries)
    {
        mDataSet = cachedEntries;
        notifyDataSetChanged();
    }
}
