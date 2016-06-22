package com.jkwill87.gpu;

import javax.swing.*;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jkwill87.gpu.GryphPhotoUpload.execPath;

class BatchUpload extends SwingWorker<Integer, String> {

    private static final String SQL_INSERT = "INSERT INTO customers VALUES(?,?,?,?,?)";
    private static final String SQL_DELETE_ID = "DELETE FROM customers WHERE universityId = ?";
    private volatile boolean isPaused;
    private int uploadCurrent;
    private ArrayList<Customer> customers = null;
    private ActiveNet activeNetConnection;
    private Connection sqlConnection;
    private GryphPhotoUpload master;

    BatchUpload(ActiveNet activeNet, GryphPhotoUpload master, String imagePath) {
        this.uploadCurrent = 0;
        this.activeNetConnection = activeNet;
        this.master = master;

        /* Open else creates a SQLite database to record upload transactions */
        String query = "CREATE TABLE IF NOT EXISTS customers (\n"
                + "	universityId INTEGER NOT NULL,\n"
                + "	activeId INTEGER,\n"
                + "	name VARCHAR,\n"
                + "	uploaded BOOLEAN NOT NULL,\n"
                + "	updated TIME NOT NULL,\n"
                + "	PRIMARY KEY (universityId)\n"
                + ");";
        try {
            String url = "jdbc:sqlite:" + execPath() + "/resources/customerRecord.db";
            sqlConnection = DriverManager.getConnection(url);
            Statement sqlStatement = sqlConnection.createStatement();
            sqlStatement.execute(query);
            System.out.println("Using SQLite database file: " + execPath() + "/resources/customerRecord.db");

        } catch (Exception ignored) {
            sqlConnection = null;
        }

        /* Create an ArrayList of unique customer images */
        customers = new ArrayList<>();
        for (String file : getImagePaths(imagePath)) {
            Customer customer = new Customer(file);
            if (customer.universityId == 0) continue;
            if (!photoInDB(customer.universityId)) customers.add(new Customer(file));
        }

        /*  Sort unique entries by student or employee number */
        Collections.sort(customers);
        for (int i = 0; i + 1 < customers.size(); i++) {
            if (customers.get(i).universityId == customers.get(i + 1).universityId)
                customers.remove(i--);
        }
    }

    private static ArrayList<String> getImagePaths(String directory) {
        ArrayList<String> imagePaths = new ArrayList<>();
        File listFile[] = new File(directory).listFiles();
        if (listFile != null) {
            for (File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    imagePaths.addAll(getImagePaths(aListFile.getAbsolutePath()));
//                } else if (aListFile.getName().matches("\\d{0,3}[1-9]\\d{5}\\.jpg")) {
                } else if (aListFile.getName().matches("\\d{9}\\.jpg")) {
                    imagePaths.add(aListFile.getAbsolutePath());
                }
            }
        }
        return imagePaths;
    }

    /**
     * Stops the SwingWorker from possessing the next photo upload.
     */
    final void pause() {
        if (!isPaused() && !isDone()) {
            isPaused = true;
            firePropertyChange("paused", false, true);
        }
    }

    /**
     * Resumes the SwingWorker if paused, allowing it proceed to uploading the next photo.
     */
    final void resume() {
        if (isPaused() && !isDone()) {
            isPaused = false;
            firePropertyChange("paused", true, false);
        }
    }

    /**
     * @return true if SwingWorker paused else false.
     */
    final boolean isPaused() {
        return isPaused;
    }

    /**
     * Provides for the total number of images queued to be uploaded.
     *
     * @return the number of queued images as an integer.
     */
    int customerCount() {
        if (this.customers == null) return 0;
        return this.customers.size();
    }

    /**
     * Processes image uploads on a separate, interruptable thread.
     *
     * @return 0 on success, 1 on failure.
     */
    @Override
    protected Integer doInBackground() {

        int resets = 0;
        int maxResets = 3;
        int searchStatus, updateStatus;
        Customer currentCustomer;

        while (uploadCurrent < customers.size()) {
            if (!isPaused()) {

                currentCustomer = customers.get(uploadCurrent);

                /* Search for customer on ActiveNet */
                searchStatus = activeNetConnection.findCustomer(currentCustomer);
                switch (searchStatus) {
                    case ActiveNet.SUCCESS:
                        System.out.println(currentCustomer + " found");
                        break;
                    case ActiveNet.SKIPPED:
                        System.out.println(currentCustomer + " skipped");
                        updateDB(currentCustomer, true);
                        break;
                    case ActiveNet.FAILURE:
                        System.out.println(currentCustomer + " not found");
                        updateDB(currentCustomer, false);
                        break;
                }

                /* Upload customer photo */
                if (searchStatus == ActiveNet.SUCCESS) {
                    do {
                        updateStatus = activeNetConnection.upload(customers.get(uploadCurrent));

                        switch (updateStatus) {
                            case ActiveNet.SUCCESS:
                                resets = 0;
                                updateDB(currentCustomer, true);
                                break;
                            case ActiveNet.FAILURE:
                            default:  // shouldn't happen
                                updateDB(currentCustomer, false);
                                activeNetConnection.reset(currentCustomer);
                                System.out.printf("upload error: retry %d/%d\n", resets, maxResets);
                                resets++;
                                break;
                        }
                    } while (updateStatus != ActiveNet.SUCCESS && updateStatus < maxResets);

                    /* Abort if error count exceeds maximum */
                    if (updateStatus == maxResets) {
                        master.uploadButton.setVisible(false);
                        master.progressBar.setVisible(false);
                        JOptionPane.showMessageDialog(null,
                                "Fatal Error, Aborting!",
                                "Notice",
                                JOptionPane.ERROR_MESSAGE);
                        return 1;
                    }
                }

                setProgress(++uploadCurrent * 100 / customers.size());
                publish(uploadCurrent + " of " + customers.size());

            } else {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        master.uploadButton.setVisible(false);
        master.progressBar.setVisible(false);
        JOptionPane.showMessageDialog(null,
                "Finished!",
                "Notice",
                JOptionPane.INFORMATION_MESSAGE);

        return 0;
    }

    @Override
    protected void process(List<String> chunks) {
        master.progressLabel.setText(chunks.get(0));
    }

    private boolean photoInDB(int universityId) {
        String query = "SELECT CAST(COUNT(1) AS BOOLEAN) AS RESP FROM customers WHERE universityId = ? AND (uploaded OR name IS NULL)";
        boolean inDB = false;

        try {
            PreparedStatement ps = sqlConnection.prepareStatement(query);
            ps.setInt(1, universityId);
            ResultSet rs = ps.executeQuery();
            inDB = rs.getBoolean("RESP");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return inDB;
    }

    private void updateDB(Customer customer, boolean uploaded) {
        String query = "INSERT OR REPLACE INTO customers(universityId,activeId,name,uploaded,updated) VALUES(?,?,?,?,?)";
        try {
            PreparedStatement ps = this.sqlConnection.prepareStatement(query);
            ps.setInt(1, customer.universityId);
            ps.setInt(2, customer.activeId);
            ps.setString(3, customer.name);
            ps.setBoolean(4, uploaded);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
}
