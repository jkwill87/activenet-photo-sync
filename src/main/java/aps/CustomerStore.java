package aps;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import java.io.*;
import java.util.HashSet;

public class CustomerStore {

    private String csvFilePath;
    private HashSet<Customer> customers;
    private String[] headings = {
            "primaryID",
            "activeID",
            "name",
            "email"
    };

    public CustomerStore(String path) throws IOException {
        this.customers = new HashSet<>();
        this.csvFilePath = path;

        File file = new File(csvFilePath);
        if (file.length() > 0) {
            importCsvFile();
        }
    }

    private static String[] customerToArray(Customer customer) {
        return new String[]{
                String.valueOf(customer.primaryID),
                String.valueOf(customer.activeID),
                customer.name,
                customer.email
        };
    }

    public boolean importCsvFile() throws FileNotFoundException {
        FileReader fileReader = new FileReader(this.csvFilePath);
        try {
            CSVReader reader = new CSVReader(fileReader);
            String[] nextLine = reader.readNext();  // skip header
            int primaryID;
            int activeID;
            String name;
            String email;
            while ((nextLine = reader.readNext()) != null) {
                primaryID = Integer.parseInt(nextLine[0]);
                activeID = Integer.parseInt(nextLine[1]);
                name = nextLine[2];
                email = nextLine[3];
                this.customers.add(new Customer(primaryID, activeID, name, email));
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean exportCsvFile() {
        try {
            FileWriter fileWriter = new FileWriter(csvFilePath + ".tmp", true);
            CSVWriter writer = new CSVWriter(fileWriter);
            File file = new File(csvFilePath + ".tmp");
            writer.writeNext(this.headings);
            for (Customer customer : this.customers) {
                writer.writeNext(customerToArray(customer));
            }
            writer.flush();
            writer.close();
            return file.renameTo(new File(csvFilePath));
        } catch (IOException e) {
            return false;
        }
    }

    public void customer_add(Customer customer) throws IOException {
        this.customers.add(customer);
    }

    public boolean customer_included(Customer customer) throws IOException {
        return this.customers.contains(customer);
    }


    public void customer_rm(Customer customer) {
        this.customers.remove(customer);
    }


    public int length() {
        return this.customers.size();
    }
}
