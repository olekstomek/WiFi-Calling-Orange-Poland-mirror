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

package pl.orangelabs.wificalling.view.helper;

import android.app.Dialog;
import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.content.NumberPickerDialogAdapter;
import pl.orangelabs.wificalling.content.items.NumberPickerItem;
import pl.orangelabs.wificalling.ctrl.RecyclerSeparatorDecoration;
import pl.orangelabs.wificalling.model.PhoneNumber;
import pl.orangelabs.wificalling.util.flavoured.FontHandler;

/**
 * Created by Cookie on 2016-10-04.
 */

public class PhonePickerDialog
{
    private final NumberPickerDialogAdapter.IOnPhoneAction mCallback;

    public PhonePickerDialog(final NumberPickerDialogAdapter.IOnPhoneAction callback)
    {
        mCallback = callback;
    }

    public void ShowDialAction(final Context context, final List<PhoneNumber> numbers)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        View inflated = View.inflate(context, R.layout.dialog_pick_number, null);
        RecyclerView recyclerView = (RecyclerView) inflated.findViewById(R.id.number_picker_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

        NumberPickerDialogAdapter adapter = new NumberPickerDialogAdapter();

        adapter.mDataSet = Stream.of(numbers).map(NumberPickerItem::new).collect(Collectors.toList());
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new RecyclerSeparatorDecoration(context));
        builder.setView(inflated);

        AlertDialog dialog = builder.create();

        adapter.setCallback(new OnPhoneItemAction(dialog));
        dialog.show();
        FontHandler.setFont(dialog);
    }

    private class OnPhoneItemAction implements NumberPickerDialogAdapter.IOnPhoneAction
    {
        final Dialog mDialog;

        OnPhoneItemAction(final Dialog dialog)
        {
            mDialog = dialog;
        }

        @Override
        public void onPressed(final String number)
        {
            mDialog.dismiss();
            mCallback.onPressed(number);
        }

    }
}
