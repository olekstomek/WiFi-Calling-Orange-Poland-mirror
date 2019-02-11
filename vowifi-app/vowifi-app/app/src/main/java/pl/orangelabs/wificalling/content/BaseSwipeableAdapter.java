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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.Utils;

/**
 * @author F
 */
public abstract class BaseSwipeableAdapter<T extends RecyclerItemBase, VH extends RecyclerView.ViewHolder> extends BaseAdapter<T, VH>
{
    protected final DisplayMetrics mMetrics;
    private final Paint mSwipeBackPaintSMS;
    private final Paint mSwipeBackPaintCall;
    private final Paint mSwipeImagePaint;
    private Bitmap mSwipeBackdropImageSMS;
    private Bitmap mSwipeBackdropImageCall;
    private IOnAdapterAction mCallback;
    private float mSwipeBitmapMargin;

    public BaseSwipeableAdapter(final Context ctx, final RecyclerView recyclerView, final IOnAdapterAction callback)
    {
        mCallback = callback;
        mSwipeBackPaintSMS = new Paint();
        mSwipeBackPaintSMS.setColor(ContextCompat.getColor(ctx, R.color.swipeBackgroundGreen));
        mSwipeBackPaintCall = new Paint();
        mSwipeBackPaintCall.setColor(ContextCompat.getColor(ctx, R.color.swipeBackgroundBlue));
        mSwipeImagePaint = new Paint();
        mSwipeBackdropImageCall = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_call);
        mSwipeBackdropImageSMS = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.ic_sms);
        mSwipeBitmapMargin = Utils.convertDpToPixels(ctx.getResources(), 10.0f);
        new SwipeHelper().attachToRecyclerView(recyclerView);

        final WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        mMetrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(mMetrics);
    }

    protected abstract boolean isSwipeable(final RecyclerItemBase item);

    public enum SwipeActionType
    {
        CALL,
    }

    public interface IOnAdapterAction
    {
        void onItemSwiped(final VHBase vh, final SwipeActionType swipeActionType);
    }

    private class SwipeHelper extends ItemTouchHelper
    {
        public SwipeHelper()
        {
            super(new Callback()
            {
                @Override
                public int getMovementFlags(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder)
                {
                    final int pos = viewHolder.getAdapterPosition();
                    return pos >= 0 && pos < mDataSet.size() && isSwipeable(mDataSet.get(pos))
                           ? makeMovementFlags(0, RIGHT)
                           : makeMovementFlags(0, 0);
                }

                @Override
                public boolean onMove(final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder, final RecyclerView.ViewHolder target)
                {

                    return false;
                }

                @Override
                public void onSwiped(final RecyclerView.ViewHolder viewHolder, final int direction)
                {
                    switch (direction)
                    {
                        case LEFT:
                            break;
                        case RIGHT:
                            mCallback.onItemSwiped((VHBase) viewHolder, SwipeActionType.CALL);
                            break;
                    }
                    notifyItemChanged(viewHolder.getAdapterPosition());
                }


                @Override
                public boolean isItemViewSwipeEnabled()
                {
                    return true;
                }

                @Override
                public void onChildDraw(final Canvas c, final RecyclerView recyclerView, final RecyclerView.ViewHolder viewHolder,
                                        final float dX, final float dY, final int actionState, final boolean isCurrentlyActive)
                {

                    if (actionState != ACTION_STATE_SWIPE)
                    {
                        return;
                    }

                    final View v = viewHolder.itemView;
                    final int backdropImageMiddlePos = v.getTop() + (v.getBottom() - v.getTop()) / 2 - mSwipeBackdropImageSMS.getHeight() / 2;

                    if (dX > 0)
                    {
                        c.drawRect((float) v.getLeft(), (float) v.getTop(), dX, (float) v.getBottom(), mSwipeBackPaintCall);
                        c.drawBitmap(mSwipeBackdropImageCall, (float) v.getLeft() + mSwipeBitmapMargin, backdropImageMiddlePos, mSwipeImagePaint);
                    }
                    else
                    {
                        c.drawRect((float) v.getRight() + dX, (float) v.getTop(), (float) v.getRight(), (float) v.getBottom(), mSwipeBackPaintSMS);
                        c.drawBitmap(mSwipeBackdropImageSMS,
                            v.getRight() - mSwipeBitmapMargin - mSwipeBackdropImageSMS.getWidth(), backdropImageMiddlePos, mSwipeImagePaint);
                    }
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            });
        }
    }
}
