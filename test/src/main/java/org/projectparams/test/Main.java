package org.projectparams.test;

import org.projectparams.annotations.DefaultValue;

public class Main {
    public static void main(String[] args) {
        var abobus = new Abobus(1);
        //var bibus = abobus.bibus(new Main()).split("b")[0];
        var bibus = Abobus.abobus(abobus.bibus()).bibus();
        System.out.println(" " + bibus);
    }
}