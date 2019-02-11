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

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.App;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.SettingsApp;
import pl.orangelabs.wificalling.net.basic.ApiResponse;
import pl.orangelabs.wificalling.net.requests.DefaultActivationServerRequest;
import pl.orangelabs.wificalling.net.responses.DefaultActivationServerResponse;
import pl.orangelabs.wificalling.util.flavoured.FontHandler;

import static android.content.ContentValues.TAG;

/**
 * @author F
 */
public class Utils
{
    private static String sVersion;
    private static int sVersionCode = -1;

    public static float getThemedDimension(final Context ctx, final int resId)
    {
        if (ctx != null && ctx.getTheme() != null)
        {
            final int[] attrs = {resId};
            final TypedArray ta = ctx.getTheme().obtainStyledAttributes(attrs);
            if (ta == null)
            {
                return -1.0f;
            }

            try
            {
                return ta.getDimension(0, -1.0f);
            }
            finally
            {
                ta.recycle();
            }
        }
        return -1.0f;
    }

    public static float convertDpToPixels(final DisplayMetrics dm, final float dp)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, dm);
    }

    public static float convertDpToPixels(final Resources res, final float dp)
    {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics());
    }

    private static String getOperatorName(Context context)
    {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getNetworkOperatorName();
    }

    @SuppressLint("HardwareIds")
    public static String getUserImsi(Context context)
    {
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getSubscriberId();
    }

    public static boolean isCorrectOperator(Context context)
    {
        String imsi = getUserImsi(context);
        return !SettingsApp.isProd || (imsi != null && !imsi.isEmpty() && imsi.startsWith("26003"));
    }

    public static boolean hasHardwareKeyboard(final Resources res)
    {
        final int keyboardType = res.getConfiguration().keyboard;
        return keyboardType == Configuration.KEYBOARD_12KEY || keyboardType == Configuration.KEYBOARD_QWERTY;
    }

    public static String firstLetterUnicode(final String str)
    {
        if (str == null || str.isEmpty())
        {
            return "";
        }
        return str.substring(0, str.offsetByCodePoints(0, 1)).toUpperCase(Locale.getDefault());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isNumeric(String str)
    {
        try
        {
            Double.parseDouble(str);
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }

    public static String deAccent(String str)
    {
        String nfdNormalizedString = Normalizer.normalize(str, Normalizer.Form.NFD);
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        return pattern.matcher(nfdNormalizedString).replaceAll("");
    }

    public static String AppVersion(final Context ctx)
    {
        try
        {
            if (sVersion == null || sVersion.isEmpty())
            {
                if (ctx != null)
                {
                    final PackageManager pm = ctx.getPackageManager();
                    if (pm != null)
                    {
                        final PackageInfo pInfo = pm.getPackageInfo(ctx.getPackageName(), 0);
                        sVersion = pInfo.versionName;
                        return sVersion;
                    }
                }
            }
            else
            {
                return sVersion;
            }
        }
        catch (final android.content.pm.PackageManager.NameNotFoundException e)
        {
            Log.w(ctx, "Could not retrieve app version name", e);
        }
        return "";
    }

    public static int AppVersionCode(final Context ctx)
    {
        try
        {
            if (sVersionCode < 0)
            {
                final PackageManager pm = ctx.getPackageManager();
                if (pm != null)
                {
                    final PackageInfo pInfo = pm.getPackageInfo(ctx.getPackageName(), 0);
                    sVersionCode = pInfo.versionCode;
                }
            }
            return sVersionCode;
        }
        catch (final android.content.pm.PackageManager.NameNotFoundException e)
        {
            Log.w(ctx, "Could not retrieve app version code", e);
        }
        return -1;
    }

    /**
     * Converts a drawable into a bitmap.
     *
     * @param drawable
     *     the drawable to be converted.
     */
    public static Bitmap drawableToBitmap(Drawable drawable)
    {
        Bitmap bitmap;
        if (drawable instanceof BitmapDrawable)
        {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        }
        else
        {
            if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0)
            {
                // Needed for drawables that are just a colour.
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888);
            }
            else
            {
                bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            }

            Log.i(TAG, "Created bitmap with width " + bitmap.getWidth() + ", height "
                       + bitmap.getHeight());

            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }
        return bitmap;
    }

    public static AlertDialog showDialogWithText(int textMessage, Context context)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
            .setTitle(R.string.app_name)
            .setMessage(textMessage)
            .setPositiveButton(android.R.string.yes, (dialog, which) ->
            {
            })
            .setIcon(R.drawable.ic_app_logo)
            .create();
        FontHandler.setFont(alertDialog);
        return alertDialog;
    }
    public static AlertDialog createSimHasChangedDialog(Context context)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.dialog_msg_sim_changed)
                .setPositiveButton(android.R.string.yes, (dialog, which) ->
                        App.getActivationComponent().clearActivation())
                .setNegativeButton(android.R.string.no, (dialog, which) ->
                {
                })
                .setIcon(R.drawable.ic_app_logo)
                .create();
        FontHandler.setFont(alertDialog);
        return alertDialog;
    }
    public static AlertDialog createCannotConnectResetDialog(Context context)
    {
        AlertDialog alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.app_name)
                .setMessage(R.string.dialog_error_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                {
                })

                .setNeutralButton(R.string.dialog_reset_button, (dialog, which) ->
                        App.getActivationComponent().clearActivation())
                .setIcon(R.drawable.ic_app_logo)
                .create();
        FontHandler.setFont(alertDialog);
        return alertDialog;
    }

    public static android.app.AlertDialog.Builder getRequestFailedBuilder(
            ApiResponse<DefaultActivationServerResponse, DefaultActivationServerRequest> mDefaultResponse
            , Context context, DialogInterface.OnClickListener onClickListener)
    {
        return new android.app.AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.error_title))
                .setCancelable(false)
                .setMessage(mDefaultResponse.getObject() != null && mDefaultResponse.getObject().getResponseDescription() != null ?
                        mDefaultResponse.getObject().getResponseDescription() : context.getString(R.string.msg_connection_error))
                .setPositiveButton(android.R.string.ok, onClickListener)
                .setIcon(android.R.drawable.ic_dialog_alert);
    }
    public static ProgressDialog getProgressDialog(Context context,int contentId)
    {
        ProgressDialog progressDialog = new ProgressDialog(context);
        progressDialog.setTitle(context.getString(R.string.app_name));
        progressDialog.setMessage(context.getString(contentId));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(false);
        progressDialog.setIcon(R.drawable.ic_app_logo);
        return progressDialog;
    }
    public static String getHash256FromString(String value)
    {
        MessageDigest digest;
        String hash = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
            digest.update(value.getBytes());

            hash = bytesToHexString(digest.digest());

        }
        catch (NoSuchAlgorithmException e1)
        {
            Log.e("Hash","Hash",e1);
        }
        return hash;
    }
    private static String bytesToHexString(byte[] bytes)
    {
        // http://stackoverflow.com/questions/332079
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++)
        {
            String hex = Integer.toHexString(0xFF & bytes[i]);
            if (hex.length() == 1)
            {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    public static void BatterySavingFeature(final Context context)
    {
        String manufacturer = Build.MANUFACTURER;
        if (manufacturer.equalsIgnoreCase("huawei"))
        {
            new HuaweiProtect(context).ifHuaweiAlert();
        }
    }
    public static void showRoutingTable()
    {
        if (SettingsApp.showWifiSettings)
        {
            String commandResult = Utils.runADBCommand("ip route list table all");
            Log.d("RouteTable", commandResult);
        }
    }
    public static String runADBCommand(String command)
    {
        StringBuilder stringBuilder = new StringBuilder(command + "\n");
        try{
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));
            String textLine;
            do {
                textLine = bufferedReader.readLine();
                stringBuilder.append(textLine).append("\n");
            } while(textLine != null);
        }catch(IOException e){
           Log.e("exception", e.toString());
        }
        return stringBuilder.toString();
    }
}
