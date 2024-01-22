package org.projectparams.test;

public class Main {
    String abobus = new Abobus().bibus();
    public static void main(String[] args) {
        var abobus = new Abobus();
        var bibus = abobus.bibus().split("b")[0];
        System.out.println(" " + bibus);
    }
}