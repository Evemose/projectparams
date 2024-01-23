package org.projectparams.test;

public class Main {
    public static void main(String[] args) {
        var abobus = new Abobus(1);
        //var bibus = abobus.bibus(new Main()).split("b")[0];
        var bibus = Abobus.abobus(abobus.bibus()).bibus();
        var doubleBibus = Abobus.abobus(abobus.doubleBibus(new Main())).doubleBibus(new Main());
        System.out.println(" " + bibus);
        System.out.println(" " + doubleBibus);
    }
}