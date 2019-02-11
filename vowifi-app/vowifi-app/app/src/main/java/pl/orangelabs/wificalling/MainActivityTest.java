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

package pl.orangelabs.wificalling;

import android.Manifest;
import android.annotation.SuppressLint;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.strongswan.android.logic.CharonVpnService;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import pl.orangelabs.log.ActivityLogs;
import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.net.basic.ApiResponse;
import pl.orangelabs.wificalling.net.requests.DefaultActivationServerRequest;
import pl.orangelabs.wificalling.net.responses.DefaultActivationServerResponse;
import pl.orangelabs.wificalling.service.activation_service.ActivationServerListener;
import pl.orangelabs.wificalling.sip.SipService;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.sip.SipSettings;
import pl.orangelabs.wificalling.util.OLPPhoneNumberUtils;
import pl.orangelabs.wificalling.util.SharedPrefs;
import pl.orangelabs.wificalling.util.VpnInit;
import pl.orangelabs.wificalling.view.ActivityIncomingCall;
import pl.orangelabs.wificalling.view.activation_client.ActivityActivationInit;

public class MainActivityTest extends AppCompatActivity implements VpnStateService.VpnStateListener, ActivationServerListener<DefaultActivationServerResponse,DefaultActivationServerRequest>
{
    public static final boolean USE_BYOD = true;
    public static final String START_PROFILE = "org.strongswan.android.action.START_PROFILE";
    private static final int PREPARE_VPN_SERVICE = 0;
    private static final int PERMISSION_RECORD_AUDIO = 10;
    private static final int PERMISSION_READ_CONTACT = 11;
    private static final int PERMISSION_READ_CAL_LOG = 12;
//    private static final String RANDOM_ALPHABET = "aąbcćdeęfghijklłmnńoópqrsśtuvwxyzźż";
//    private static final Random RANDOM = new Random();

//    /*
//     * The libraries are extracted to /data/data/org.strongswan.android/...
//     * during installation.  On newer releases most are loaded in JNI_OnLoad.
//     */
//    static
//    {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2)
//        {
//            System.loadLibrary("strongswan");
//
//            if (MainActivityTest.USE_BYOD)
//            {
//                System.loadLibrary("tncif");
//                System.loadLibrary("tnccs");
//                System.loadLibrary("imcv");
//            }
//
//            System.loadLibrary("charon");
//            System.loadLibrary("ipsec");
//        }
//        System.loadLibrary("androidbridge");
//        System.loadLibrary("pjsua2");
//    }

    private final VpnInit mVpnInit = new VpnInit();
    private final VpnInit.IVpnInitCallback mVpnInitCallback = new VpnInit.IVpnInitCallback()
    {
        @Override
        public void startActivity(final Intent intent, final int requestCode)
        {
            MainActivityTest.this.startActivityForResult(intent, requestCode);
        }

        @Override
        public void startService(final Intent intent)
        {
            MainActivityTest.this.startService(intent);
        }

        @Override
        public Context ctx()
        {
            return MainActivityTest.this;
        }
    };
    private VpnStateService mService;
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            Log.i(this, "Service Connected");
            mService = ((VpnStateService.LocalBinder) service).getService();
            mService.registerListener(MainActivityTest.this);

