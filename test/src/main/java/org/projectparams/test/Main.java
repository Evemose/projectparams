package org.projectparams.test;

public class Main {
    public static void main(String[] args) {
        var abobus = new Abobus(2);
        var sucus = new Sucus(3);
        //var bibus = abobus.bibus(new Main()).split("b")[0];
        var bibus = Abobus.abobus().bibus();
        var doubleBibus = Abobus.abobus(abobus.doubleBibus(new Main()), Abobus.akakus()).doubleBibus(new Main());
        System.out.println(" " + bibus);
        System.out.println(" " + doubleBibus);
        System.out.println(" " + sucus.bibus());
    }
}