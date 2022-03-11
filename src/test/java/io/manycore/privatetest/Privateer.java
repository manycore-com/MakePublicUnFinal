package io.manycore.privatetest;

public class Privateer {

    final private int abc;
    final private boolean odd;

    public Privateer(int abc, boolean odd) {
        this.abc = abc;
        this.odd = odd;
    }

    final static private Privateer privateFactory(int abc) {
        if (0 == (1 & abc)) {
            return new Privateer(abc, true);
        } else {
            return new Privateer(abc, false);
        }
    }

    final static protected Privateer protectedFactory(int abc) {
        return privateFactory(abc);
    }

    final static public Privateer publicFactory(int abc) {
        return protectedFactory(abc);
    }

    final private int privateAbc() {
        return abc;
    }

    final protected int protectedAbc() {
        return abc;
    }

    final public int publicAbc() {
        return abc;
    }

    final static private void privateTest() {
        Privateer x = publicFactory(13);
        Privateer y = privateFactory(3);
        if (x.odd != y.odd) {
            throw new RuntimeException("odd error");
        }

        if (x.privateAbc() != x.publicAbc()) {
            throw new RuntimeException("abc error");
        }
    }

    final static public void publicTest() {
        privateTest();
    }

}
