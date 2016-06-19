package com.jkwill87.gpu;

import java.io.File;
import java.util.Comparator;

class Customer implements Comparator<Customer>, Comparable<Customer> {

    int universityId = 0;
    int activeId = 0;
    String name = null;
    private String filePath = null;

    Customer(String filePath) {
        this.filePath = new File(filePath).getAbsolutePath();
        this.universityId = guessuniversityId();
    }

    String getFilePath() {
        return filePath;
    }

    private int guessuniversityId() {
        if (filePath == null) return 0;
        String student_number;
        student_number = new File(filePath).getName();
        student_number = student_number.substring(0, student_number.length() - 4);
        return Integer.parseInt(student_number);
    }

    public String toString() {
        if (universityId == 0) return null;
        return "0" + Integer.toString(this.universityId);
    }

    public int compare(Customer a, Customer b) {
        return a.universityId - b.universityId;
    }

    public int compareTo(Customer o) {
        return this.universityId - o.universityId;
    }

    public boolean equals(Object object) {
        boolean sameSame = false;

        if (object != null && object instanceof Customer) {
            sameSame = this.universityId == ((Customer) object).universityId;
        }

        return sameSame;
    }
}
