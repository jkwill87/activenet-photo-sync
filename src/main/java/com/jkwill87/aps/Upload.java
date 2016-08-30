package com.jkwill87.aps;

import org.apache.http.HttpEntity;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

enum UploadEvent {
    UPLOADED("UPLOADED", "uploaded"),
    NOTFOUND("NOTFOUND", "not found"),
    SKIPPED("SKIPPED", "skipped"),
    IMGERROR("IMGERROR", "image error");

    private String e, s;

    UploadEvent(String e, String s) {
        this.e = e;
        this.s = s;
    }

    public String event() {
        return this.e;
    }

    public String text() {
        return this.s;
    }
}

public class Upload implements Runnable {

    private Connection connection;
    private CookieStore cookieStore;
    private File file;
    private URI uri;
    private boolean logging;

    public Upload(String url, Connection con, CookieStore cs, File f, boolean logging) {
        this.connection = con;
        this.cookieStore = cs;
        this.file = f;
        this.logging = logging;
        try {
            this.uri = new URI(url + "adminChange.sdi");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private static String parseString(Document html, String target) {
        try {
            String parsed = html
                    .select("td[class=tlbl]:contains(" + target + ")")
                    .first()
                    .nextElementSibling()
                    .text();
            if (parsed == null) return null;
            if (parsed.contains("--")) return null;
            return parsed.isEmpty()
                    ? null
                    : parsed.replaceAll("[^A-Za-z0-9\\-_@. ]", "");
        } catch (NullPointerException ignored) {
            return null;
        }
    }

    @Override
    public void run() {
        if (this.cookieStore == null)
            return;
        Customer currentCustomer;
        HttpUriRequest query;

        CloseableHttpClient chc = HttpClients.custom()
                .setDefaultCookieStore(this.cookieStore)
                .build();

        /* If file cannot be found, skip */
        try {
            currentCustomer = new Customer(file);
        } catch (IOException e) {
            log(file, UploadEvent.IMGERROR);
            return;
        }

        try {

            /* Skip if already indexed */
            if (dbKeyExists(currentCustomer.primaryID)) {
                log(file, UploadEvent.SKIPPED);
                return;
            }

            /* Check against student/ staff alternate keys
             * ..this code is specific to the University of Guelph */
            query = RequestBuilder.post()
                    .setUri(this.uri)
                    .addParameter("xnetframe", "false")
                    .addParameter("oc", "Customer")
                    .addParameter("akt_id",
                            (currentCustomer.isStudent ? "1" : "2"))
                    .addParameter("akt_ids", "2,1")
                    .addParameter("reset_pattern", "true")
                    .addParameter("search_alternateKey", "true")
                    .addParameter(
                            "akt_value_" + (currentCustomer.isStudent ? "1" : "2"),
                            currentCustomer.toString()
                    )
                    .build();

            CloseableHttpResponse r = chc.execute(query);

            /* Parse response */
            HttpEntity entity = r.getEntity();
            Document doc = Jsoup.parse(EntityUtils.toString(entity, "UTF-8"));
            EntityUtils.consume(entity);

            /* Fall back to using swipe if missed */
            if (doc.title().equals("Find Customer")) {

                query = RequestBuilder.post()
                        .setUri(this.uri)
                        .addParameter("xnetframe", "false")
                        .addParameter("oc", "Customer")
                        .addParameter("reset_pattern", "true")
                        .addParameter("scan_card", "true")
                        .addParameter("akt_ids", "2,1")
                        .addParameter("scan_element", currentCustomer.toString())
                        .build();
                r = chc.execute(query);
                EntityUtils.consume(entity);

                /* Parse response */
                entity = r.getEntity();
                doc = Jsoup.parse(EntityUtils.toString(entity, "UTF-8"));
                EntityUtils.consume(entity);

                if (doc.title().equals("Find Customer")) {
                    log(file, UploadEvent.NOTFOUND);

                    return;
                }
            }
            Pattern p = Pattern.compile(".+#(\\d+)\\)");
            Matcher m = p.matcher(doc.title());
            if (!m.find()) {
                log(file, UploadEvent.NOTFOUND);
                return;
            }

            /* Parse fields */
            currentCustomer.activeID = Integer.parseInt(m.group(1));
            currentCustomer.name = parseString(doc, "Customer Name");
            currentCustomer.email = parseString(doc, "Email");

            /* Upload to ActiveNet */
            try {
                upload(file, currentCustomer.activeID);
            } catch (ImageError imageError) {
                try {
                    Thread.sleep(5000);  // wait 1 second
                    upload(file, currentCustomer.activeID);
                } catch (ImageError | InterruptedException err) {
                    log(file, UploadEvent.IMGERROR);

                    return;
                }
            }

            /* Enter into database, retry on error */
            try {
                dbInsert(currentCustomer);
            } catch (Exception ignored) {
                try {
                    Thread.sleep(5000);  // wait 1 second
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                dbInsert(currentCustomer);
            }
            log(file, UploadEvent.UPLOADED);

        } catch (IOException ignored) {
            log(file, UploadEvent.IMGERROR);
        }

        try {
            chc.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean dbInsert(Customer customer) {

        try {
            String SQL_INSERT_CUSTOMER = "REPLACE INTO customer("
                    + "primaryID,"
                    + "activeID,"
                    + "name,"
                    + "email"
                    + ") VALUES(?,?,?,?)";
            PreparedStatement ps = this.connection.prepareStatement(
                    SQL_INSERT_CUSTOMER);
            ps.setLong(1, customer.primaryID);
            ps.setInt(2, customer.activeID);
            ps.setString(3, customer.name);
            ps.setString(4, customer.email);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


    /**************** Code Below Adapted from ActiveNet Applet ****************/

    private void upload(File file, long activeID) throws ImageError {
        ByteArrayOutputStream output;
        BufferedImage img = scale(file);
        int image_bytes = img.getWidth(null);
        int boundary = img.getHeight(null);
        BufferedImage bImg = new BufferedImage(image_bytes, boundary, 1);
        bImg.createGraphics().drawImage(img, 0, 0, null);
        output = new ByteArrayOutputStream();
        try {
            ImageIO.write(bImg, "jpg", output);
            output.close();
        } catch (IOException e) {
            throw new ImageError();
        }

        byte[] image_bytes1 = output.toByteArray();
        String boundary1 = this.getBoundary(image_bytes1);

        try {
            String destination = "uploadPicture.sdi";
            destination = destination + "?sdireqauth=0";
            URL e1 = new URL("https://anprodca.active.com/uofg/servlet/"
                    + destination);
            HttpURLConnection conn = (HttpURLConnection) e1.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Content-Type",
                    "multipart/form-data; boundary=" + boundary1);
            conn.setUseCaches(false);
            conn.setDefaultUseCaches(false);
            new StringBuilder();
            DataOutputStream postFormFile = new DataOutputStream(
                    conn.getOutputStream());
            PrintStream ps = new PrintStream(postFormFile);
            ps.println("--" + boundary1);
            ps.print("Content-Disposition: form-data; name=\"xxx\"\r\n");
            ps.print("\r\n");
            ps.print("yyy\r\n");
            ps.print("--" + boundary1 + "\r\n");
            ps.print("Content-Disposition: form-data;"
                    + " name=\"customer_id\"\r\n");
            ps.print("\r\n");
            ps.print(activeID + "\r\n");
            ps.print("--" + boundary1 + "\r\n");
            ps.print("Content-Disposition: form-data; name=\"pass_id\"\r\n");
            ps.print("\r\n");
            ps.print("\r\n");
            ps.print("--" + boundary1 + "\r\n");
            ps.print("Content-Disposition: form-data; name=\"file\"; "
                    + "filename=\"picture\"\r\n");
            ps.print("Content-Type: image/jpeg\r\n");
            ps.print("\r\n");
            ps.write(image_bytes1);
            ps.print("\r\n");
            ps.print("--" + boundary1 + "--\r\n");
            ps.flush();
            ps.close();
            int response_code = conn.getResponseCode();
            if (response_code != 202) throw new ImageError();

        } catch (Exception e) {
            throw new ImageError();
        }
    }

    private BufferedImage scale(File file) throws ImageError {
        BufferedImage bImg;
        if (file == null) return null;
        try {
            bImg = ImageIO.read(file);
        } catch (IOException e) {
            log(file, UploadEvent.IMGERROR);
            throw new ImageError();
        }

        int desired_width = 180;
        int desired_height = 220;

        int image_width = bImg.getWidth(null);
        int image_height = bImg.getHeight(null);
        if (image_width > 0 && image_height > 0) {
            if (image_width != desired_width || image_height != desired_height) {
                double source_left = (double) desired_width
                        / (double) image_width;
                double source_bottom = (double) desired_height
                        / (double) image_height;
                double bi = Math.max(source_left, source_bottom);
                int new_width = (int) ((double) image_width * bi + 0.9D);
                int new_height = (int) ((double) image_height * bi + 0.9D);
                BufferedImage bi1 = new BufferedImage(new_width, new_height, 1);
                bi1.createGraphics().drawImage(bImg, 0, 0, new_width,
                        new_height, 0, 0, image_width, image_height, null);
                bImg = bi1;
                image_width = new_width;
                image_height = new_height;
            }

            if (image_width > desired_width || image_height > desired_height) {
                int source_left1 = 0;
                int source_right = image_width;
                if (image_width > desired_width) {
                    source_left1 = (image_width - desired_width) / 2;
                    source_right = image_width - source_left1;
                }

                int source_bottom1 = image_height;
                int source_top = 0;
                if (image_height > desired_height) {
                    source_top = (image_height - desired_height) / 2;
                    source_bottom1 = image_height - source_top;
                }

                BufferedImage bi2 = new BufferedImage(source_right
                        - source_left1, source_bottom1 - source_top, 1);
                bi2.createGraphics().drawImage(
                        bImg, 0, 0, source_right - source_left1,
                        source_bottom1 - source_top, source_left1, source_top,
                        source_right, source_bottom1, null
                );
                bImg = bi2;
            }
            return bImg;
        } else {
            return bImg;
        }
    }

    private String getBoundary(byte[] b) {
        String boundary;
        boolean found;
        do {
            boundary = "_=_NextPart_001_" + Math.random();
            byte[] boundary_bytes = boundary.getBytes();
            found = false;
            int count = Math.max(Math.min(b.length, b.length
                    - boundary_bytes.length), 0);

            for (int i = 0; i < count; ++i) {
                if (b[i] == boundary_bytes[0]) {
                    boolean matched = true;

                    for (int j = 1; j < boundary_bytes.length; ++j) {
                        if (b[i + j] != boundary_bytes[j]) {
                            matched = false;
                            break;
                        }
                    }

                    if (matched) {
                        found = true;
                        break;
                    }
                }
            }
        } while (found);
        return boundary;
    }

    protected boolean dbKeyExists(long primaryID) {
        Statement stmt;
        ResultSet rs;
        boolean exists = false;
        try {
            stmt = this.connection.createStatement();
            rs = stmt.executeQuery("SELECT COUNT(*) FROM customer WHERE primaryID="
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

    private void log(File file, UploadEvent ue) {

        /* Print message to screen */
        switch (ue) {
            case UPLOADED:
            case SKIPPED:
                System.out.printf("%s %s.\n", file, ue.text());
                break;
            case NOTFOUND:
            case IMGERROR:
                System.err.printf("%s %s.\n", file, ue.text());
                break;
        }

        /* Log to SQL Server */
        if (!this.logging) return;
        try {
            PreparedStatement ps = this.connection.prepareStatement(
                    "INSERT INTO log VALUES (?, ?, DEFAULT)");
            ps.setLong(1, Customer.getId(file));
            ps.setString(2, ue.event());
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class ImageError extends Exception {
    ImageError() {
        super();
    }
}