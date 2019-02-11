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
import android.provider.*;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import com.google.i18n.phonenumbers.ShortNumberInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.App;

import static android.provider.Settings.Global.AIRPLANE_MODE_ON;

/**
 * Created by Cookie on 2015-02-25.
 */
public class OLPPhoneNumberUtils
{
    private static final ArrayList<String>
        emergencyNumbers =
        new ArrayList<>(Arrays
            .asList("999", "998", "997", "996", "995", "994", "993", "992", "991", "987", "985", "984", "981", "986", "19252", "19285",
                "601100100", "601100300", "133"));
    // 112 will handle in another place
    public static String formatNumber(String phoneNumber, String defaultCountryIso)
    {
        // Do not attempt to format numbers that start with a hash or star symbol.

        if (phoneNumber != null)
        {
            if (phoneNumber.startsWith("#") || phoneNumber.startsWith("*"))
            {
                return phoneNumber;
            }

            PhoneNumberUtil util = PhoneNumberUtil.getInstance();
            String result = null;
            try
            {
                Phonenumber.PhoneNumber pn = util.parseAndKeepRawInput(phoneNumber, defaultCountryIso);
                result = util.formatInOriginalFormat(pn, defaultCountryIso);
            }
            catch (NumberParseException ignore)
            {
                Log.d("Ignore it", ignore.toString());
            }
            return result;
        }
        return "";
    }

    public static String formatNumberToE164(String number, String defaultCountryIso)
    {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        String result = null;
        try
        {
            Phonenumber.PhoneNumber pn = util.parse(number, defaultCountryIso);
            if (util.isValidNumber(pn))
            {
                result = util.format(pn, PhoneNumberUtil.PhoneNumberFormat.E164);
            }
        }
        catch (NumberParseException ignore)
        {
            Log.d("Ignore it", ignore.toString());
        }
        return result;
    }

    public static String SimpleNumber(String number, String defaultCountryIso)
    {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        String result = number;
        try
        {
            Phonenumber.PhoneNumber pn = util.parse(number, defaultCountryIso);
            //  if (util.isValidNumber(pn))
            {
                result = String.valueOf(pn.getNationalNumber());
            }
        }
        catch (NumberParseException ignore)
        {
        }
        return result;
    }

    public static String normalizeNumber(String number)
    {
        if (TextUtils.isEmpty(number))
        {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int len = number.length();
        for (int i = 0; i < len; i++)
        {
            char c = number.charAt(i);
            // Character.digit() supports ASCII and Unicode digits (full width, Arabic-Indic, etc.)
            int digit = Character.digit(c, 10);
            if (digit != -1)
            {
                sb.append(digit);
            }
            else if (i == 0 && c == '+')
            {
                sb.append(c);
            }
            else if (c == '*' || c == '#')
            {
                sb.append(c);
            }
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'))
            {
                return normalizeNumber(PhoneNumberUtils.convertKeypadLettersToDigits(number));
            }
        }
        return sb.toString();
    }

    public static int GetPhoneType(String number, String defaultCountryIso)
    {
        PhoneNumberUtil util = PhoneNumberUtil.getInstance();
        PhoneNumberUtil.PhoneNumberType type = PhoneNumberUtil.PhoneNumberType.UNKNOWN;
        try
        {
            Phonenumber.PhoneNumber pn = util.parse(number, defaultCountryIso);
            type = util.getNumberType(pn);
        }
        catch (NumberParseException ignore)
        {
        }

        return ParsePhoneNumberUtilTypeToCommonDataKindsPhoneType(type);
    }

    private static int ParsePhoneNumberUtilTypeToCommonDataKindsPhoneType(PhoneNumberUtil.PhoneNumberType phoneNumberType)
    {
        switch (phoneNumberType)
        {
            case FIXED_LINE:
                return ContactsContract.CommonDataKinds.Phone.TYPE_HOME;
            case MOBILE:
                return ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE;
            case PAGER:
                return ContactsContract.CommonDataKinds.Phone.TYPE_PAGER;
            case UNKNOWN:
            case FIXED_LINE_OR_MOBILE:
            case TOLL_FREE:
            case PREMIUM_RATE:
            case SHARED_COST:
            case PERSONAL_NUMBER:
            case VOIP:
            case UAN:
            case VOICEMAIL:
            default:
                return ContactsContract.CommonDataKinds.Phone.TYPE_OTHER;
        }
    }

    /**
     * number must be from Emergency List or 112 and service not in out of service
     * @param num
     * @return
     */
    public static boolean shouldCallEmergencyNativeFromOutsideApp(String num)
    {
        return isOnEmergencyListWithout112(num)|| (!App.getMobileServiceStateHelper().isMobileOutOfService() && num.equals("112"));
    }

    /**
     * number must be from Emergency list and service must be on
     * @param num
     * @return
     */
    public static boolean shouldCallEmergencyNativeFromApp(String num)
    {
        return  isOnEmergencyListWithout112(num) || App.getMobileServiceStateHelper().is112onMobileOn(num) ;
    }
    public static boolean isOnEmergencyListWithout112(String number)
    {
        return (!number.equals("112") && IsOnEmergencyList(number));
    }
    public static boolean IsOnEmergencyList(String num)
    {
        ShortNumberInfo util = ShortNumberInfo.getInstance();
        return util.isEmergencyNumber(num, Locale.getDefault().getCountry()) || emergencyNumbers.contains(num);
    }

    public static boolean isUSSDNumber(String number)
    {
        return number != null && number.startsWith("*") && number.endsWith("#");
    }
    public static boolean isAirplaneModeON(Context context)
    {
       return android.provider.Settings.System.getInt(context.getContentResolver(), AIRPLANE_MODE_ON,
                0) != 0;
    }
    public static boolean isPhoneNetworkON(Context context)
    {
        return ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getNetworkType()
                != TelephonyManager.NETWORK_TYPE_UNKNOWN;
    }
}
