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

public final class NaiveTrustProvider extends Provider
{
  /** The name of our algorithm **/
  private static final String TRUST_PROVIDER_ALG = "NaiveTrustAlgorithm";

  /** Need to refer to ourselves somehow to know if we're already registered **/
  private static final String TRUST_PROVIDER_ID  = "NaiveTrustProvider";

  /**
   * Hook in at the provider level to handle libraries and 3rd party 
   * utilities that use their own factory. Requires permission to 
   * execute AccessController.doPrivileged,
   * so this probably won't work in applets or other high-security jvms
   **/

  public NaiveTrustProvider ()
  {
    super ( TRUST_PROVIDER_ID,
            (double) 0.1,
            "NaiveTrustProvider (provides all secure socket factories by ignoring problems in the chain of certificate trust)" );

    AccessController.doPrivileged ( new PrivilegedAction() {
      public Object run()
      {
        put ("TrustManagerFactory." + NaiveTrustManagerFactory.getAlgorithm(),
             NaiveTrustManagerFactory.class.getName() );
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
   **/

  public static void setAlwaysTrust (boolean enableNaiveTrustProvider)
  {
    if (enableNaiveTrustProvider) {
      Provider registered = Security.getProvider (TRUST_PROVIDER_ID);
      if (null == registered) {
        Security.insertProviderAt (new NaiveTrustProvider (), 1);
        Security.setProperty ("ssl.TrustManagerFactory.algorithm",
                              TRUST_PROVIDER_ALG);
      }
    } else {
      throw new UnsupportedOperationException (
          "Disable Naive trust provider not yet implemented");
    }
  }

  /**
   * The factory for the NaiveTrustProvider
   **/
  public final static class NaiveTrustManagerFactory 
         extends TrustManagerFactorySpi
  {
    public NaiveTrustManagerFactory () { }

    protected void engineInit (ManagerFactoryParameters mgrparams)
    {
    }

    protected void engineInit (KeyStore keystore)
    {
    }

    /**
     * Returns a collection of trust managers that are naive. 
     * This collection is just a single element array containing
     * our {@link NaiveTrustManager} class.
     **/
    protected TrustManager[] engineGetTrustManagers ()
    {
      // Returns a new array of just a single NaiveTrustManager.
      return new TrustManager[] { new NaiveTrustManager() } ;
    }

    /**
     * Returns our "NaiveTrustAlgorithm" string.
     * @return The string, "NaiveTrustAlgorithm"
     */
    public static String getAlgorithm()
    {
      return TRUST_PROVIDER_ALG;
    }
  }


private static class NaiveTrustManager implements X509TrustManager {

  /**
   * Doesn't throw an exception, so this is how it approves a certificate.
   * @see javax.net.ssl.X509TrustManager#checkClientTrusted(java.security.cert.X509Certificate[], String)
   **/
  public void checkClientTrusted ( X509Certificate[] cert, String authType )
              throws CertificateException 
  {
  }

  /**
   * Doesn't throw an exception, so this is how it approves a certificate.
   * @see javax.net.ssl.X509TrustManager#checkServerTrusted(java.security.cert.X509Certificate[], String)
   **/
  public void checkServerTrusted ( X509Certificate[] cert, String authType ) 
     throws CertificateException 
  {
  }

  /**
   * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
   **/
  public X509Certificate[] getAcceptedIssuers ()
  {
    return null;  // I've seen someone return new X509Certificate[ 0 ]; 
  }

}



}
