package org.projectparams.test;

import org.projectparams.annotations.DefaultValue;

import java.util.*;

import static org.projectparams.test.Abobus.Dodus;

@SuppressWarnings("all")
public class Main {
    private static final Abobus abobus = new Abobus();
    private static boolean someVar = false;

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
        var temp = new int[][]{};
        rovarus();
        bokak();
        Main.bibus();
        System.out.println(" " + bibus);
        System.out.println(" " + doubleBibus);
        System.out.println(" " + sucus.bibus());
        System.out.println(sucus.bibus() + " " + sucus.bibus());
    }


    private static float bibus(@DefaultValue("0f") Float someVar) {
        return 3.4f;
    }

    public static void rovarus(@DefaultValue("new org.projectparams.test.Sucus().mains[Sucus.mains[1].getZero()]." +
            "<Map<Integer, java.util.List<Float>>>akakus(Map.of(3, List.of((float)(double)6.d)))")
                               List<Map<Integer, List<Float>>>
                                       someVar) {
        System.out.println(" " + someVar);
        System.out.println(someVar.getFirst().getClass());
    }

    public static void bokak(@DefaultValue("new int[][][]{{{0}, {2, 4}}, {{}}, {}}") int[][][] someVar) {
        System.out.println("bibus " + Arrays.deepToString(someVar));
    }

    public enum Akakus {
        A, B, C
    }

    public static <T> List<T> akakus(T t) {
        return new ArrayList<T>(List.of(t));
    }

    public static class someVar{}

    private static int getZero() {
        return 0;
    }

    private static int getFive() {
        return 5;
    }

}