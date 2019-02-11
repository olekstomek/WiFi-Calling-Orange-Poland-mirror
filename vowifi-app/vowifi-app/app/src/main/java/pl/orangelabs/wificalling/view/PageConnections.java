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

import android.content.Context;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.BaseSwipeableAdapter;
import pl.orangelabs.wificalling.content.ConnectionsPageAdapter;
import pl.orangelabs.wificalling.content.RecyclerItemBase;
import pl.orangelabs.wificalling.content.VHBase;
import pl.orangelabs.wificalling.content.items.ContactDetailsConnectionItem;
import pl.orangelabs.wificalling.content.items.DividerItem;
import pl.orangelabs.wificalling.ctrl.ParallaxRecyclerView;
import pl.orangelabs.wificalling.ctrl.RecyclerSeparatorDecoration;
import pl.orangelabs.wificalling.model.Dividers;
import pl.orangelabs.wificalling.model.VWCallLog;
import pl.orangelabs.wificalling.model.db.AsyncGetCallLogs;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.t9.T1000Dictionary;
import pl.orangelabs.wificalling.util.AsyncHelper;
import pl.orangelabs.wificalling.util.MemoryCache;
import pl.orangelabs.wificalling.util.SharedPrefs;

/**
 * @author F
 */
public class PageConnections extends PageBase
{
    private static final ApproxDividerPhaseEntry[] APPROX_DIVIDER_PHASES = new ApproxDividerPhaseEntry[]
        {
            // TODO decide final intervals
            new ApproxDividerPhaseEntry(-1, R.string.call_log_divider_approx_today),
            new ApproxDividerPhaseEntry(0, R.string.call_log_divider_approx_yesterday),
            new ApproxDividerPhaseEntry(1, R.string.call_log_divider_approx_week),
            new ApproxDividerPhaseEntry(6, R.string.call_log_divider_approx_month),
            new ApproxDividerPhaseEntry(29, R.string.call_log_divider_approx_year),
            new ApproxDividerPhaseEntry(364, R.string.call_log_divider_approx_forever)
        };
    private ConnectionsPageAdapter mAdapter;

    public PageConnections(final Context context)
    {
        super(context);
        init(context);
    }


    public PageConnections(final Context context, final AttributeSet attrs)
    {
        super(context, attrs);
        init(context);
    }

    public PageConnections(final Context context, final AttributeSet attrs, final int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    public void handleCTLScroll(final int offset)
    {
        mRecycler.handleExternalScroll(offset);
    }

    @Override
    public void onContactsDataChanged(final T1000Dictionary.T1000DataSet entries)
    {
        AsyncHelper.execute(new AsyncGetCallLogs(getContext(), new PageConnections.OnGetCallLogCallback()));
    }

    @Override
    public void onCallLogsDataChanged(List<VWCallLog> callLogList)
    {
        updateAdapter(callLogList);
    }

    private void init(final Context context)
    {
        final LayoutInflater inf = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View inflated = inf.inflate(R.layout.page_connections, this, true);
        mRecycler = (ParallaxRecyclerView) inflated.findViewById(R.id.page_connections_recycler);
        mRecycler.addItemDecoration(new RecyclerSeparatorDecoration(context));
        mRecycler.setLayoutManager(new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));
        initErrorFrame(inflated);
        if (MemoryCache.Me().Contains(T1000Dictionary.T1000DataSet.class))
        {
            AsyncHelper.execute(new AsyncGetCallLogs(getContext(), new PageConnections.OnGetCallLogCallback()));
        }
    }

