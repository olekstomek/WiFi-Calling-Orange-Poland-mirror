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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.List;

import pl.orangelabs.log.Log;

import static android.content.ContentValues.TAG;

/**
 * Created by marcin on 09.02.17.
 */

public class WifiUtils
{

    public static int getCurrentNetworkRSSI(Context context)
    {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        return wifiInfo.getRssi();
    }
    public static void isInternetConnectionAvailable(InternetCheckerListener mInternetCheckerListener, Context context)
    {
        new InternetCheckerAsyncTask(mInternetCheckerListener, context).execute();
    }

    static public boolean isInternetConnectionAvailable(Context context)
    {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected())
        {
            try
            {
                URL url = new URL("https://google.com");
                HttpURLConnection hppHttpURLConnection = (HttpURLConnection) url.openConnection();
                hppHttpURLConnection.setConnectTimeout(1500);
                hppHttpURLConnection.connect();
                if (hppHttpURLConnection.getResponseCode() == 200)
                {
                    android.util.Log.d(TAG, "isInternetConnectionAvailable: true");
                    return true;
                }
                else
                {
                    android.util.Log.d(TAG, "isInternetConnectionAvailable: false");
                    return false;
                }
            }
            catch (MalformedURLException e1)
            {
                android.util.Log.d(TAG, "isInternetConnectionAvailable: " + e1.getMessage());
                return false;
            }
            catch (IOException e)
            {
                android.util.Log.d(TAG, "isInternetConnectionAvailable: " + e.getMessage());
                return false;
            }
        }
        return false;
    }

    public interface InternetCheckerListener
    {
        void onResult(boolean isOnline);
    }

    public static class InternetCheckerAsyncTask extends AsyncTask<Void, Void, Boolean>
    {
        InternetCheckerListener mInternetCheckerListener;
        Context mContext;

        public InternetCheckerAsyncTask(InternetCheckerListener internetCheckerListener, Context context)
        {
            mInternetCheckerListener = internetCheckerListener;
            mContext = context;
        }

        @Override
        protected Boolean doInBackground(Void... params)
        {
            return isInternetConnectionAvailable(mContext);
        }

        @Override
        protected void onPostExecute(Boolean isAvailable)
        {
            super.onPostExecute(isAvailable);
            mInternetCheckerListener.onResult(isAvailable);
        }
    }
    public static String getVPNIPAddress(boolean useIPv4)
    {
        try
        {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : interfaces)
            {
                if (networkInterface.isUp() && networkInterface.getName().contains("tun0"))
                {
                    List<InetAddress> inetAddresses = Collections.list(networkInterface.getInetAddresses());
                    for (InetAddress inetAddress : inetAddresses)
                    {
                        if (!inetAddress.isLoopbackAddress())
                        {
                            String hostAddress = inetAddress.getHostAddress();
                            boolean isIPv4 = hostAddress.indexOf(':') < 0;

                            if (useIPv4)
                            {
                                if (isIPv4)
                                    return hostAddress;
                            }
                            else
                            {
                                if (!isIPv4)
                                {
                                    int delim = hostAddress.indexOf('%'); // drop ip6 zone suffix
                                    return delim < 0 ? hostAddress.toUpperCase() : hostAddress.substring(0, delim).toUpperCase();
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            Log.d(TAG, ex.toString());
        }
        return "";
    }

    public static boolean isVPNActive()
    {
        try {
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces()))
            {
                if (networkInterface.isUp() && networkInterface.getName().contains("tun0"))
                {
                    return true;
                }
            }
        }
        catch (Exception ex)
        {
            Log.d(TAG, ex.toString());
        }
        return false;
    }
}
