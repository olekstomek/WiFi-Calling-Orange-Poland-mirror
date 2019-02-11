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

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.flavoured.FontHandler;

/**
 * Created by kozlovsky on 10/17/2016.
 */

public class LicensesDialog
{
    public void show(Context context)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View layout =  View.inflate(context, R.layout.dialog_licenses, null);
        loadContent(context, (WebView) layout.findViewById(R.id.dialog_licenses_content));
        builder.setView(layout);

        builder.setPositiveButton(android.R.string.ok, (dialog, which) -> dialog.dismiss());

        AlertDialog alert = builder.create();
        alert.show();

        FontHandler.setFont(alert);
    }

    private void loadContent(final Context ctx, final WebView contentView)
    {
        final InputStream resStream = ctx.getResources().openRawResource(R.raw.licenses);
        try (final BufferedReader is = new BufferedReader(new InputStreamReader(resStream, "UTF-8")))
        {
            String line;
            final StringBuilder sb = new StringBuilder();
            while ((line = is.readLine()) != null)
            {
                sb.append(line).append("\r\n");
            }

            contentView.loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF-8", null);
        }
        catch (final IOException e)
        {
            Log.w(this, "Error reading res", e);
        }
    }
}
