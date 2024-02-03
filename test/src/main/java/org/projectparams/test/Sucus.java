package org.projectparams.test;

import org.projectparams.annotations.DefaultValue;

@SuppressWarnings("all")
public class Sucus extends Abobus {
    public static final Main[] mains = new Main[3];
    {
        mains[0] = new Main();
        mains[1] = new Main();
        mains[2] = new Main();
    }
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

    public static float momos(@DefaultValue("657") Float abobus) {
        return 3.4f;
    }
}
