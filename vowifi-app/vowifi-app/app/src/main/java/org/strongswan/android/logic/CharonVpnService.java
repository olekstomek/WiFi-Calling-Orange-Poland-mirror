/*
 * Copyright (C) 2012-2016 Tobias Brunner
 * Copyright (C) 2012 Giuliano Grassi
 * Copyright (C) 2012 Ralf Sager
 * HSR Hochschule fuer Technik Rapperswil
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 2 of the License, or (at your
 * option) any later version.  See <http://www.fsf.org/copyleft/gpl.txt>.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 */

package org.strongswan.android.logic;

import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.security.KeyChainException;
import android.system.OsConstants;

import org.pjsip.pjsua2.pjsua_state;

import java.io.File;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.ImcState;
import pl.orangelabs.wificalling.R;
import pl.orangelabs.wificalling.RemediationInstruction;
import pl.orangelabs.wificalling.SettingsApp;
import pl.orangelabs.wificalling.SettingsWriter;
import pl.orangelabs.wificalling.VpnProfile;
import pl.orangelabs.wificalling.VpnProfileDataSource;
import pl.orangelabs.wificalling.VpnStateService;
import pl.orangelabs.wificalling.VpnStateService.ErrorState;
import pl.orangelabs.wificalling.VpnStateService.State;
import pl.orangelabs.wificalling.VpnType;
import pl.orangelabs.wificalling.service.activation_service.KeystoreHandling;
import pl.orangelabs.wificalling.service.connection_service.ConnectionService;
import pl.orangelabs.wificalling.sip.BroadcastSipReceiver;
import pl.orangelabs.wificalling.sip.SipMessagesReceiver;
import pl.orangelabs.wificalling.sip.SipService;
import pl.orangelabs.wificalling.sip.SipServiceCommand;
import pl.orangelabs.wificalling.util.OLPNotificationBuilder;
import pl.orangelabs.wificalling.view.ActivitySettings;

import static pl.orangelabs.wificalling.util.OLPNotificationBuilder.NOTIFICATION_MAIN;

public class CharonVpnService extends VpnService implements Runnable, VpnStateService.VpnStateListener
{
	private static final String TAG = CharonVpnService.class.getSimpleName();
	public static final String LOG_FILE = "charon.log";
	public static final int VPN_STATE_NOTIFICATION_ID = 1;

	private String mLogFile;
	private VpnProfileDataSource mDataSource;
	private Thread mConnectionHandler;
	private VpnProfile mCurrentProfile;

	private VpnProfile mNextProfile;
	private volatile boolean mProfileUpdated;
	private volatile boolean mTerminate;
	private volatile boolean mIsDisconnecting;
	public static String sLockalAddress = "";
	public static String sSIPAddress = "";
	private X509Certificate mRootCACache;
	private BroadcastSipReceiver mSipReceiver;
	private VpnStateService mService;
	private final Object mServiceLock = new Object();
	private final ServiceConnection mServiceConnection = new ServiceConnection() {
		@Override
		public void onServiceDisconnected(ComponentName name)
		{	/* since the service is local this is theoretically only called when the process is terminated */
			synchronized (mServiceLock)
			{
				mService = null;
			}
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service)
		{
			synchronized (mServiceLock)
			{
				mService = ((VpnStateService.LocalBinder)service).getService();
			}
			/* we are now ready to start the handler thread */
			mService.registerListener(CharonVpnService.this);
			mConnectionHandler.start();
		}
	};

