package aps;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class ActiveNet {

    private BasicCookieStore cookieStore = null;
    private String url;

    public ActiveNet(String username, String password, String domain) throws LoginError {
        this.url = "https://anprodca.active.com/" + domain + "/servlet/";
        this.init(username, password, url);
    }

    public BasicCookieStore getCookieStore() {
        return cookieStore;
    }

    private void init(String username, String password, String url) throws LoginError {

        /* Set up cookie store */
        if (cookieStore != null) {
            cookieStore.clear();
        }
        cookieStore = new BasicCookieStore();

        /* Set up http client */
        CloseableHttpClient chc = HttpClients.custom()
                .setDefaultCookieStore(cookieStore)
                .build();
        HttpUriRequest login;

        try {

            /* Send init request */
            login = RequestBuilder.post()
                    .setUri(new URI(url + "processAdminLogin.sdi"))
                    .addParameter("uname", username)
                    .addParameter("adminloginpassword", password)
                    .build();
            CloseableHttpResponse r = chc.execute(login);

            /* Parse response */
            HttpEntity entity = r.getEntity();
            Document doc = Jsoup.parse(EntityUtils.toString(entity, "UTF-8"));
            EntityUtils.consume(entity);

            /* Verify log in successful */
            if (doc.title().contains("Admin Login"))
                throw new LoginError();

        } catch (IOException | URISyntaxException e) {
            cookieStore = null;
            try {
                chc.close();
            } catch (IOException ignored) {
            }
            throw new LoginError();
        }

        /* Get 'JSESSIONID' from cookie store */
        List<Cookie> cookies = cookieStore.getCookies();
        if (cookies.isEmpty())
            throw new LoginError();
        String sessionID = cookies.get(0).getValue();

        /* Associate session ID w/ workstation */
        try {
            login = RequestBuilder.post()
                    .setUri(new URI(url + "processAssignWorkstation.sdi"))
                    .addParameter("workstation_id", "9")  // ActiveNet Support
                    .build();
            CloseableHttpResponse r = chc.execute(login);

        } catch (IOException | URISyntaxException e) {
            cookieStore = null;
            sessionID = null;
            throw new LoginError();
        } finally {
            try {
                chc.close();
            } catch (IOException ignored) {
            }
        }
    }


    public String getUrl() {
        return url;
    }
}

class LoginError extends Exception {
    LoginError() {
        super();
    }

}

