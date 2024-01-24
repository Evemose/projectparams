package org.projectparams.test;

import org.projectparams.annotations.DefaultValue;

public class Abobus {
    public Abobus(@DefaultValue("3.4") float abobus) {
    }
    public float bibus(@DefaultValue("3.4") float abobus) {
        return abobus;
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

    public class Bibus extends Abobus {
        public Bibus(@DefaultValue("3.4") float abobus) {
            super(abobus);
        }
//        public UltimaPopus ultima(@DefaultValue("3.4") float abobus) {
//            return new UltimaPopus();
//        }

        public class UltimaPopus extends Bibus {
            public UltimaPopus(@DefaultValue("3.4") float abobus) {
                super(abobus);
            }
//            public UltimaPopus ororos(@DefaultValue("3.4") float abobus) {
//                return Bibus.this.new UltimaPopus();
//            }
        }
    }

    public static boolean akakus(@DefaultValue("4") long abobus) {
        return true;
    }
}