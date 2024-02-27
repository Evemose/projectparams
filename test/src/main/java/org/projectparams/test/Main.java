package org.projectparams.test;

import org.projectparams.annotations.DefaultValue;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import static java.util.Objects.requireNonNullElse;

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
        classBibus.bitoto();
        classBibus.bitoto();
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
        //List.of(1, 2, 3).forEach(Main::somavarus);
        Function<Integer, Integer> someVar = Main::getOne;
        lambda();
    }

    public Main() {
    }

    private static void lambda(@DefaultValue("() -> getOne(2)") Supplier<Integer> someVar,
                               @DefaultValue("(param) -> someVar") Predicate<Integer> someVar2) {
        System.out.println(" " + someVar.get());
    }

    public Main(int i) {
        var m = new org.projectparams.test.Main(Main::getOne);
    }

    public Main(Function<Integer, Integer> someVar) {
    }

    private static int getOne(int i, @DefaultValue("Main::getZero") Supplier<Integer> someVar) {
        Objects.requireNonNullElse(someVar, Main::getZero);
        requireNonNullElse(someVar, Main::getZero);
        return 1;
    }

    private static void t(@DefaultValue("Main::new") Function<Integer, Main> someVar) {
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

    // function
    private static String temp(Integer i, @DefaultValue("Main::getZero") Supplier<Integer> someVar) {
        return "temp";
    }

    private static String newnew(@DefaultValue("Tororos::new") Supplier<Tororos> sup) {
        return String.valueOf(sup.get());
    }

    private static String samovar(@DefaultValue("Main::temp") Function<Integer, String> someVar) {
        return someVar.apply(5);
    }

    public enum Akakus {
        A, B, C
    }

    private static void akoroyos(@DefaultValue("abobus.new Bibus()") Abobus.Bibus someVar) {
        System.out.println(" " + someVar);
    }

    private static void somavarus(@DefaultValue("5 + 3.4d") Double someVar, @DefaultValue("4") int someInt) {
        System.out.println(" " + someVar + " " + someInt);
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