    private String resolveDividerContent(final VWCallLog callLogPrevEntry, final VWCallLog callLogEntry,
                                         final SharedPrefs.ConnectionsDividerMode dividerMode)
    {
        if (callLogPrevEntry == null) // first entry always needs a divider
        {
            return resolveDividerContent(callLogEntry, dividerMode);
        }
        final Calendar calPrev = Calendar.getInstance();
        final Calendar calNext = Calendar.getInstance();
        calPrev.setTime(new Date(callLogPrevEntry.mDate));
        calNext.setTime(new Date(callLogEntry.mDate));
        switch (dividerMode)
        {
            case DETAILED:
                if (calPrev.get(Calendar.DAY_OF_YEAR) != calNext.get(Calendar.DAY_OF_YEAR) ||
                    calPrev.get(Calendar.YEAR) != calNext.get(Calendar.YEAR))
                {
                    return resolveDividerContent(callLogEntry, dividerMode);
                }
                break;
            case APPROXIMATE:
            default:
                ApproxDividerPhaseEntry bestPhaseEntry = resolveApproxDividerPhaseEntry(calPrev, calNext);
                if (bestPhaseEntry != null)
                {
                    return getContext().getString(bestPhaseEntry.mLabelResId);
                }
                break;
        }
        return null;
    }

    @Nullable
    private ApproxDividerPhaseEntry resolveApproxDividerPhaseEntry(final Calendar calPrev, final Calendar calNext)
    {
        final Calendar calTest = Calendar.getInstance();
        ApproxDividerPhaseEntry bestPhaseEntry = null;
        Date todayWithZeroTime = getTodayAtZeroZeroZero();
        for (final ApproxDividerPhaseEntry phaseEntry : APPROX_DIVIDER_PHASES)
        {
            calTest.setTime(todayWithZeroTime);
            calTest.add(Calendar.DAY_OF_MONTH, -phaseEntry.mDays);
            if ((calPrev == null || calPrev.after(calTest)) && calNext.before(calTest))
            {
                bestPhaseEntry = phaseEntry;
            }
        }
        return bestPhaseEntry;
    }

