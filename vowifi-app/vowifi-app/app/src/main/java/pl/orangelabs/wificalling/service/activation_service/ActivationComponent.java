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
 * Created by marcin on 25.01.17.
 */

public interface ActivationComponent
{
    ActivationDataKeeper getActivationDataKeeper();

    void registerListener(ActivationServerListener activationServerListener);
    void unRegisterListener(ActivationServerListener activationServerListener);
    void activateAccount(boolean renew);
    void generateNewPassword(boolean renew);
    void generateNewCertificate(boolean renew);

    RepeatStack getRepeatStack();

    void makeRepeatRequest();

    void cancelRepeatRequest(RepeatModeState repeatModeState);

    void onSMSHasCame(String body);
    ActivationState getActivationState();
    boolean isActive();
    RepeatModeState getRepeatModeState();

    void clearActivation();
}
