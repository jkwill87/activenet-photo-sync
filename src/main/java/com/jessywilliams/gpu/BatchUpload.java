package com.jessywilliams.gpu;

import javax.swing.*;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.jessywilliams.gpu.GryphPhotoUpload.execPath;

public class BatchUpload extends SwingWorker<Integer, String> {

    private static final String SQL_INSERT = "INSERT INTO customers VALUES(?,?,?,?)";
    private static final String SQL_DELETE_ID = "DELETE FROM customers WHERE ID = ?";
    private volatile boolean isPaused;
    private int uploadCurrent;
    private ArrayList<Customer> customers = null;
    private ActiveNet activeNetConnection;
    private Connection sqlConnection;
    private GryphPhotoUpload master;

    protected BatchUpload(ActiveNet activeNet, GryphPhotoUpload master) {
        this.uploadCurrent = 0;
        this.activeNetConnection = activeNet;
        this.master = master;
        sqlConnection = createConnection();
        customers = new ArrayList<>();
    }

    private static Connection createConnection() {
        String query = "CREATE TABLE IF NOT EXISTS customers (\n"
                + "	id INTEGER NOT NULL,\n"
                + "	name VARCHAR,\n"
                + "	uploaded BOOLEAN NOT NULL,\n"
                + "	updated TIME NOT NULL,\n"
                + "	PRIMARY KEY (ID)\n"
                + ");";
        String url;
        Connection sqlConnection;
        try {
            url = "jdbc:sqlite:" + execPath() + "/resources/customerRecord.db";
            sqlConnection = DriverManager.getConnection(url);
            Statement sqlStatement = sqlConnection.createStatement();
            sqlStatement.execute(query);
            System.out.println("Using SQLite database file: " + execPath() + "/resources/customerRecord.db");

        } catch (Exception ignored) {
            return null;
        }
        return sqlConnection;
    }

    private static ArrayList<String> getImagePaths(String directory) {
        ArrayList<String> imagePaths = new ArrayList<>();
        File listFile[] = new File(directory).listFiles();
        if (listFile != null) {
            for (File aListFile : listFile) {
                if (aListFile.isDirectory()) {
                    imagePaths.addAll(getImagePaths(aListFile.getAbsolutePath()));
                } else if (aListFile.getName().matches("\\d{0,3}[1-9]\\d{5}\\.jpg")) {
                    imagePaths.add(aListFile.getAbsolutePath());
                }
            }
        }
        return imagePaths;
    }

    protected final void pause() {
        if (!isPaused() && !isDone()) {
            isPaused = true;
            firePropertyChange("paused", false, true);
        }
    }

    protected final void resume() {
        if (isPaused() && !isDone()) {
            isPaused = false;
            firePropertyChange("paused", true, false);
        }
    }

    protected final boolean isPaused() {
        return isPaused;
    }

    @Override
    protected Integer doInBackground() throws Exception {

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
                Thread.sleep(500);
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

    protected int customerCount() {
        if (this.customers == null) return 0;
        return this.customers.size();
    }

    protected ArrayList<Customer> createCustomerList(String imagePath) {
        for (String file : getImagePaths(imagePath)) {
            Customer customer = new Customer(file);
            if (customer.studentNumber == 0) continue;
            if (!photoInDB(customer.studentNumber)) this.customers.add(new Customer(file));
        }

        /*  Sort and remove duplicates*/
        Collections.sort(customers);
        for (int i = 0; i + 1 < this.customers.size(); i++) {
            if (this.customers.get(i).studentNumber == this.customers.get(i + 1).studentNumber)
                this.customers.remove(i--);
        }

        return customers;
    }

    protected boolean photoInDB(int studentNumber) {
        String query = "SELECT CAST(COUNT(1) AS BOOLEAN) AS RESP FROM customers WHERE ID = ? AND (uploaded OR name IS NULL)";
        boolean inDB = false;

        try {
            PreparedStatement ps = sqlConnection.prepareStatement(query);
            ps.setInt(1, studentNumber);
            ResultSet rs = ps.executeQuery();
            inDB = rs.getBoolean("RESP");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return inDB;
    }

    private void updateDB(Customer customer, boolean uploaded) {
        String query = "INSERT OR REPLACE INTO customers(id,name,uploaded,updated) VALUES(?,?,?,?)";
        try {
            PreparedStatement ps = this.sqlConnection.prepareStatement(query);
            ps.setInt(1, customer.studentNumber);
            ps.setString(2, customer.name);
            ps.setBoolean(3, uploaded);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

}
