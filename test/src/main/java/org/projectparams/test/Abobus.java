package org.projectparams.test;
import org.projectparams.annotations.DefaultValue;
public class Abobus {
    public String bibus(@DefaultValue("true") boolean abobus) {
        return abobus ? "abobus" : "bibus";
    }
}