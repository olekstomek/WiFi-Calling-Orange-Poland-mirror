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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.support.annotation.NonNull;
import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

import pl.orangelabs.log.Log;
import pl.orangelabs.wificalling.BuildConfig;

/**
 * @author F
 */
public class KeystoreHandling
{

    private final static int KEY_SIZE = 2048;
    private final static String CIPHER_TRANSFORM = "RSA/None/PKCS1Padding";

    public static Certificate ReadKeystoreCert(final Context ctx)
    {
        try
        {
            final KeyStore ks = OpenKeystore(ctx);
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
        if (BuildConfig.CONFIG_PROD)
        {
            final KeyStore ks;
            try
            {
                ks = OpenKeystore(ctx);
                return ks.getCertificateChain(CsrHelper.NAME);
            } catch (Exception e)
            {
                Log.d(null, "App error", e);
            }
            return null;
        }
        else
        {
            return KeystoreHandlingDefaultAccounts.ReadKeystoreChain(ctx);
        }
    }

    public static PrivateKey ReadPrivateKey(final Context ctx)
    {
        if (BuildConfig.CONFIG_PROD)
        {
            final KeyStore ks;
            try
            {
                ks = OpenKeystore(ctx);
                final KeyStore.Entry entry = ks.getEntry(CsrHelper.NAME, null);
                return ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();
            } catch (Exception e)
            {
                Log.d(null, "App error", e);
            }
        }
        else
        {
            return KeystoreHandlingDefaultAccounts.ReadPrivateKey(ctx);
        }
        return null;
    }

    static void createKeystoreCert(final Context ctx, String cert, final PrivateKey privKey)
    {
        cert = cert.substring(0,cert.length()-1);
        byte[] bytes = cert.getBytes(); //Base64.decode(cert.replaceAll("-----BEGIN CERTIFICATE-----", "").replaceAll("-----END CERTIFICATE-----", ""), Base64.DEFAULT);
        createKeystoreCert(ctx, bytes, privKey);
    }
    private static void createKeystoreCert(final Context ctx, byte[] bytesCert, final PrivateKey privKey)
    {
        InputStream bis = new ByteArrayInputStream(bytesCert);
        final Certificate certificate;
        try
        {
            certificate = CertificateFactory.getInstance("X.509").generateCertificate(bis);
            createKeystoreCert(ctx, certificate, privKey);
        } catch (CertificateException e)
        {
            Log.d(null, "App error", e);
        }
    }
    public static void createKeystoreCert(final Context ctx, final InputStream certStream, final PrivateKey privKey)
    {
        try
        {
            final Certificate certificate = CertificateFactory.getInstance("X.509").generateCertificate(certStream);
            createKeystoreCert(ctx, certificate, privKey);

        } catch (CertificateException e)
        {
            Log.d(null, "App error", e);
        }
    }
    private static void createKeystoreCert(final Context ctx, final Certificate certificate, final PrivateKey privKey)
    {
        final KeyStore ks;
        try
        {
            ks = OpenKeystore(ctx);
            createAndroidKeyStore(certificate, privKey, ks);
        }
        catch (Exception e)
        {
            Log.d(null, "App error", e);
        }
    }

    private static void createAndroidKeyStore(Certificate certificate, PrivateKey privKey, KeyStore ks) throws KeyStoreException
    {
        ks.setKeyEntry(CsrHelper.NAME, privKey, null , new Certificate[] {certificate});
    }

    @NonNull
    private static KeyStore OpenKeystore(final Context ctx) throws KeyStoreException, IOException, NoSuchAlgorithmException,
        CertificateException
    {
        final KeyStore ks;
        ks = openAndroidKeyStore();
        return ks;
    }


    @NonNull
    private static KeyStore openAndroidKeyStore() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException
    {
        KeyStore ks;
        ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        return ks;
    }
    static String encryptString(String alias, String text, Context context) {
        String encryptedPassword = null;
        try {
            final KeyStore keyStore;
            keyStore = OpenKeystore(context);
            createNewKeys(keyStore, alias, context);
            KeyStore.Entry entry = keyStore.getEntry(alias, null);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)entry;
            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            Cipher input = Cipher.getInstance(CIPHER_TRANSFORM);
            input.init(Cipher.ENCRYPT_MODE, publicKey);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    outputStream, input);
            cipherOutputStream.write(text.getBytes("UTF-8"));
            cipherOutputStream.close();

            byte [] values = outputStream.toByteArray();
            encryptedPassword = Base64.encodeToString(values, Base64.DEFAULT);
        } catch (Exception e) {
            Log.e(Log.STATIC_CTX,"", e);
        }
        return encryptedPassword;
    }
    static String decryptString(String alias, String encryptedText, Context context) {
        String decryptedText = "";
        try {
            final KeyStore keyStore;
            keyStore = OpenKeystore(context);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)keyStore.getEntry(alias, null);
            Cipher output = Cipher.getInstance(CIPHER_TRANSFORM);
            output.init(Cipher.DECRYPT_MODE, privateKeyEntry.getPrivateKey());
            CipherInputStream cipherInputStream = new CipherInputStream(
                    new ByteArrayInputStream(Base64.decode(encryptedText, Base64.DEFAULT)), output);
            ArrayList<Byte> values = new ArrayList<>();
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                values.add((byte)nextByte);
            }

            byte[] bytes = new byte[values.size()];
            for(int i = 0; i < bytes.length; i++) {
                bytes[i] = values.get(i);
            }

            decryptedText = new String(bytes, 0, bytes.length, "UTF-8");

        } catch (Exception e) {
            Log.e(Log.STATIC_CTX,"", e);
        }
        return decryptedText;
    }

    private static void createNewKeys(KeyStore keyStore, String alias, Context context) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException, InvalidAlgorithmParameterException, NoSuchProviderException
    {
        if (!keyStore.containsAlias(alias))
        {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "AndroidKeyStore");
            initializeKeygen(alias, 1000, generator, false, context);
            generator.generateKeyPair();
        }
    }
    static void initializeKeygen(String alias,int days, KeyPairGenerator keyPairGenerator,boolean sign, Context context) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException
    {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int purpose = sign ? KeyProperties.PURPOSE_SIGN | KeyProperties.PURPOSE_VERIFY : KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT;
            initializeKeygenAndroidM(alias, days, keyPairGenerator, purpose);
        }
        else
        {

            initializationKeygenUnderAndroidM(alias, days, keyPairGenerator, context);
        }
    }


    public static Date getDateAfterDays(int days)
    {
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(new Date());
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private static void initializeKeygenAndroidM(String alias,int days, KeyPairGenerator keyPairGenerator,int purpose) throws InvalidAlgorithmParameterException
    {
        keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(
                alias,
                purpose)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512, KeyProperties.DIGEST_SHA1)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .setKeySize(KEY_SIZE)
                .setCertificateSubject(new X500Principal("CN="+ alias))
                .setCertificateNotBefore(new Date()).setCertificateNotAfter(getDateAfterDays(days))
                .build());
    }
    @SuppressWarnings("deprecation") // lower API
    private static void initializationKeygenUnderAndroidM(String alias, int days, KeyPairGenerator keyPairGenerator, Context context) throws InvalidAlgorithmParameterException
    {
        KeyPairGeneratorSpec spec =
                new KeyPairGeneratorSpec.Builder(context).setAlias(alias)
                        .setSubject(new X500Principal("CN="+ alias)).setSerialNumber(BigInteger.valueOf(100))
                        .setStartDate(new Date()).setEndDate(getDateAfterDays(days)).build();
        keyPairGenerator.initialize(spec);
    }
}
