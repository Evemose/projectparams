package org.projectparams.test;

import org.projectparams.annotations.DefaultValue;

import static org.projectparams.test.Abobus.Dodus;
import static org.projectparams.test.Abobus.someVar;

@SuppressWarnings("all")
public class Main {
    private static final Abobus abobus = new Abobus();

    public static void main(String[] args) {
        var abobus = new Abobus();
        var sucus = new Sucus();
        var classBibus = abobus.new Bibus().new UltimaPopus(new Sucus(), 3.4f).ororos(56f);
        classBibus.bitoto();
        var bibus4 = abobus.bibus();
        var bibus = Abobus.abobus().bibus();
        var bibus2 = new Dodus().bibus();
        var doubleBibus = Abobus.abobus(abobus.doubleBibus(new Main()), Abobus.akakus()).doubleBibus(new Main());
        var abobusSucus = new Abobus.Sucus().bibus();
        rovarus();
        Main.bibus();
        System.out.println(" " + bibus);
        System.out.println(" " + doubleBibus);
        System.out.println(" " + sucus.bibus());
        System.out.println(sucus.bibus() + " " + sucus.bibus());
    }

    private static float bibus(@DefaultValue("0") Float someVar) {
        return 3.4f;
    }

    public static void rovarus(@DefaultValue("someVar") Abobus someVar) {
        System.out.println(" " + someVar);
    }

    public enum Akakus {
        A, B, C
    }

    public static class someVar{}

}