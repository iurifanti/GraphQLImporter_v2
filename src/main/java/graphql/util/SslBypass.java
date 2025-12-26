package graphql.util;

import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import javax.net.ssl.*;

/**
 * Utility per disabilitare la verifica SSL in ambienti dove è necessario
 * accettare certificati non validi (solo per utilizzo controllato).
 */
public final class SslBypass {

    private SslBypass() {}

    /**
     * Configura l'HTTPS client predefinito perché accetti qualsiasi certificato
     * e host, utile in contesti di test con certificati self-signed.
     */
    public static void disableSslVerificationIfNeeded() {
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new SecureRandom());

            // L'intervento riguarda esclusivamente le connessioni HTTPS
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        } catch (Exception e) {
            throw new IllegalStateException("Unable to disable SSL verification", e);
        }
    }
}

