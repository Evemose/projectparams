package org.projectparams.test;
import org.projectparams.annotations.DefaultValue;
public class Abobus {
    public int bibus(@DefaultValue("1") int abobus) {
        return abobus;
    }
}