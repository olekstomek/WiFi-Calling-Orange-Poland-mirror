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

package pl.orangelabs.wificalling;

import android.annotation.SuppressLint;
import android.content.Context;
import android.telephony.TelephonyManager;

import pl.orangelabs.wificalling.service.activation_service.ActivationDataKeeper;
import pl.orangelabs.wificalling.service.activation_service.ActivationDataKeeperDefault;

public class VpnProfileDataSource
{
	public VpnProfileDataSource()
	{

	}


	@SuppressLint("HardwareIds")
	public VpnProfile createUser(final Context context)
	{
		VpnProfile profile = new VpnProfile();
		if (BuildConfig.CONFIG_PROD)
		{
			TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
			ActivationDataKeeper activationDataKeeper = new ActivationDataKeeperDefault(context);
			profile.setGateway(SettingsApp.ipsecGateway);
			profile.setPassword(activationDataKeeper.loadVPNPassword());
			profile.setRemoteId(SettingsApp.ipsecRemoteId);
			profile.setLocalId(context.getString(R.string.local_id_begin) + telephonyManager.getSubscriberId() + context.getString(R.string.local_id_end));
			profile.setMTU(1500);
			profile.setPort(null);
			profile.setSplitTunneling(null);
			profile.setUserCertificateAlias(null);
			profile.setCertificateAlias(null);
			profile.setName(context.getString(R.string.vpn_name));
			profile.setUsername(context.getString(R.string.user_name));
			profile.setVpnType(VpnType.IKEV2_CERT_EAP);
			profile.setAaaIdentity(context.getString(R.string.vpn_identity));
		} else
		{
			profile.setGateway(SettingsApp.ipsecGateway);
			profile.setPassword(SettingsApp.ipsecPassword);
			profile.setRemoteId(SettingsApp.ipsecRemoteId);
			profile.setLocalId(SettingsApp.ipsecLocalId);
			profile.setMTU(1500);
			profile.setPort(null);
			profile.setSplitTunneling(null);
			profile.setUserCertificateAlias(null);
			profile.setCertificateAlias(null);
			profile.setName(context.getString(R.string.vpn_name));
			profile.setUsername("username");
			profile.setVpnType(VpnType.IKEV2_CERT_EAP);
//            profile.setAaaIdentity("C=PL, L=Warsaw, O=Orange, CN=aaa.nai.epc.mnc003.mcc260.3gppnetwork.org");
			//profile.setAaaIdentity(context.getString(R.string.vpn_identity));
		}

		return profile;
	}

	/**
	 * Get a single VPN profile from the database.
	 *
	 * @return the profile or null, if not found
	 */
	public VpnProfile getVpnProfile(Context context)
	{
		return createUser(context);
	}
}