    @NonNull
    private Date getTodayAtZeroZeroZero()
    {
        Date todayWithZeroTime = null;
        try
        {
            Date today = new Date();
            DateFormat formatter = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            todayWithZeroTime = formatter.parse(formatter.format(today));
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        if (todayWithZeroTime == null){
            todayWithZeroTime = new Date();
        }
        return todayWithZeroTime;
    }

    private String resolveDividerContent(final VWCallLog entry, final SharedPrefs.ConnectionsDividerMode dividerMode)
    {
        switch (dividerMode)
        {
            case DETAILED:
                return new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(new Date(entry.mDate));
            case APPROXIMATE:
            default:
                final Calendar calNext = Calendar.getInstance();
                calNext.setTime(new Date(entry.mDate));
                final ApproxDividerPhaseEntry bestPhaseEntry = resolveApproxDividerPhaseEntry(null, calNext);
                if (bestPhaseEntry != null)
                {
                    return getContext().getString(bestPhaseEntry.mLabelResId);
                }
        }
        return null;
    }

    private void onAdapterAction(final VHBase vhBase, final BaseSwipeableAdapter.SwipeActionType swipeActionType)
    {
        Log.i(this, "Adapter action: " + swipeActionType);
        if (swipeActionType.equals(BaseSwipeableAdapter.SwipeActionType.CALL))
        {
            ContactDetailsConnectionItem connectionItem = ((ContactDetailsConnectionItem) mAdapter.mDataSet.get(vhBase.getAdapterPosition()));
            makeCall(connectionItem);
        }

    }

    private void makeCall(ContactDetailsConnectionItem connectionItem)
    {
        if (connectionItem.mCallLog.mPhoneNumber.startsWith("-"))
        {
            // TODO: 2016-09-30 probably number is private
        }
        else
        {
            SipServiceCommand.makeCall(getContext(), connectionItem.mCallLog.mPhoneNumber);
        }
    }

    private void onConnectionClick(ContactDetailsConnectionItem contactDetailsConnectionItem)
    {
        makeCall(contactDetailsConnectionItem);
    }

    private static class ApproxDividerPhaseEntry
    {
        public final int mDays;
        public final int mLabelResId;

        public ApproxDividerPhaseEntry(final int days, final int labelResId)
        {
            mDays = days;
            mLabelResId = labelResId;
        }
    }

    private class OnGetCallLogCallback implements AsyncGetCallLogs.OnCallLogByContactResultCallback
    {
        @Override
        public void OnLoadCompleteListener(final List<VWCallLog> callLogList)
        {
            if (mAdapter != null)
            {
                mAdapter.mDataSet.clear();
            }
            updateAdapter(callLogList);
        }
    }

    private void updateAdapter(List<VWCallLog> callLogList)
    {
        if (mRecycler == null || callLogList == null)
        {
            return;
        }
        boolean setAdapter = false;
        if (mAdapter == null)
        {
            setAdapter = true;
            mAdapter = new ConnectionsPageAdapter(getContext(), mRecycler, PageConnections.this::onAdapterAction, PageConnections.this::onConnectionClick);
        }
        if (mAdapter.mDataSet.size() == 0 || callLogList.size() == 0)
        {
            initCallLogsAdapter(callLogList);
        }
        else
        {
            updateCallLogList(callLogList);
        }

        if (setAdapter)
        {
            mRecycler.setAdapter(mAdapter);
        }
    }


    private void initCallLogsAdapter(List<VWCallLog> callLogList)
    {
        if (callLogList != null)
        {
            new AsyncTaskNewList(callLogList).execute();
        }
    }
    private void updateCallLogList(List<VWCallLog> callLogList)
    {

        if (callLogList != null)
        {
            boolean isAdded = addMissingElements(callLogList);
            if (!isAdded)
            {
                Log.e(this,"nie zostal dodany");
                new AsyncTaskRemove(callLogList).execute();
            }
            else
            {
                mAdapter.notifyDataSetChanged();
                mErrorFrame.setVisibility(GONE);
            }
        }
    }

    private boolean addMissingElements(List<VWCallLog> callLogList)
    {
        addToday(callLogList);
        int firstNotDividerIndex = getFirstNotDividerItem();
        boolean isAdded = false;
        if (firstNotDividerIndex != Integer.MAX_VALUE)
        {
            int lastId = ((ContactDetailsConnectionItem) mAdapter.mDataSet.get(firstNotDividerIndex)).mCallLog.id;
            for (int index = 0; index < callLogList.size(); index++)
            {
                VWCallLog callLogEntry = callLogList.get(index);
                if (callLogEntry.id > lastId)
                {
                    isAdded = true;
                    mAdapter.mDataSet.add(index + 1, new ContactDetailsConnectionItem(callLogEntry));
                }
                else
                {
                    break;
                }
            }
        }
        return isAdded;
    }

    private void addToday(List<VWCallLog> callLogList)
    {
        final SharedPrefs.ConnectionsDividerMode dividerMode =
                mPrefs.LoadEnumFromBool(SharedPrefs.KEY_SETTINGS_DIVIDER_MODE, SharedPrefs.Defaults.DIVIDER_MODE);
        final String dividerContent = resolveDividerContent(null, callLogList.get(0), dividerMode);
        if (dividerContent != null)
        {
            if (mAdapter.mDataSet.get(0) instanceof DividerItem && !(((DividerItem) mAdapter.mDataSet.get(0)).mHeader.equals(dividerContent)))
            {
                mAdapter.mDataSet.add(0, new DividerItem(dividerContent, ConnectionsPageAdapter.VIEW_TYPE_DIVIDER, Dividers.DividerType.CALLS));
            }
        }
    }

    private int getFirstNotDividerItem()
    {
        for (int index = 0; index < mAdapter.mDataSet.size(); index++)
        {
            if (mAdapter.mDataSet.get(index) instanceof ContactDetailsConnectionItem)
            {
                return index;
            }
        }
        return Integer.MAX_VALUE;
    }
    private class AsyncTaskNewList extends AsyncTask<Void, Void, List<RecyclerItemBase>>{

        private List<VWCallLog> callLogList = null;
        private AsyncTaskNewList(List<VWCallLog> callLogList)
        {
            this.callLogList = callLogList;
        }

        @Override
        protected List<RecyclerItemBase> doInBackground(Void... params)
        {
            final SharedPrefs.ConnectionsDividerMode dividerMode =
                    mPrefs.LoadEnumFromBool(SharedPrefs.KEY_SETTINGS_DIVIDER_MODE, SharedPrefs.Defaults.DIVIDER_MODE);
            VWCallLog callLogPrevEntry = null;
            List<RecyclerItemBase> newList = new ArrayList<>();
            for (final VWCallLog callLogEntry : callLogList)
            {
                //noinspection WrongThread
                final String dividerContent = resolveDividerContent(callLogPrevEntry, callLogEntry, dividerMode);
                if (dividerContent != null)
                {
                    newList.add(new DividerItem(dividerContent, ConnectionsPageAdapter.VIEW_TYPE_DIVIDER, Dividers.DividerType.CALLS));
                }
                newList.add(new ContactDetailsConnectionItem(callLogEntry));
                callLogPrevEntry = callLogEntry;
            }
            return newList;
        }

        @Override
        protected void onPostExecute(List<RecyclerItemBase> recyclerItemBases)
        {

            if (recyclerItemBases.isEmpty())
            {
                mAdapter.mDataSet.clear();
                handleLoadingError(R.string.page_connections_error_empty, false);
                mAdapter.notifyDataSetChanged();
            }
            else
            {
                mAdapter.mDataSet.clear();
                mAdapter.mDataSet.addAll(recyclerItemBases);
                mAdapter.notifyDataSetChanged();
                mErrorFrame.setVisibility(GONE);
                mRecycler.setVisibility(VISIBLE);
            }
        }
    }
    private class AsyncTaskRemove extends AsyncTask<Void, Void, List<RecyclerItemBase>>{

        private List<VWCallLog> callLogList = null;
        private AsyncTaskRemove(List<VWCallLog> callLogList)
        {
            this.callLogList = callLogList;
        }

        @Override
        protected List<RecyclerItemBase> doInBackground(Void... params)
        {
            List<RecyclerItemBase> listToRemove = getItemsToRemove();
            if (listToRemove.size() > 0)
            {
                mAdapter.mDataSet.removeAll(listToRemove);
                removeOldDividers();
            }
            return listToRemove;
        }

        @NonNull
        private List<RecyclerItemBase> getItemsToRemove()
        {
            List<RecyclerItemBase> listToRemove = new LinkedList<>();
            for (Iterator<RecyclerItemBase> iterator = new ArrayList<>(mAdapter.mDataSet).listIterator(); iterator.hasNext();)
            {
                RecyclerItemBase item = iterator.next();
                if (item instanceof ContactDetailsConnectionItem)
                {
                    boolean isInCallLogList = false;
                    for (VWCallLog callLogEntry : callLogList)
                    {
                        if (((ContactDetailsConnectionItem) item).mCallLog.id == callLogEntry.id)
                        {
                            isInCallLogList = true;
                            break;
                        }
                    }
                    if (!isInCallLogList)
                    {
                        listToRemove.add(item);
                    }
                }
            }
            return listToRemove;
        }

        private void removeOldDividers()
        {
            List<RecyclerItemBase> listToRemoveItem = new LinkedList<>();
            RecyclerItemBase recyclerItemBase = null;
            for (Iterator<RecyclerItemBase> iterator = new ArrayList<>(mAdapter.mDataSet).listIterator(); iterator.hasNext();)
            {
                RecyclerItemBase item = iterator.next();
                if (recyclerItemBase != null && recyclerItemBase instanceof DividerItem && item instanceof  DividerItem)
                {
                    listToRemoveItem.add(recyclerItemBase);
                }
                recyclerItemBase = item;
            }
            mAdapter.mDataSet.removeAll(listToRemoveItem);
        }

        @Override
        protected void onPostExecute(List<RecyclerItemBase> recyclerItemBases)
        {
            mAdapter.notifyDataSetChanged();
        }
    }
}
