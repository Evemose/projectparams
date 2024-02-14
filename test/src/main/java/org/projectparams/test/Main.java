package org.projectparams.test;

import org.projectparams.annotations.DefaultValue;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

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
        rovarus();
        bokak();
        Main.bibus();
        System.out.println(" " + bibus);
        System.out.println(" " + doubleBibus);
        System.out.println(" " + sucus.bibus());
        System.out.println(sucus.bibus() + " " + sucus.bibus());
        System.out.println(samovar());
        System.out.println(newnew());
        Main.<String>params();
    }

    private static class Tororos {
        private static final int someVar = 3;
    }


    private static float bibus(@DefaultValue("0f") float someVar) {
        return 3.4f;
    }

    private static <T> T bibonus() {
        return null;
    }

    public static <T> void params(@DefaultValue("Main::<T>bibonus") Supplier<T> t) {
        System.out.println(" " + t.get());
    }

    public static void rovarus(@DefaultValue("new org.projectparams.test.Sucus().mains[Sucus.mains[1].getZero()]." +
            "<Map<Integer, List<Float>>>akakus(Map.of(3, List.of((float)(double)6.d))).isEmpty() ? " +
            "!(true ? false : true) ? <Map<Integer, List<Float>>>akakus(Map.of(10, List.of(0.55f))) : List.of(new HashMap<>(Map.of(3, List.of((float)(double)6.d))))" +
            ": List.of(new HashMap<>(Map.of(3, List.of((float)(double)6.d))))")
                               List<Map<Integer, List<Float>>> someVar) {
        System.out.println(" " + someVar);
        System.out.println(someVar.getFirst().getClass());
    }

    public static void bokak(@DefaultValue("new int[][][]{{{0}, {2, 4}}, {{}}, {}}") int[][][] someVar) {
        System.out.println("bibus " + Arrays.deepToString(someVar));
    }
    private static String temp() {
        return "temp";
    }

    private static String newnew(@DefaultValue("Tororos::new") Supplier<Tororos> sup) {
        return String.valueOf(sup.get());
    }

    private static String samovar(@DefaultValue("Main::temp") Supplier<String> someVar) {
        return someVar.get();
    }


    public enum Akakus {
        A, B, C
    }

    private static void akoroyos(@DefaultValue("abobus.new Bibus()") Abobus.Bibus someVar) {
        System.out.println(" " + someVar);
    }

    private static void somavarus(@DefaultValue("5 + 3.4d") double someVar) {
        System.out.println(" " + someVar);
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