	/**
	 * as defined in charonservice.h
	 */
	static final int STATE_CHILD_SA_UP = 1;
	static final int STATE_CHILD_SA_DOWN = 2;
	static final int STATE_AUTH_ERROR = 3;
	static final int STATE_PEER_AUTH_ERROR = 4;
	static final int STATE_LOOKUP_ERROR = 5;
	static final int STATE_UNREACHABLE_ERROR = 6;
	static final int STATE_GENERIC_ERROR = 7;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent != null)
		{
			Bundle bundle = intent.getExtras();
			VpnProfile profile = null;
			if (bundle != null)
			{
				profile = mDataSource.getVpnProfile(getApplicationContext());
			}
			setNextProfile(profile);
		}
		else
		{
			Log.i(this, "START intent null");
			setNextProfile(null);
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onCreate()
	{
		mLogFile = getFilesDir().getAbsolutePath() + File.separator + LOG_FILE;

		mDataSource = new VpnProfileDataSource();
		/* use a separate thread as main thread for charon */
		mConnectionHandler = new Thread(this);

		createRootCA();
		if (ConnectionService.isServiceON(getApplicationContext()))
		{
			mSipReceiver = new BroadcastSipReceiver(new CharonInternalSipMessageReceiver());
			mSipReceiver.RegisterSipReceiver(this);
			startService(new Intent(this, SipService.class));
		}
		/* the thread is started when the service is bound */
		bindService(new Intent(this, VpnStateService.class),
					mServiceConnection, Service.BIND_AUTO_CREATE);
	}
	private void createRootCA()
	{
		final InputStream rootCAStream = getResources().openRawResource(R.raw.cert);
		try
		{
			Certificate rootCA = CertificateFactory.getInstance("X.509").generateCertificate(rootCAStream);
			if (!(rootCA instanceof X509Certificate))
			{
				throw new CertificateException("Cert isn't x509?");
			}
			mRootCACache = (X509Certificate) rootCA;
		}
		catch (CertificateException e)
		{
			Log.d(this, "App error", e);
		}
	}
	public static void stopVPN(Context context)
	{
		Intent intent = new Intent(context, CharonVpnService.class);
		context.startService(intent);
	}

	@Override
	public void onRevoke()
	{	/* the system revoked the rights grated with the initial prepare() call.
		 * called when the user clicks disconnect in the system's VPN dialog */
		ConnectionService.turnServiceOFF(getApplicationContext());
	}

	@Override
	public void onDestroy()
	{
		mTerminate = true;
		setNextProfile(null);
		try
		{
			mConnectionHandler.join();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		if (mService != null)
		{
			mService.unregisterListener(this);
			unbindService(mServiceConnection);
		}
	}

	/**
	 * Set the profile that is to be initiated next. Notify the handler thread.
	 *
	 * @param profile the profile to initiate
	 */
	private void setNextProfile(VpnProfile profile)
	{
		synchronized (this)
		{
			this.mNextProfile = profile;
			mProfileUpdated = true;
			notifyAll();
		}
	}

	@Override
	public void run()
	{
		while (true)
		{
			synchronized (this)
			{
				try
				{
					while (!mProfileUpdated)
					{
						wait();
					}

					mProfileUpdated = false;
					onCharonStop();
					if (mNextProfile == null)
					{
						setState(State.DISABLED);
						if (mTerminate)
						{
							break;
						}
					}
					else
					{
						mCurrentProfile = mNextProfile;
						mNextProfile = null;

						/* store this in a separate (volatile) variable to avoid
						 * a possible deadlock during deinitialization */

						startConnection(mCurrentProfile);
						mIsDisconnecting = false;
						BuilderAdapter builder = new BuilderAdapter(mCurrentProfile.getName(), mCurrentProfile.getSplitTunneling());
						if (initializeCharon(builder, mLogFile, mCurrentProfile.getVpnType().has(VpnType.VpnTypeFeature.BYOD)))
						{
							Log.i(TAG, "charon started");
							SettingsWriter writer = new SettingsWriter();
							writer.setValue("global.language", Locale.getDefault().getLanguage());
							writer.setValue("global.mtu", mCurrentProfile.getMTU());
							writer.setValue("connection.type", mCurrentProfile.getVpnType().getIdentifier());
							writer.setValue("connection.server", mCurrentProfile.getGateway());
							writer.setValue("connection.port", mCurrentProfile.getPort());
							writer.setValue("connection.username", mCurrentProfile.getUsername());
							writer.setValue("connection.password", mCurrentProfile.getPassword());
							writer.setValue("connection.local_id", mCurrentProfile.getLocalId());
							writer.setValue("connection.remote_id", mCurrentProfile.getRemoteId());
							writer.setValue("connection.aaa_identity", mCurrentProfile.getAaaIdentity());
							initiate(writer.serialize());
						}
						else
						{
							Log.e(TAG, "failed to start charon");
							setError(ErrorState.GENERIC_ERROR);
							mCurrentProfile = null;
						}
					}
				}
				catch (InterruptedException ex)
				{
					stopCurrentConnection();
					setState(State.DISABLED);
				}
			}
		}
	}

	/**
	 * Stop any existing connection by deinitializing charon.
	 */
	private void stopCurrentConnection()
	{
		Log.d(this, "stopCurrentConnection()");
		SipServiceCommand.deInitialize(this, SipService.DE_INIT_PLACES.CHARON_STOP.ordinal());
	}
	private void onCharonRevoke()
	{
		Log.d(this, "onCharonRevoke()");
		setNextProfile(null);
	}

	private void onCharonStop()
	{
		synchronized (this)
		{
			if (mCurrentProfile != null)
			{
				setState(VpnStateService.State.DISCONNECTING);
				mIsDisconnecting = true;
				deinitializeCharon();
				Log.i(this, "charon stopped");
				mCurrentProfile = null;
			}
		}
	}
	private void onCharonDestroy()
	{
		Log.d(this, "onCharonDestroy()");
		stopService(new Intent(CharonVpnService.this, SipService.class));
		mTerminate = true;
		setNextProfile(null);
		try
		{
			mConnectionHandler.join();
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
		if (mService != null)
		{
			unbindService(mServiceConnection);
		}

		mSipReceiver.UnRegisterSipReceiver(CharonVpnService.this);
	}
	private class CharonInternalSipMessageReceiver extends SipMessagesReceiver
	{
		@Override
		public void OnStackState(final pjsua_state state, final SipService.DE_INIT_PLACES from)
		{
			if (state == pjsua_state.PJSUA_STATE_NULL)
			{
				if (from.equals(SipService.DE_INIT_PLACES.CHARON_REVOKE))
				{
					onCharonRevoke();
				}
				else if (from.equals(SipService.DE_INIT_PLACES.CHARON_DESTROY))
				{
					onCharonDestroy();
				}
				else if (from.equals(SipService.DE_INIT_PLACES.CHARON_STOP))
				{
					onCharonStop();
				}
			}
		}
	}
	/**
	 * Add a permanent notification while we are connected to avoid the service getting killed by
	 * the system when low on memory.
	 */
	private void addNotification()
	{
		OLPNotificationBuilder mNotificationBuilder = new OLPNotificationBuilder(getApplicationContext());
		startForeground(NOTIFICATION_MAIN, mNotificationBuilder.GetMainNotification(ConnectionService.getConnectionState()));
	}

	@Override
	public void stateChanged() {

	}

	/**
	 * Notify the state service about a new connection attempt.
	 * Called by the handler thread.
	 *
	 * @param profile currently active VPN profile
	 */
	private void startConnection(VpnProfile profile)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				mService.startConnection(profile);
			}
		}
	}

	/**
	 * Update the current VPN state on the state service. Called by the handler
	 * thread and any of charon's threads.
	 *
	 * @param state current state
	 */
	private void setState(State state)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				mService.setState(state);
			}
		}
	}

	/**
	 * Set an error on the state service. Called by the handler thread and any
	 * of charon's threads.
	 *
	 * @param error error state
	 */
	private void setError(ErrorState error)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				mService.setError(error);
			}
		}
	}

	/**
	 * Set the IMC state on the state service. Called by the handler thread and
	 * any of charon's threads.
	 *
	 * @param state IMC state
	 */
	private void setImcState(ImcState state)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				mService.setImcState(state);
			}
		}
	}

	/**
	 * Set an error on the state service. Called by the handler thread and any
	 * of charon's threads.
	 *
	 * @param error error state
	 */
	private void setErrorDisconnect(ErrorState error)
	{
		synchronized (mServiceLock)
		{
			if (mService != null)
			{
				if (!mIsDisconnecting)
				{
					mService.setError(error);
				}
			}
		}
	}

	/**
	 * Updates the state of the current connection.
	 * Called via JNI by different threads (but not concurrently).
	 *
	 * @param status new state
	 */
	public void updateStatus(int status)
	{
		switch (status)
		{
			case STATE_CHILD_SA_DOWN:
				if (!mIsDisconnecting)
				{
					setState(State.CONNECTING);
				}
				break;
			case STATE_CHILD_SA_UP:
				addNotification();
				setState(State.CONNECTED);
				break;
			case STATE_AUTH_ERROR:
				setErrorDisconnect(ErrorState.AUTH_FAILED);
				break;
			case STATE_PEER_AUTH_ERROR:
				setErrorDisconnect(ErrorState.PEER_AUTH_FAILED);
				break;
			case STATE_LOOKUP_ERROR:
				setErrorDisconnect(ErrorState.LOOKUP_FAILED);
				break;
			case STATE_UNREACHABLE_ERROR:
				setErrorDisconnect(ErrorState.UNREACHABLE);
				break;
			case STATE_GENERIC_ERROR:
				setErrorDisconnect(ErrorState.GENERIC_ERROR);
				break;
			default:
				Log.e(TAG, "Unknown status code received");
				break;
		}
	}

	/**
	 * Updates the IMC state of the current connection.
	 * Called via JNI by different threads (but not concurrently).
	 *
	 * @param value new state
	 */
	public void updateImcState(int value)
	{
		ImcState state = ImcState.fromValue(value);
		if (state != null)
		{
			setImcState(state);
		}
	}

	/**
	 * Add a remediation instruction to the VPN state service.
	 * Called via JNI by different threads (but not concurrently).
	 *
	 * @param xml XML text
	 */
	public void addRemediationInstruction(String xml)
	{
		for (RemediationInstruction instruction : RemediationInstruction.fromXml(xml))
		{
			synchronized (mServiceLock)
			{
				if (mService != null)
				{
					mService.addRemediationInstruction(instruction);
				}
			}
		}
	}

	/**
	 * Function called via JNI to generate a list of DER encoded CA certificates
	 * as byte array.
	 *
	 * @return a list of DER encoded CA certificates
	 */
	private byte[][] getTrustedCertificates()
	{
		//olp/ currently we don't use root CA and trust our self-signed cert automatically
		byte[][] encodedCerts = new byte[1][];
		try
		{
			encodedCerts[0] = mRootCACache.getEncoded();
			return encodedCerts;
		}
		catch (CertificateEncodingException e)
		{
			Log.d(this, "App error", e);
		}
		return null;
	}

	/**
	 * Function called via JNI to get a list containing the DER encoded certificates
	 * of the user selected certificate chain (beginning with the user certificate).
	 *
	 * Since this method is called from a thread of charon's thread pool we are safe
	 * to call methods on KeyChain directly.
	 *
	 * @return list containing the certificates (first element is the user certificate)
	 * @throws InterruptedException
	 * @throws KeyChainException
	 * @throws CertificateEncodingException
	 */
	private byte[][] getUserCertificate() throws KeyChainException, InterruptedException, CertificateEncodingException
	{
		ArrayList<byte[]> encodings = new ArrayList<>();
		Certificate[] chain = KeystoreHandling.ReadKeystoreChain(getApplicationContext());
		if (chain == null || chain.length == 0)
		{
			return null;
		}
		for (Certificate cert : chain)
		{
			encodings.add(cert.getEncoded());
		}
		return encodings.toArray(new byte[encodings.size()][]);
	}

	/**
	 * Function called via JNI to get the private key the user selected.
	 *
	 * Since this method is called from a thread of charon's thread pool we are safe
	 * to call methods on KeyChain directly.
	 *
	 * @return the private key
	 * @throws InterruptedException
	 * @throws KeyChainException
	 * @throws CertificateEncodingException
	 */
	private PrivateKey getUserKey() throws KeyChainException, InterruptedException
	{
		return KeystoreHandling.ReadPrivateKey(getApplicationContext());
		//return KeyChain.getPrivateKey(getApplicationContext(), mCurrentUserCertificateAlias);
	}

	/**
	 * Initialization of charon, provided by libandroidbridge.so
	 *
	 * @param builder BuilderAdapter for this connection
	 * @param logfile absolute path to the logfile
	 * @param byod enable BYOD features
	 * @return TRUE if initialization was successful
	 */
	@SuppressWarnings("JniMissingFunction")
	public native boolean initializeCharon(BuilderAdapter builder, String logfile, boolean byod);

	/**
	 * Deinitialize charon, provided by libandroidbridge.so
	 */
	@SuppressWarnings("JniMissingFunction")
	public native void deinitializeCharon();

	/**
	 * Initiate VPN, provided by libandroidbridge.so
	 */
	@SuppressWarnings("JniMissingFunction")
	public native void initiate(String config);

	/**
	 * Adapter for VpnService.Builder which is used to access it safely via JNI.
	 * There is a corresponding C object to access it from native code.
	 */
	public class BuilderAdapter
	{
		private final String mName;
		private final Integer mSplitTunneling;
		private VpnService.Builder mBuilder;
		private BuilderCache mCache;
		private BuilderCache mEstablishedCache;

		public BuilderAdapter(String name, Integer splitTunneling)
		{
			mName = name;
			mSplitTunneling = splitTunneling;
			mBuilder = createBuilder(name);
			mCache = new BuilderCache(mSplitTunneling);
		}

		private VpnService.Builder createBuilder(String name)
		{
			VpnService.Builder builder = new CharonVpnService.Builder();
			builder.setSession(mName);

			/* even though the option displayed in the system dialog says "Configure"
			 * we just use our main Activity */
			Context context = getApplicationContext();
			Intent intent = new Intent(context, ActivitySettings.class);
			PendingIntent pending = PendingIntent.getActivity(context, 0, intent,
															  PendingIntent.FLAG_UPDATE_CURRENT);
			builder.setConfigureIntent(pending);
			return builder;
		}

		public synchronized boolean addAddress(String address, int prefixLength)
		{
			try
			{
				mCache.addAddress(address, prefixLength);
				sLockalAddress = address;
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized boolean addDnsServer(String address)
		{
			try
			{
				mBuilder.addDnsServer(address);
				mCache.recordAddressFamily(address);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized boolean addRoute(String address, int prefixLength)
		{
			try
			{
				mCache.addRoute(address, prefixLength);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized boolean addSearchDomain(String domain)
		{
			try
			{
				mBuilder.addSearchDomain(domain);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized boolean setMtu(int mtu)
		{
			try
			{
				mCache.setMtu(mtu);
			}
			catch (IllegalArgumentException ex)
			{
				return false;
			}
			return true;
		}

		public synchronized int establish(final String serverIpBytes)
		{
			final String []ips = serverIpBytes.split(",");
			ParcelFileDescriptor fd;
			try
			{
				sSIPAddress = ips[0]; // TODO at the moment only using first address
				for (final String ip : ips)
				{
					mCache.addRoute(ip, 32);
				}
				mCache.applyData(mBuilder);
				fd = mBuilder.establish();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return -1;
			}
			if (fd == null)
			{
				return -1;
			}
			/* now that the TUN device is created we don't need the current
			 * builder anymore, but we might need another when reestablishing */
			mBuilder = createBuilder(mName);
			mEstablishedCache = mCache;
			mCache = new BuilderCache(mSplitTunneling);
			return fd.detachFd();
		}

		public synchronized int establishNoDns(byte[] serverIpBytes)
		{
			ParcelFileDescriptor fd;

			if (mEstablishedCache == null)
			{
				return -1;
			}
			try
			{
				Builder builder = createBuilder(mName);
				mEstablishedCache.applyData(builder);
				fd = builder.establish();
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
				return -1;
			}
			if (fd == null)
			{
				return -1;
			}
			return fd.detachFd();
		}
	}

	/**
	 * Cache non DNS related information so we can recreate the builder without
	 * that information when reestablishing IKE_SAs
	 */
	public class BuilderCache
	{
		private final List<PrefixedAddress> mAddresses = new ArrayList<PrefixedAddress>();
		private final List<PrefixedAddress> mRoutesIPv4 = new ArrayList<PrefixedAddress>();
		private final List<PrefixedAddress> mRoutesIPv6 = new ArrayList<PrefixedAddress>();
		private final int mSplitTunneling;
		private int mMtu;
		private boolean mIPv4Seen, mIPv6Seen;

		public BuilderCache(Integer splitTunneling)
		{
			mSplitTunneling = splitTunneling != null ? splitTunneling : 0;
		}

		public void addAddress(String address, int prefixLength)
		{
			mAddresses.add(new PrefixedAddress(address, prefixLength));
			recordAddressFamily(address);
		}

		public void addRoute(String address, int prefixLength)
		{
			try
			{
				if (isIPv6(address))
				{
					mRoutesIPv6.add(new PrefixedAddress(address, prefixLength));
				}
				else
				{
					mRoutesIPv4.add(new PrefixedAddress(address, prefixLength));
				}
			}
			catch (UnknownHostException ex)
			{
				ex.printStackTrace();
			}
		}

		public void setMtu(int mtu)
		{
			mMtu = mtu;
		}

		public void recordAddressFamily(String address)
		{
			try
			{
				if (isIPv6(address))
				{
					mIPv6Seen = true;
				}
				else
				{
					mIPv4Seen = true;
				}
			}
			catch (UnknownHostException ex)
			{
				ex.printStackTrace();
			}
		}

		@TargetApi(Build.VERSION_CODES.LOLLIPOP)
		public void applyData(VpnService.Builder builder)
		{
			for (PrefixedAddress address : mAddresses)
			{
				builder.addAddress(address.mAddress, address.mPrefix);
			}
			/* add routes depending on whether split tunneling is allowed or not,
			 * that is, whether we have to handle and block non-VPN traffic */
			if ((mSplitTunneling & VpnProfile.SPLIT_TUNNELING_BLOCK_IPV4) == 0)
			{
				if (mIPv4Seen)
				{	/* split tunneling is used depending on the routes */
					final Iterator<PrefixedAddress> iterator = mRoutesIPv4.iterator();
					while (iterator.hasNext())
					{
						final PrefixedAddress next = iterator.next();
						if (next.mPrefix < 30)
						{
							iterator.remove();
						}
					}
					for (PrefixedAddress route : mRoutesIPv4)
					{

						Log.d(this, "Adding route: " + route.mAddress + "/" + route.mPrefix);
						builder.addRoute(route.mAddress, route.mPrefix);
						if (!SettingsApp.isProd)
						{
							builder.addRoute("172.18.91.240", 28);
						}
						else
						{
							builder.addRoute("10.80.82.0", 24); //bez tego nie ma dźwięku
							builder.addRoute("10.70.82.0", 24); //bez tego nie ma dźwięku
							builder.addRoute("172.18.92.0", 24); //bez tego nie ma dźwięku
							builder.addRoute("172.18.97.0", 24); //bez tego nie ma dźwięku

//                            builder.addRoute("0.0.0.0", 0); //bez tego nie ma dźwięku
						}
					}
				}
				else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				{	/* allow traffic that would otherwise be blocked to bypass the VPN */
					builder.allowFamily(OsConstants.AF_INET);
				}
			}
			else if (mIPv4Seen)
			{	/* only needed if we've seen any addresses.  otherwise, traffic
				 * is blocked by default (we also install no routes in that case) */
				builder.addRoute("0.0.0.0", 0);
			}
			/* same thing for IPv6 */
			if ((mSplitTunneling & VpnProfile.SPLIT_TUNNELING_BLOCK_IPV6) == 0)
			{
				if (mIPv6Seen)
				{
					for (PrefixedAddress route : mRoutesIPv6)
					{
						builder.addRoute(route.mAddress, route.mPrefix);
					}
				}
				else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
				{
					builder.allowFamily(OsConstants.AF_INET6);
				}
			}
			else if (mIPv6Seen)
			{
				builder.addRoute("::", 0);
			}
            Log.i(this, "Setting mtu: " + mMtu);
            builder.setMtu(mMtu);
            try
            {
                builder.addAllowedApplication(getPackageName());
            }
            catch (PackageManager.NameNotFoundException e)
            {
                Log.d(this, "App error", e);
            }
		}

		private boolean isIPv6(String address) throws UnknownHostException
		{
			InetAddress addr = InetAddress.getByName(address);
			if (addr instanceof Inet4Address)
			{
				return false;
			}
			else if (addr instanceof Inet6Address)
			{
				return true;
			}
			return false;
		}

		private class PrefixedAddress
		{
			public String mAddress;
			public int mPrefix;

			public PrefixedAddress(String address, int prefix)
			{
				this.mAddress = address;
				this.mPrefix = prefix;
			}
		}
	}

	/*
	 * The libraries are extracted to /data/data/org.strongswan.android/...
	 * during installation.  On newer releases most are loaded in JNI_OnLoad.
	 */
	static
	{
		System.loadLibrary("androidbridge");
	}
}
