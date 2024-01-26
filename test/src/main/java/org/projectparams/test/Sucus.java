package org.projectparams.test;

import org.projectparams.annotations.DefaultValue;

@SuppressWarnings("unused")
public class Sucus extends Abobus {
    public Sucus(@DefaultValue("3.4") float abobus) {
        super(abobus);
    }
    @Override
    public float bibus(@DefaultValue("5") Double abobus) {
        return (float) abobus.doubleValue();
    }

    private void cocos(@DefaultValue Float abobus) {
    }

    public void papus() {
        cocos();
        totos();
    }
}
