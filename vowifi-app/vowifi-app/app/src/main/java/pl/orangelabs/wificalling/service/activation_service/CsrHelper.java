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

import org.spongycastle.asn1.ASN1ObjectIdentifier;
import org.spongycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.spongycastle.asn1.x500.X500Name;
import org.spongycastle.asn1.x509.AlgorithmIdentifier;
import org.spongycastle.asn1.x509.BasicConstraints;
import org.spongycastle.asn1.x509.Extension;
import org.spongycastle.asn1.x509.ExtensionsGenerator;
import org.spongycastle.openssl.jcajce.JcaPEMWriter;
import org.spongycastle.operator.ContentSigner;
import org.spongycastle.operator.OperatorCreationException;
import org.spongycastle.pkcs.PKCS10CertificationRequest;
import org.spongycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.spongycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.spongycastle.util.io.pem.PemObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.HashMap;
import java.util.Map;

import pl.orangelabs.log.Log;

/**
 * Created by marcin on 30.11.16.
 */

public class CsrHelper {

    private final static String DEFAULT_SIGNATURE_ALGORITHM = "SHA256withRSA";
    private final static String CN_PATTERN = "CN=%s, O=Orange, OU=OrangeLabs";
    public final static String NAME = "OrangeLabCert";
    private final static int CERTIFICATE_DAY_VALID = 100;
    private final static boolean CSRwithHeader = true;

    private static class JCESigner implements ContentSigner {

        private static Map<String, AlgorithmIdentifier> ALGOS = new HashMap<String, AlgorithmIdentifier>();

        static {
            ALGOS.put("SHA256withRSA".toLowerCase(), new AlgorithmIdentifier(
                    new ASN1ObjectIdentifier("1.2.840.113549.1.1.11")));
            ALGOS.put("SHA1withRSA".toLowerCase(), new AlgorithmIdentifier(
                    new ASN1ObjectIdentifier("1.2.840.113549.1.1.5")));

        }

        private String mAlgorithm;
        private Signature signature;
        private ByteArrayOutputStream outputStream;

        JCESigner(PrivateKey privateKey, String signAlgorithm) {
            //Utils.throwIfNull(privateKey, sigAlgo);
            mAlgorithm = signAlgorithm.toLowerCase();
            try {
                this.outputStream = new ByteArrayOutputStream();
                this.signature = Signature.getInstance(signAlgorithm);
                this.signature.initSign(privateKey);
            } catch (GeneralSecurityException gse) {
                throw new IllegalArgumentException(gse.getMessage());
            }
        }

        @Override
        public AlgorithmIdentifier getAlgorithmIdentifier() {
            AlgorithmIdentifier id = ALGOS.get(mAlgorithm);
            if (id == null) {
                throw new IllegalArgumentException("Does not support algorithm: " +
                        mAlgorithm);
            }
            return id;
        }

        @Override
        public OutputStream getOutputStream() {
            return outputStream;
        }

        @Override
        public byte[] getSignature() {
            try {
                signature.update(outputStream.toByteArray());
                return signature.sign();
            } catch (GeneralSecurityException gse) {
                gse.printStackTrace();
                return null;
            }
        }
    }
    public static String getStringCsr(byte[] csr) throws IOException
    {
        PemObject pemObject = new PemObject("CERTIFICATE REQUEST", csr);
        StringWriter str = new StringWriter();
        JcaPEMWriter pemWriter = new JcaPEMWriter(str);
        pemWriter.writeObject(pemObject);
        pemWriter.close();
        str.close();
        return str.toString();
    }
    //Create the certificate signing request (CSR) from private and public keys
    private static PKCS10CertificationRequest generateCSR(KeyPair keyPair, String cn) throws IOException,
            OperatorCreationException
    {
        String principal = String.format(CN_PATTERN, cn);
        ContentSigner signer = new JCESigner (keyPair.getPrivate(), DEFAULT_SIGNATURE_ALGORITHM);
        PKCS10CertificationRequestBuilder csrBuilder = new JcaPKCS10CertificationRequestBuilder(
                new X500Name(principal), keyPair.getPublic());
        ExtensionsGenerator extensionsGenerator = new ExtensionsGenerator();
        extensionsGenerator.addExtension(Extension.basicConstraints, true, new BasicConstraints(
                true));
        csrBuilder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest,
                extensionsGenerator.generate());


        return csrBuilder.build(signer);
    }
    private static CSRKey getCSR(String name, Context context) throws NoSuchAlgorithmException, IOException, InvalidAlgorithmParameterException, NoSuchProviderException
    {

        //Generate KeyPair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA" ,"AndroidKeyStore");
        KeystoreHandling.initializeKeygen(NAME, CERTIFICATE_DAY_VALID,
                keyGen, true, context);
        KeyPair keyPair = keyGen.generateKeyPair();
        PKCS10CertificationRequest csr = null;
        try
        {
            csr = CsrHelper.generateCSR(keyPair, name);

        } catch (OperatorCreationException e)
        {
            Log.e(Log.STATIC_CTX,"", e);
        }
        if (csr != null)
        {
            return new CSRKey(CsrHelper.getStringCsr(csr.getEncoded()),keyPair.getPrivate());
        }
        else
        {
            return null;
        }
    }

    public static CSRKey getFormattedCSR(Context context)
    {

        try
        {
            CSRKey csr = getCSR(NAME, context);
            if (!CSRwithHeader)
            {
                csr.setCert(csr.getCert().replace("-----BEGIN CERTIFICATE REQUEST-----\n", ""));
                csr.setCert(csr.getCert().replace("\n-----END CERTIFICATE REQUEST-----", ""));
            }

            return csr;
        }
        catch (NoSuchAlgorithmException | IOException | InvalidAlgorithmParameterException | NoSuchProviderException e)
        {
            Log.e(Log.STATIC_CTX,"",e);
        }
        return null;
    }
}