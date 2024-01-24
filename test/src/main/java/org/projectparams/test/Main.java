package org.projectparams.test;

import static org.projectparams.test.Abobus.*;

public class Main {
    public static void main(String[] args) {
        var abobus = new org.projectparams.test.Abobus();
        var sucus = new Sucus(3);
        var classBibus = new Bibus();
        var bibus4 = abobus.bibus();
        var bibus = Abobus.abobus().bibus();
        var doubleBibus = Abobus.abobus(abobus.doubleBibus(new Main()), Abobus.akakus()).doubleBibus(new Main());
        System.out.println(" " + bibus);
        System.out.println(" " + doubleBibus);
        System.out.println(" " + sucus.bibus());
    }
}