            if (START_PROFILE.equals(getIntent().getAction()))
            {
                mVpnInit.onVpnProfileSelected(new VpnInitCallback(), new VpnProfile());
            }
        }
    };

    private TextView mTv;
    private Button mBtn;
    private SharedPrefs mPrefs;
    private TextView mUSSDMessage;
    private long startTime = 0;
    private BroadcastReceiver mBluetoothReceiver;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_test);

        mPrefs = new SharedPrefs(this);


        findViewById(R.id.tmpidIncCall).setOnClickListener(v -> {
            startActivity(new Intent(MainActivityTest.this, ActivityIncomingCall.class));
            finish();
        });

        mTv = (TextView) findViewById(R.id.tmpid2);
        mBtn = (Button) findViewById(R.id.tmpid);
        mBtn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(final View view)
            {
                if (Connected())
                {
                    Log.i(this, "Stopping charon");
                    mService.disconnect();
                }
                else
                {
                    Log.i(this, "Starting charon");
                    mVpnInit.onVpnProfileSelected(mVpnInitCallback, new VpnProfile());
                }
            }
        });


        RequestPermission();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();


        BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener()
        {
            public void onServiceConnected(int profile, BluetoothProfile proxy)
            {
                if (profile == BluetoothProfile.A2DP)
                {
                    boolean deviceConnected = false;
                    BluetoothA2dp btA2dp = (BluetoothA2dp) proxy;
                    List<BluetoothDevice> a2dpConnectedDevices = btA2dp.getConnectedDevices();
                    Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();


                    if (a2dpConnectedDevices.size() != 0)
                    {
                        for (BluetoothDevice device : bondedDevices)
                        {

                            deviceConnected = true;
                            Log.d(this, "DEVICE NAME:" + device.getName());

                        }
                    }
                    if (!deviceConnected)
                    {
                        Toast.makeText(MainActivityTest.this, "DEVICE NOT CONNECTED" + a2dpConnectedDevices.size(), Toast.LENGTH_SHORT).show();
                    }
                    bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, btA2dp);
                }
            }

            public void onServiceDisconnected(int profile)
            {
                // TODO
            }
        };

        bluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.A2DP);

        BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                String action = intent.getAction();
                if (action.equals(BluetoothDevice.ACTION_FOUND))
                {
                    Toast.makeText(getApplicationContext(), "BT found", Toast.LENGTH_SHORT).show();
                }
                else if (action.equals(BluetoothDevice.ACTION_ACL_CONNECTED))
                {
                    Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
                    for (BluetoothDevice device : bondedDevices
                        )
                    {

                        Log.d(this, "class of the device is: " + device.getName() + " : " + device.getBluetoothClass().getMajorDeviceClass());
//                        bluetoothAdapter.getProfileProxy(context, mProfileListener, BluetoothProfile.A2DP);


                    }


                    Toast.makeText(getApplicationContext(), "BT Connected", Toast.LENGTH_SHORT).show();
                }
                else if (action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED))
                {
                    Toast.makeText(getApplicationContext(), "BT Disconnected", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "BT Disconnect requested", Toast.LENGTH_SHORT).show();
                }

            }
        };


        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(mBluetoothReceiver, filter);


        this.bindService(new Intent(this, VpnStateService.class), mServiceConnection, Service.BIND_AUTO_CREATE);


        findViewById(R.id.tmpidSipReg).setOnClickListener(view ->
            startActivity(new Intent(this, ActivityLogs.class))
      //  SipServiceCommand.regAccount(MainActivityTest.this)
        );

        findViewById(R.id.tmpidSipUnrReg)
            .setOnClickListener(view -> SipServiceCommand.unRegAccount(MainActivityTest.this, SipService.DE_INIT_PLACES.OTHER.ordinal()));

        //String phone = Uri.encode("555|43243");
        String phoneNo = "55542341@#$#$@!$U)(*_)(+_)fdsfs";
        findViewById(R.id.tmpSipCall).setOnClickListener(view -> {

                String number = OLPPhoneNumberUtils.SimpleNumber("509384517", Locale.getDefault().getCountry());
                String numberplus = OLPPhoneNumberUtils.SimpleNumber("+42509384517", Locale.getDefault().getCountry());


                Log.d(this, "phone number is " + phoneNo + ", and after=" + phoneNo.replaceAll(SipSettings.SIP_REGEX, ""));
                Log.d(this, "normalizeNumber is " + number + " , " + numberplus);

                // SipServiceCommand.makeCall(MainActivityTest.this, phone);

            }
        );
        findViewById(R.id.btn_provisioning).setOnClickListener(v -> makeProvisioningRequest());
        findViewById(R.id.btn_new_certificate).setOnClickListener(v -> makeNewCertificateRequest());
        findViewById(R.id.btn_new_password).setOnClickListener(v -> makeNewPasswordRequest());
        findViewById(R.id.button3).setOnClickListener(v -> startActivity(new Intent(MainActivityTest.this, ActivityActivationInit.class)));


        CheckWiFiVolteCalling();
    }

    private void CheckWiFiVolteCalling()
    {
        int retVolte = -1;
        int retWifi = -1;

        TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        try
        {
            Method isVolteAvailable = TelephonyManager.class.getMethod("isVolteAvailable");
            retVolte = (boolean) isVolteAvailable.invoke(manager) ? 1 : 0;
        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        try
        {
            Method isWifiCallingAvailable = TelephonyManager.class.getMethod("isWifiCallingAvailable");
            retWifi = (boolean) isWifiCallingAvailable.invoke(manager) ? 1 : 0;

        }
        catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
        catch (InvocationTargetException e)
        {
            e.printStackTrace();
        }
        catch (IllegalAccessException e)
        {
            e.printStackTrace();
        }

        String resultVolte = retVolte == 1 ? "available" : retVolte == 0 ? "not available" : "faild";
        String resultWifi = retWifi == 1 ? "available" : retWifi == 0 ? "not available" : "faild";

        TextView textView = (TextView) findViewById(R.id.vifiVolteState);
        textView.setText(getString(R.string.volteState, resultVolte, resultWifi));
    }

    private void readContact()
    {
//        AsyncHelper.execute(new AsyncGetT1000Contacts(this, new OnLoadEntriesEnd()));
//        final List<T1000Entry> entries = new ArrayList<>();
//        startTime = System.currentTimeMillis();
//        // TODO: 2016-09-01 add permission manually
//        android.util.Log.d("readContact", "start read");
    }

    @Override
    protected void onResume()

    {

        super.onResume();
        readContact();
        App.getActivationComponent().registerListener(this);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        App.getActivationComponent().unRegisterListener(this);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mService.disconnect();
        this.unbindService(mServiceConnection);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults)
    {
        switch (requestCode)
        {
            case PERMISSION_RECORD_AUDIO:
            {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                }
                else
                {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        switch (requestCode)
        {
            case PREPARE_VPN_SERVICE:
                if (resultCode == RESULT_OK)
                {
                    Log.i(this, "STARTING SERVICE");
                    Intent intent = new Intent(this, CharonVpnService.class);
                    intent.putExtras(new Bundle());
                    this.startService(intent);
                }
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void stateChanged()
    {
        if (mService != null)
        {
            mTv.setText("STATE: " + mService.getState() + "\nERROR: " + mService.getErrorState() + "\nIMC: " + mService.getImcState());
            if (Connected())
            {

                mBtn.setText("stop");


                //      SipLib.getInstance().Initialize();

            }
            else
            {
                mBtn.setText("start");


                if (mService.getState() == VpnStateService.State.DISABLED)
                {
                    Log.d(this, "deinit");
                    //            SipLib.getInstance().DeInitialize();
                }
            }
        }
        else
        {
            mTv.setText("STATE: NONE");
        }

    }

    @SuppressLint("SetTextI18n")
    private void makeProvisioningRequest()
    {
        ((TextView) findViewById(R.id.tv_results)).setText("start provisioning");
        ((TextView) findViewById(R.id.tv_request)).setText("");
        App.getActivationComponent().activateAccount(true);
    }

    @SuppressLint("SetTextI18n")
    private void makeNewCertificateRequest()
    {
        ((TextView) findViewById(R.id.tv_results)).setText("start new Certificate");
        ((TextView) findViewById(R.id.tv_request)).setText("");
        App.getActivationComponent().generateNewCertificate(true);
    }

    @SuppressLint("SetTextI18n")
    private void makeNewPasswordRequest()
    {
        ((TextView) findViewById(R.id.tv_results)).setText("start new Password");
        ((TextView) findViewById(R.id.tv_request)).setText("");
        App.getActivationComponent().generateNewPassword(true);
    }

    private boolean Connected()
    {
        return mService != null && mService.getState() == VpnStateService.State.CONNECTED;
    }

    private void RequestPermission()
    {
        ActivityCompat.requestPermissions(MainActivityTest.this,
            new String[] {Manifest.permission.RECORD_AUDIO,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.WRITE_CALL_LOG},
            PERMISSION_RECORD_AUDIO);

        if (ContextCompat.checkSelfPermission(MainActivityTest.this,
            Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED)
        {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivityTest.this,
                Manifest.permission.RECORD_AUDIO))
            {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            }
            else
            {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivityTest.this,
                    new String[] {Manifest.permission.RECORD_AUDIO},
                    PERMISSION_RECORD_AUDIO);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ContextCompat.checkSelfPermission(MainActivityTest.this,
            Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED)
        {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivityTest.this,
                Manifest.permission.READ_CONTACTS))
            {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            }
            else
            {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivityTest.this,
                    new String[] {Manifest.permission.READ_CONTACTS},
                    PERMISSION_READ_CONTACT);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

        if (ActivityCompat.checkSelfPermission(MainActivityTest.this, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED)
        {
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivityTest.this,
                Manifest.permission.READ_CALL_LOG))
            {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            }
            else
            {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(MainActivityTest.this,
                    new String[] {Manifest.permission.READ_CALL_LOG},
                    PERMISSION_READ_CAL_LOG);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }


    }

    @Override
    public void onActivatedAccount()
    {
        Toast.makeText(getApplicationContext(), "SMS has came", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onActivationAwaiting(ApiResponse<DefaultActivationServerResponse, DefaultActivationServerRequest> mDefaultResponse)
    {
        showResponse(mDefaultResponse);
    }

    @Override
    public void onActivationFailed(ApiResponse<DefaultActivationServerResponse, DefaultActivationServerRequest> mDefaultResponse)
    {
        showResponse(mDefaultResponse);
    }

    @Override
    public void onNewPasswordSuccess()
    {
        Toast.makeText(getApplicationContext(), "SMS has came", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onNewPasswordAwaiting(ApiResponse<DefaultActivationServerResponse, DefaultActivationServerRequest> mDefaultResponse)
    {
        showResponse(mDefaultResponse);
    }

    @Override
    public void onNewPasswordFailed(ApiResponse<DefaultActivationServerResponse, DefaultActivationServerRequest> mDefaultResponse)
    {
        showResponse(mDefaultResponse);
    }

    @Override
    public void onNewCertificateSuccess(ApiResponse<DefaultActivationServerResponse, DefaultActivationServerRequest> mDefaultResponse)
    {
        showResponse(mDefaultResponse);
    }

    @Override
    public void onNewCertificateFailed(ApiResponse<DefaultActivationServerResponse, DefaultActivationServerRequest> mDefaultResponse)
    {
        showResponse(mDefaultResponse);
    }

    private void showResponse(ApiResponse<DefaultActivationServerResponse, DefaultActivationServerRequest> mDefaultResponse)
    {
        Toast.makeText(getApplicationContext(), "end " + mDefaultResponse.mDefaultRequest.getClass(), Toast.LENGTH_SHORT).show();
        ((TextView) findViewById(R.id.tv_results)).setText(mDefaultResponse.mDefaultRequest.getClass() + " response " + mDefaultResponse.toString());
        ((TextView) findViewById(R.id.tv_request)).setText(mDefaultResponse.mDefaultRequest.toString());
    }


    private class VpnInitCallback implements VpnInit.IVpnInitCallback
    {
        @Override
        public void startActivity(final Intent intent, final int requestCode)
        {
            MainActivityTest.this.startActivityForResult(intent, requestCode);
        }

        @Override
        public void startService(final Intent intent)
        {
            MainActivityTest.this.startService(intent);
        }

        @Override
        public Context ctx()
        {
            return MainActivityTest.this;
        }
    }


//    private class OnLoadEntriesEnd implements AsyncGetT1000Contacts.OnContactDetailResultCallback
//    {
//        @Override
//        public void onLoadCompleteListener(final List<T1000Entry> entryList)
//        {
//            long stopTime = System.currentTimeMillis();
//
//            Log.d(this, "OnLoadEntriesEnd time: " + (stopTime - startTime));
//            if (entryList == null)
//            {
//                return;
//            }
//            Collator collator = Collator.getInstance(Locale.getDefault());
//            MemoryCache.Me().Save(new T1000Dictionary.T1000DataSet(Stream.of(entryList)
//                .sorted((lhs, rhs) -> collator.compare(lhs.displayableName(), rhs.displayableName()))
//                .collect(Collectors.toList())));
//        }
//    }
}


