package aps;

import java.io.Console;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private final static String CSV_FILE_PATH = "customers.csv";
    private final static String IMG_DIR_PATH = "images";

    public static void main(String[] args) {
        System.out.println("ActiveNet Photo Uploader 3.0\n");
        ActiveNet activeNet = login();
        ArrayList<File> images = getImageFiles(new File(IMG_DIR_PATH));

        System.out.println("Found " + images.size() + " image(s).\n");
        int threads = images.size();
        if (threads == 0) return;
        if (threads > 12) threads = 12;

        CustomerStore customerStore;
        try {
            customerStore = new CustomerStore(CSV_FILE_PATH);
        } catch (IOException e) {
            System.out.println("Couldn't open CSV for reading!");
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (File image : images) {
            Runnable worker = new Upload(
                    activeNet.getUrl(),
                    activeNet.getCookieStore(),
                    customerStore,
                    image
            );
            executor.execute(worker);
        }
        executor.shutdown();
        //noinspection StatementWithEmptyBody
        while (!executor.isTerminated()) {
        }
        customerStore.exportCsvFile();
    }

    private static ArrayList<File> getImageFiles(File imageDirectory) {
        ArrayList<File> imagePaths = new ArrayList<>();
        if (!imageDirectory.exists() && !imageDirectory.mkdir()) {
            return imagePaths;
        }
        File listFile[] = imageDirectory.listFiles();
        if (listFile != null) {
            for (File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    imagePaths.addAll(getImageFiles(aListFile));
                } else if (aListFile.getName().matches("\\d+.jpg")) {
                    imagePaths.add(aListFile);
                }
            }
        }
        return imagePaths;
    }

    private static ActiveNet login() {
        Console console = System.console();
        String username;
        String password;
        ActiveNet activeNet;

        System.out.println("Authentication Required");
        while (true) {
            String anDomain = "uofg";
            username = console.readLine("Username? ");
            password = new String(console.readPassword("Password? "));

            /* Try logging in w/ provided credentials, reprompt on error */
            try {
                activeNet = new ActiveNet(username, password, anDomain);
            } catch (LoginError loginError) {
                System.out.println("\nInvalid Credentials, please try again.");
                continue;
            }
            break;
        }
        System.out.println();
        return activeNet;
    }
}
