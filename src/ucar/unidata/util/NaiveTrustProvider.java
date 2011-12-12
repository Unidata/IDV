/*
 * Copyright 1997-2010 Unidata Program Center/University Corporation for
 * Atmospheric Research, P.O. Box 3000, Boulder, CO 80307,
 * support@unidata.ucar.edu.
 * 
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or (at
 * your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
//j-

/**
 * Code is from http://www.howardism.org/Technical/Java/SelfSignedCerts.html
 */

package ucar.unidata.util;


import java.security.*;
import java.security.cert.*;

import javax.net.ssl.*;


/**
 * Provides all secure socket factories, with a socket that ignores
 * problems in the chain of certificate trust. This is good for embedded
 * applications that just want the encryption aspect of SSL communication,
 * without worrying too much about validating the identify of the server at the
 * other end of the connection. In other words, this may leave you vulnerable
 * to a man-in-the-middle attack.
 */

public final class NaiveTrustProvider extends Provider {

    /** The name of our algorithm */
    private static final String TRUST_PROVIDER_ALG = "NaiveTrustAlgorithm";

    /** Need to refer to ourselves somehow to know if we're already registered */
    private static final String TRUST_PROVIDER_ID = "NaiveTrustProvider";

    /**
     * Hook in at the provider level to handle libraries and 3rd party
     * utilities that use their own factory. Requires permission to
     * execute AccessController.doPrivileged,
     * so this probably won't work in applets or other high-security jvms
     */

    public NaiveTrustProvider() {
        super(TRUST_PROVIDER_ID, (double) 0.1,
              "NaiveTrustProvider (provides all secure socket factories by ignoring problems in the chain of certificate trust)");

        AccessController.doPrivileged(new PrivilegedAction() {
            public Object run() {
                put("TrustManagerFactory."
                    + NaiveTrustManagerFactory
                        .getAlgorithm(), NaiveTrustManagerFactory.class
                        .getName());
                return null;
            }
        });
    }

    /**
     * This is the only method the client code need to call. Yup, just put
     * NaiveTrustProvider.setAlwaysTrust() into your initialization code
     * and you're good to go
     *
     * @param enableNaiveTrustProvider set to true to always trust (set to false
     *          it not yet implemented)
     */

    public static void setAlwaysTrust(boolean enableNaiveTrustProvider) {
        if (enableNaiveTrustProvider) {
            Provider registered = Security.getProvider(TRUST_PROVIDER_ID);
            if (null == registered) {
                Security.insertProviderAt(new NaiveTrustProvider(), 1);
                Security.setProperty("ssl.TrustManagerFactory.algorithm",
                                     TRUST_PROVIDER_ALG);
            }
        } else {
            throw new UnsupportedOperationException(
                "Disable Naive trust provider not yet implemented");
        }
    }

    /**
     * The factory for the NaiveTrustProvider
     */
    public final static class NaiveTrustManagerFactory extends TrustManagerFactorySpi {

        /**
         * _more_
         */
        public NaiveTrustManagerFactory() {}

        /**
         * _more_
         *
         * @param mgrparams _more_
         */
        protected void engineInit(ManagerFactoryParameters mgrparams) {}

        /**
         * _more_
         *
         * @param keystore _more_
         */
        protected void engineInit(KeyStore keystore) {}

        /**
         * Returns a collection of trust managers that are naive.
         * This collection is just a single element array containing
         * our NaiveTrustManager class.
         *
         * @return _more_
         */
        protected TrustManager[] engineGetTrustManagers() {
            // Returns a new array of just a single NaiveTrustManager.
            return new TrustManager[] { new NaiveTrustManager() };
        }

        /**
         * Returns our "NaiveTrustAlgorithm" string.
         * @return The string, "NaiveTrustAlgorithm"
         */
        public static String getAlgorithm() {
            return TRUST_PROVIDER_ALG;
        }
    }


    /**
     * Class description
     *
     *
     * @version        Enter version here..., Mon, Aug 8, '11
     * @author         Enter your name here...    
     */
    private static class NaiveTrustManager implements X509TrustManager {

        /**
         * Doesn't throw an exception, so this is how it approves a certificate.
         * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], String)
         *
         * @param cert _more_
         * @param authType _more_
         *
         * @throws CertificateException _more_
         */
        public void checkClientTrusted(X509Certificate[] cert,
                                       String authType)
                throws CertificateException {}

        /**
         * Doesn't throw an exception, so this is how it approves a certificate.
         * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], String)
         *
         * @param cert _more_
         * @param authType _more_
         *
         * @throws CertificateException _more_
         */
        public void checkServerTrusted(X509Certificate[] cert,
                                       String authType)
                throws CertificateException {}

        /**
         * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
         *
         * @return _more_
         */
        public X509Certificate[] getAcceptedIssuers() {
            return null;  // I've seen someone return new X509Certificate[ 0 ]; 
        }

    }



}
