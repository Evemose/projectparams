package org.projectparams.test;

import org.jetbrains.annotations.Nullable;
import org.projectparams.annotations.DefaultValue;

public class Abobus {
    public Abobus(@DefaultValue("3.4") float abobus) {
    }
    public float bibus(@DefaultValue("3.4") float abobus) {
        return this.doubleBibus(new Main());
    }
    public float doubleBibus(Main main, @DefaultValue("3.4") float abobus2) {
        return abobus2;
    }

    protected boolean totos(@DefaultValue("true") boolean abobus) {
        return true;
    }

    public static Abobus abobus(@DefaultValue("3.4") float abobus, @DefaultValue("true") boolean abobus2, @DefaultValue("4") int abobus3) {
        return new Abobus();
    }

    public static class Dodus {
        public Dodus(@DefaultValue("3.4") float abobus) {
        }
        public float bibus(@DefaultValue("3.4") float abobus) {
            return 0;
        }
    }

    public class Bibus extends Abobus {

        public Bibus(@DefaultValue("3.4") float abobus) {
            super(abobus);
        }
        public Bibus ultima(@DefaultValue("3.4") float abobus, @DefaultValue("true") boolean abobus2, @DefaultValue("4") int abobus3) {
            return new Bibus(abobus);
        }
//        public UltimaPopus ultima(@DefaultValue("3.4") float abobus) {
//            return new UltimaPopus();
//        }

        public class UltimaPopus extends Bibus {
            public UltimaPopus(Abobus abobus45, @DefaultValue("3.4") float abobus) {
                super(abobus);
            }
            public UltimaPopus ororos(@DefaultValue("3.4") float abobus) {
                return this.new UltimaPopus(new Abobus());
            }

        }
    }

    public static class Sucus {
        public Sucus(@DefaultValue("3.4") float abobus) {
        }
        public float bibus(@DefaultValue("3.4") float abobus) {
            return 0;
        }
    }

    public static boolean akakus(@DefaultValue("4") long abobus) {
        return true;
    }
}