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

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.SettingsApp;

/**
 * Created by marcin on 02.02.17.
 */

public class KeystoreHandlingDefaultAccounts
{

    private static final String TMP_KEYSTORE_PASSWORD = "keystore password here";
    //create cert for default accounts
    public static void InitCertFromResources(Context context)
    {
        if (ReadKeystoreCert(context, KeystorePass()) == null)
        {
            Log.i(KeystoreHandlingDefaultAccounts.class, "No cert in keystore... saving");
            try
            {
                final InputStream inputStream = context.getResources().openRawResource(SettingsApp.ipsecKey); //private
//                final InputStream inputStream = getResources().openRawResource(R.raw.cert_key_260038000001236); //private
                int bytes;
                byte[] buffer = new byte[2048];
                final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                while ((bytes = inputStream.read(buffer)) >= 0)
                {
                    byteStream.write(Arrays.copyOfRange(buffer, 0, bytes));
                }
                final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(Base64.decode(byteStream.toByteArray(), Base64.DEFAULT));
                final KeyFactory kf = KeyFactory.getInstance("RSA");
                final PrivateKey privateKey = kf.generatePrivate(spec);
                createKeystoreCert(context , context.getResources().openRawResource(SettingsApp.ipsecCert), privateKey, KeystorePass()); //public
            }
            catch (Exception e)
            {
                Log.d(KeystoreHandlingDefaultAccounts.class, "App error", e);
            }
        }
    }
    public static char[] KeystorePass()
    {
        return (TMP_KEYSTORE_PASSWORD + SettingsApp.sipRegisterImsi).toCharArray();
    }
    private static File KeystoreFile(final Context ctx)
    {
        return new File(ctx.getFilesDir(), "certfile-" + SettingsApp.sipRegisterImsi);
    }
    @NonNull
    private static KeyStore OpenKeystore(final Context ctx, final char[] pass) throws KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException
    {
        final KeyStore ks;
        ks = openDefaultKeystore(ctx, pass);
        return ks;
    }

    public static void createKeystoreCert(final Context ctx, final InputStream certStream, final PrivateKey privKey, final char[] pass)
    {
        try
        {
            final Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(certStream);
            createKeystoreCert(ctx, certificate, privKey, pass);
        }
        catch (CertificateException e)
        {
            Log.d(null, "App error", e);
        }
    }
    public static void createKeystoreCert(final Context ctx, final Certificate certificate, final PrivateKey privKey, final char[] pass)
    {
        final KeyStore ks;
        try
        {
            ks = OpenKeystore(ctx, pass);
            createDefaultKeyStore(ctx, certificate, privKey, pass, ks);
        }
        catch (Exception e)
        {
            Log.d(null, "App error", e);
        }
    }

    private static void createDefaultKeyStore(Context ctx, Certificate certificate, PrivateKey privKey, char[] pass, KeyStore ks) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        ks.setKeyEntry(CsrHelper.NAME, privKey, pass, new Certificate[] {certificate});
        FileOutputStream fos = null;
        try
        {
            fos = new FileOutputStream(KeystoreFile(ctx));
            ks.store(fos, pass);
        }
        finally
        {
            if (fos != null)
            {
                fos.close();
            }
        }
    }
    @NonNull
    private static KeyStore openDefaultKeystore(Context ctx, char[] pass) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        KeyStore ks;
        FileInputStream fis = null;
        try
        {
            ks = KeyStore.getInstance(KeyStore.getDefaultType());
            final File certFile = KeystoreFile(ctx);
            if (certFile.exists())
            {
                fis = new FileInputStream(certFile);
                ks.load(fis, pass);
            }
            else
            {
                ks.load(null, pass);
            }
        }
        finally
        {
            if (fis != null)
            {
                try
                {
                    fis.close();
                }
                catch (IOException ignored)
                {
                    Log.d("Ignore it", ignored.toString());
                }
            }
        }
        return ks;
    }
    public static Certificate ReadKeystoreCert(final Context ctx, final char[] pass)
    {
        try
        {
            final KeyStore ks = OpenKeystore(ctx, pass);
            return ks.getCertificate(CsrHelper.NAME);
        }
        catch (Exception e)
        {
            Log.d(null, "App error", e);
        }
        return null;
    }
    public static Certificate[] ReadKeystoreChain(final Context ctx)
    {
        final KeyStore ks;
        try
        {
            ks = OpenKeystore(ctx, KeystorePass());
            return ks.getCertificateChain(CsrHelper.NAME);
        }
        catch (Exception e)
        {
            Log.d(null, "App error", e);
        }
        return null;
    }
    public static PrivateKey ReadPrivateKey(final Context ctx)
    {
        final KeyStore ks;
        try
        {
            ks = OpenKeystore(ctx, KeystorePass());
            final KeyStore.Entry entry = ks.getEntry(CsrHelper.NAME, new KeyStore.PasswordProtection(KeystorePass()));
            return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
        }
        catch (Exception e)
        {
            Log.d(null, "App error", e);
        }
        return null;
    }
}
