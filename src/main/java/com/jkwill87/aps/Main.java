package com.jkwill87.aps;

import java.io.Console;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

    private final static String TITLE = "ActiveNet Photo Uploader 2.0";

    private ActiveNet activeNet;
    private Scanner scanner = new Scanner(System.in);
    private String username;
    private String password;

    public static void main(String[] args) {

        Main menu = new Main();

        /* Present user with main menu, get selection */
        String[] choices = {
                "Query Database",
                "Upload Images",
                "EXIT PROGRAM"
        };
        int selection;

        /* Redirect to sub-menu based on selection */
        do {
            selection = menu.menuGen(TITLE, choices);
            switch (selection) {
                case 0:
                    menu.query();
                    break;
                case 1:
                    menu.upload();
                    break;
            }
        } while (selection != 2);

        try {
            menu.activeNet.getSqlConnection().close();
        } catch (SQLException | NullPointerException ignored) {
        }
        System.out.println("Exiting");
    }

    private static ArrayList<File> getImagePaths(File directory) {
        ArrayList<File> imagePaths = new ArrayList<>();
        File listFile[] = directory.listFiles();
        if (listFile != null) {
            for (File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    imagePaths.addAll(getImagePaths(aListFile));
                } else if (aListFile.getName().matches("\\d+.jpg")) {
                    imagePaths.add(aListFile);
                }
            }
        }
        return imagePaths;
    }

    private static void clearConsole() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private void upload() {
        getCredentials();
        ArrayList<File> images = getImages();
        System.out.println("\nFound " + images.size() + " images.\n");
        int threads = images.size();
        if (threads == 0) return;
        if (threads > 12) threads = 12;

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        for (File image : images) {
            Runnable worker = new Upload(
                    activeNet.getUrl(),
                    activeNet.getSqlConnection(),
                    activeNet.getCookieStore(),
                    image,
                    false  // logging disabled by default
            );
            executor.execute(worker);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {
        }

        System.out.println("Press Enter to Continue");
        scanner.nextLine();
    }

    private void query() {

        String field, search;
        ArrayList<String> results;
        String[] menu = {
                "Primary ID",
                "Active ID",
                "Name",
                "Email",
                "CANCEL / GO BACK"
        };

        while (true) {
            int choice = menuGen("Query Sub-menu", menu);
            switch (choice) {
                case 0:
                    field = "primaryID";
                    break;
                case 1:
                    field = "activeID";
                    break;
                case 2:
                    field = "name";
                    break;
                case 3:
                    field = "email";
                    break;
                default:
                    return;
            }

            getCredentials();

            System.out.print("\nSearch? ");
            search = scanner.nextLine();

            results = activeNet.dbQuery(field, search);
            System.out.println("\nResults:");

            if (results.isEmpty()) System.out.println("None");
            else for (int i = 0; i < results.size(); i++)
                System.out.println((i + 1) + ") " + results.get(i));

            System.out.println("Press Enter to Continue");
            scanner.nextLine();
        }
    }

    private ArrayList<File> getImages() {
        String userInput;
        File userPath;
        ArrayList<File> fileList;

        while (true) {
            System.out.format("Directory to Upload? ");
            userInput = scanner.nextLine();
            if (userInput.length() != 0)
                try {
                    userPath = new File(userInput);
                    if (!userPath.isDirectory()) {
                        System.out.format("\nInvalid input. Please try again.\n");
                        continue;
                    }

                    fileList = getImagePaths(userPath);
                    break;

                } catch (Exception ignored) {
                    System.out.format("\nInvalid input. Please try again.\n");
                    continue;
                }

            System.out.format("\nInvalid input. Please try again.\n");
        }
        return fileList;
    }

    private int menuGen(String heading, String[] entries) {
        int userInput;

        clearConsole();
        System.out.println("\n" + heading.toUpperCase());
        for (int i = 0; i < heading.length(); i++)
            System.out.print("-");
        System.out.print("\n\n");

        for (int i = 0; i < entries.length; i++) {
            System.out.println((i + 1) + ") " + entries[i]);
        }

        outerLoop:
        while (true) {

            /* Prompt user for choice */
            System.out.format("\nYour Choice? ");

            /* Disregard non alpha responses */
            while (!scanner.hasNextInt()) {
                scanner.next();
                System.out.println("Invalid input. Please try again.");
                continue outerLoop;
            }

            /* Validate integer response is within range */
            userInput = scanner.nextInt();
            if (userInput < 1 || userInput > entries.length) {
                System.out.println("Invalid input. Please try again.");
                continue;
            }

            break;
        }
        scanner.nextLine();
        System.out.println();
        return userInput - 1;
    }

    private void getCredentials() {

        Console console = System.console();


        if (this.username != null && this.password != null) return;

        System.out.println("Authentication Required");

        while (true) {

            String mysqlUrl = console.readLine("MYSQL Server? ");
            String anDomain = console.readLine("ActiveNet Domain? ");
            this.username = console.readLine("Username? ");
            this.password = new String(console.readPassword("Password? "));

            /* Try logging in w/ provided credentials, reprompt on error */
            try {
                this.activeNet = new ActiveNet(
                        username, password, mysqlUrl, anDomain);
            } catch (LoginError loginError) {
                System.out.println("\nInvalid Credentials, please try again.");
                this.username = this.password = null;
                continue;
            }
            break;
        }
        System.out.println();
        System.out.println(activeNet.getCustomerCount() + " images on server.");
    }


}
