package org.projectparams.test;

public class Main {
    public static void main(String[] args) {
        var abobus = new Abobus();
        var bibus = abobus.bibus("bibus");
        System.out.println(bibus);
    }
}