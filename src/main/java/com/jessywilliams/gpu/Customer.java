package com.jessywilliams.gpu;

import java.io.File;
import java.util.Comparator;

public class Customer implements Comparator<Customer>, Comparable<Customer> {

    protected String filePath = null;
    protected int studentNumber = 0;
    protected String name = null;

    public Customer(String filePath) {
        this.filePath = new File(filePath).getAbsolutePath();
        this.studentNumber = guessStudentNumber();
    }

    protected String getFilePath() {
        return filePath;
    }

    private int guessStudentNumber() {
        if (filePath == null) return 0;
        String student_number;
        student_number = new File(filePath).getName();
        student_number = student_number.substring(0, student_number.length() - 4);
        return Integer.parseInt(student_number);
    }

    public String toString() {
        if (studentNumber == 0) return null;
        return "0" + Integer.toString(this.studentNumber);
    }

    public int compare(Customer a, Customer b) {
        return a.studentNumber - b.studentNumber;
    }

    public int compareTo(Customer o) {
        return this.studentNumber - o.studentNumber;
    }

    public boolean equals(Object object)
    {
        boolean sameSame = false;

        if (object != null && object instanceof Customer) {
            sameSame = this.studentNumber == ((Customer) object).studentNumber;
        }

        return sameSame;
    }

}
