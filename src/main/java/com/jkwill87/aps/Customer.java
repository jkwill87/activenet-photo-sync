package com.jkwill87.aps;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;

class Customer implements Comparator<Customer>, Comparable<Customer> {

    long primaryID = 0;
    int activeID = 0;

    String name = null;
    String email = null;
    boolean isStudent;

    Customer(File srcFile) throws IOException {
        if (!srcFile.isFile()) throw new IOException();
        this.primaryID = getId(srcFile);
        this.isStudent = this.primaryID < 1000000;
    }

    private static long getId(File file) {
        String parsed = file.getName().replaceAll("[^0-9]", "");
        return (parsed.isEmpty() || parsed.length() > 11)
                ? 0 : Long.parseLong(parsed);
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
}
