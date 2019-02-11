package org.strongswan.android.logic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class NetworkManager extends BroadcastReceiver
{
    private final Context mContext;
    private boolean mRegistered;

    public NetworkManager(Context context)
    {
        mContext = context;
    }

    public void Register()
    {
       // mContext.registerReceiver(this, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    public void Unregister()
    {
      //  mContext.unregisterReceiver(this);
    }

    public boolean isConnected()
    {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = null;
        if (cm != null)
        {
            info = cm.getActiveNetworkInfo();
        }
        return info != null && info.isConnected();
    }

    @Override
    public void onReceive(Context context, Intent intent)
    {
       // networkChanged(!isConnected());
    }

    /**
     * Notify the native parts about a network change
     *
     * @param disconnected
     *     true if no connection is available at the moment
     */
    @SuppressWarnings("JniMissingFunction")
    public native void networkChanged(boolean disconnected);
}