package org.projectparams.test;
import org.projectparams.annotations.DefaultValue;
public class Abobus {
    public String bibus(@DefaultValue("abobus") String abobus) {
        return abobus;
    }
}