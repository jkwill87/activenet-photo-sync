package com.jkwill87.aps;

import org.apache.commons.dbcp2.BasicDataSource;
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
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActiveNet {

    private BasicCookieStore cookieStore = null;
    private Connection sqlConnection;
    private String url;

    public ActiveNet(String username, String password, String sqlUrl, String domain) throws LoginError {

        this.url = "https://anprodca.active.com/" + domain + "/servlet/";
        this.init(username, password, url);

        try {
            BasicDataSource ds = new BasicDataSource();
            ds.setDriverClassName("com.mysql.jdbc.Driver");
            ds.setUsername(username);
            ds.setPassword(password);
            ds.setUrl("jdbc:mysql://" + sqlUrl + ":3306/aps");
            this.sqlConnection = ds.getConnection();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new LoginError();
        }
    }

    public BasicCookieStore getCookieStore() {
        return cookieStore;
    }

    public Connection getSqlConnection() {
        return sqlConnection;
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

    public int getCustomerCount() {
        String CUSTOMER_COUNT = "SELECT COUNT(1) FROM customer;";

        int count = -1;
        if (sqlConnection == null) return count;
        try {
            Statement st = sqlConnection.createStatement();
            ResultSet rs = st.executeQuery(CUSTOMER_COUNT);
            rs.next();
            count = rs.getInt(1);
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return count;
    }

    public ArrayList<String> dbQuery(String field, String search) {
        String SQL_INSERT_IMG = "SELECT * FROM customer WHERE " + field
                + " LIKE ?";
        ArrayList<String> results = new ArrayList<>();
        PreparedStatement ps;
        ResultSet rs;

        if (sqlConnection == null) return results;
        if (field == null) return results;
        if (search == null) return results;

        try {
            ps = sqlConnection.prepareStatement(
                    SQL_INSERT_IMG);
            ps.setString(1, '%' + search + '%');
            rs = ps.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
        try {
            while (rs.next()) {
                results.add(String.format("%s (%d) [%s]",
                        rs.getString("name"),
                        rs.getLong("activeID"),
                        rs.getTimestamp("uploaded") == null
                                ? "not uploaded"
                                : "uploaded"
                ));
            }
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    protected boolean dbKeyExists(long primaryID) {
        Statement stmt;
        ResultSet rs;
        boolean exists = false;
        try {
            stmt = sqlConnection.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) FROM customer WHERE primaryID= "
                    + primaryID);
            rs.next();
            exists = rs.getInt(1) == 1;
            rs.close();
            stmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exists;
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

