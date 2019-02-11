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
import android.content.DialogInterface;
import android.content.res.Resources;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.InputStream;

import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.flavoured.FontHandler;

/**
 * Created by kozlovsky on 10/17/2016.
 */

public class TermsAgreementDialog
{
    public static final String AGREEMENT_ACCEPTED = "AgreementAccepted";
    private TextView mAgreementText;

    /**
     * @param context
     * @param termsAndAgreementsDialogListener
     *     called on buttons action (only used if onlyInformative==false)
     * @param onlyInformative
     *     if true, dialog is displayed for informative purposes and doesn't offer accepting/declining tos
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void showTermsAndAgreementsDialog(Context context, final TermsAndAgreementsDialogListener termsAndAgreementsDialogListener,
                                             final boolean onlyInformative)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View layout = View.inflate(context, R.layout.dialog_agreement, null);
        mAgreementText = (TextView) layout.findViewById(R.id.dialog_agreement_text);
        try {
            Resources res = context.getResources();
            InputStream in_s = res.openRawResource(R.raw.terms);

            byte[] b = new byte[in_s.available()];
            in_s.read(b);
            mAgreementText.setText(new String(b));
        } catch (Exception e) {
            // e.printStackTrace();
            mAgreementText.setText("");
        }

        builder.setView(layout);
        if (onlyInformative)
        {
            final TextView title = (TextView) layout.findViewById(R.id.dialog_agreement_title);
            title.setText(R.string.accept_terms_informative_title);
            builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                dialog.dismiss();
            });
        }
        else
        {
            builder.setCancelable(false);
            builder.setPositiveButton(R.string.accept, (dialog, which) -> {
                SharedPrefs prefs = new SharedPrefs(context);
                prefs.Save(AGREEMENT_ACCEPTED, true);
                termsAndAgreementsDialogListener.onAcceptedTermsAndAgreement();
            });

            builder.setNegativeButton(R.string.cancel, (dialog, which) -> {
                dialog.dismiss();
            });
        }
        AlertDialog alert = builder.create();
        alert.show();

        FontHandler.setFont(alert);
        final Button negativeButton = alert.getButton(DialogInterface.BUTTON_NEGATIVE);
        if (negativeButton != null)
        {
            negativeButton.setTextColor(ResourcesCompat.getColor(context.getResources(), R.color.textInvertedAccented, null));
        }

    }


    @SuppressWarnings("deprecation") // lower API
    public void setTextFromHTML(String html)
    {
        mAgreementText.setText(Html.fromHtml(html));
    }

    public interface TermsAndAgreementsDialogListener
    {
        void onAcceptedTermsAndAgreement();
    }
}
