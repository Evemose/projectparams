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

    public static class Bibus {
        public Bibus(@DefaultValue("3.4") float abobus) {
        }
        public static float bibus(@DefaultValue("3.4") float abobus) {
            return abobus;
        }
    }

    public static boolean akakus(@DefaultValue("4") long abobus) {
        return true;
    }
}