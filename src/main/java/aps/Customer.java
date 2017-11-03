package aps;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;

class Customer implements Comparator<Customer>, Comparable<Customer> {

    int primaryID = 0;
    int activeID = 0;
    String name = null;
    String email = null;
    boolean isStudent;

    Customer(File imagePath) throws IOException {
        if (!imagePath.isFile()) throw new IOException();
        String parsed = imagePath.getName().replaceAll("[^0-9]", "");
        this.primaryID = (parsed.isEmpty() || parsed.length() > 11)
                ? 0 : Integer.parseInt(parsed);
        this.isStudent = this.primaryID < 1000000;
    }

    Customer(int primaryID, int activeID, String name, String email) {
        this.primaryID = primaryID;
        this.activeID = activeID;
        this.name = name;
        this.email = email;
        this.isStudent = this.primaryID < 1000000;
    }

    public String toString() {
        String asString = Long.toString(this.primaryID);
        return isStudent ? "0" + asString : asString;
    }

    public int compare(Customer a, Customer b) {
        return (int) (a.primaryID - b.primaryID);
    }

    public int compareTo(Customer o) {
        return (int) (this.primaryID - o.primaryID);
    }

    public boolean equals(Object object) {
        boolean sameSame = false;
        if (object != null && object instanceof Customer) {
            sameSame = Objects.equals(this.primaryID,
                    ((Customer) object).primaryID);
        }
        return sameSame;
    }

    @Override
    public int hashCode() {
        return this.primaryID;
    }
}
