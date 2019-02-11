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

package pl.orangelabs.wificalling.service.activation_service;

/**
 * Created by marcin on 20.01.17.
 */

public interface ActivationDataKeeper
{
    void saveVPNPassword(String password);
    String loadVPNPassword();

    void saveSIPPassword(String password);
    String loadSIPPassword();

    String loadHashOldPassword();

    void saveUsersNumber(String userNumber);
    String loadUserNumber();

    void saveImsiConnectedWithPassword(String imei);

    String loadImsi();

    ActivationState loadState();

    void saveState(ActivationState activationState);

    void saveActivationDate(long date);
    long loadActivationDate();

    void saveCertificateExpirationDate();
    long loadCertificateExpirationDate();

    void clearActivation();

}
