package com.jkwill87.gpu;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.sikuli.script.Key;
import org.sikuli.script.Screen;

import java.io.File;

import static com.jkwill87.gpu.GryphPhotoUpload.execPath;
import static java.lang.System.exit;

public class ActiveNet {

    static final int SUCCESS = 0;
    static final int FAILURE = 1;
    static final int SKIPPED = 2;

    private WebDriver webDriver = null;
    private boolean _isAuthenticated = false;
    private String _execPath;
    private String username;
    private String password;

    public ActiveNet() {
        if (init() == FAILURE) exit(1);
    }

    private static boolean check(String path, int time, boolean click) {
        Screen s = new Screen();
        for (int i = 0; i < time; i++) {
            if (s.exists(path) != null) {
                if (click) {
                    s.click();
                }
                return true;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        return false;
    }

    int authenticate(String username, String password) {

        /* Exit early if a connection has not yet been made */
        if (!this.isConnected()) return FAILURE;

        webDriver.get("https://anprodca.active.com/uofg/servlet/processAssignWorkstation.sdi");

        /* Wait 1 second to give password a chance to auto-complete */
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        /* Type in username */
        WebElement username_field = webDriver.findElement(By.name("uname"));
        username_field.clear();
        username_field.sendKeys(username);

        /* Type in password */
        WebElement password_field = webDriver.findElement(By.name("adminloginpassword"));
        password_field.clear();
        password_field.sendKeys(password);

        /* Check to see if login succeeded */
        webDriver.findElement(By.name("login")).submit();
        if (!webDriver.getTitle().equals("ActiveNet - University of Guelph")) return FAILURE;

        /* Select default workstation */
        webDriver.get("https://anprodca.active.com/uofg/servlet/processAssignWorkstation.sdi");
        webDriver.findElement(By.name("ws1")).submit();
        this._isAuthenticated = true;
        this.username = username;
        this.password = password;
        return SUCCESS;
    }

    boolean isConnected() {
        return this.webDriver != null;
    }

    boolean isAuthenticated() {
        return this._isAuthenticated;
    }

    int findCustomer(Customer customer) {

        /* Abort early if not connected or authenticated */
        if (!isConnected() || !isAuthenticated()) return FAILURE;

        try {
            /* Go to customer search page */
            webDriver.get("https://anprodca.active.com/uofg/servlet/admin.sdi?oc=Customer");
            WebElement id_entry = webDriver.findElement(By.id("akt_value_1"));

            /* Enter student number prepended by zero */
            id_entry.sendKeys(customer.toString());
            WebElement form = webDriver.findElement(By.name("akt_use_as_direct_access"));
            form.submit();

            /* Success if page name begins with "Customer:" */
            if (!webDriver.getTitle().contains("Customer:")) return FAILURE;

            /* Find customer name */
            customer.name = webDriver.findElement(By.className("tlbl")).findElement(By.xpath("./following-sibling::td")).getText();

            /* Find customer activeId */
            customer.activeId = Integer.parseInt(webDriver.getTitle().replaceAll("[^\\d.]", ""));
        } catch (Exception e) {
            return FAILURE;
        }

        /* Skip customers who already have their image uploded */
        try {
            webDriver.findElement(By.id("customer_photo_img"));
        } catch (Exception e) {
            return SUCCESS;
        }
        return SKIPPED;
    }

    int upload(Customer customer) {

        try {
            webDriver.get("https://anprodca.active.com/uofg/servlet/takePhoto.sdi?customer_id=" + customer.activeId);
        } catch (Exception e) {
            return FAILURE;
        }

        Screen s;
        s = new Screen();
        int wait_time;

        /* Wait 1 minute for upload button to show up */
        if (!check(_execPath + "/resources/upload_button.png", 60, true)) return FAILURE;

        /* Wait 1.5 seconds */
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
        s.type(customer.getFilePath());
        s.type(Key.ENTER);

        /* Verify upload was successful */
        if (!check(_execPath + "/resources/picture_saved.png", 30, false)) return FAILURE;
        return SUCCESS;
    }

    int reset(Customer customer) {
        if (!this.isConnected()) return FAILURE;
        this.kill();
        if (this.init() == FAILURE) return FAILURE;
        if (this.authenticate(this.username, this.password) != SUCCESS) return FAILURE;
        if (this.findCustomer(customer) != SUCCESS) return FAILURE;
        return SUCCESS;
    }

    void kill() {
        if (!isConnected()) return;
        this.webDriver.quit();
        this.webDriver = null;
    }

    private int init() {
        try {
            _execPath = execPath();
            String OS = System.getProperty("os.name").toLowerCase();

            if (OS.contains("win")) {
                File file = new File(_execPath + "/resources/IEDriverServer.exe");
                System.setProperty("webdriver.ie.driver", file.getAbsolutePath());
                webDriver = new InternetExplorerDriver();
            } else if (OS.contains("mac")) {
                webDriver = new ChromeDriver();
            }
            return SUCCESS;
        } catch (Exception e) {
            System.out.println(e.toString());
            return FAILURE;
        }
    }
}
