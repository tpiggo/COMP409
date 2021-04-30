package longformfinal;

import java.util.ArrayList;

public class Test {
    private static void loopSim() {
        InputActor inp =  (InputActor) Factory.createActor("input");
        Actor inc = Factory.createActor("inc");
        Actor out = Factory.createActor("output");
        Actor lt10 = Factory.createActor("<", 10);
        Actor f1 = Factory.createActor("fork");
        Actor f2 = Factory.createActor("fork");
        Actor f3 = Factory.createActor("fork");
        Actor merge = Factory.createActor("merge");
        Actor s = Factory.createActor("switch");
        ArrayList<Channel> c = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            c.add(Factory.createChannel());
        }
        merge.connectIn(c.get(0), 0);
        // true
        merge.connectIn(c.get(1), 1);
        // false
        merge.connectIn(c.get(2), 2);
        c.get(0).set(0);
        merge.connectOut(c.get(3), 0);
        f1.connectIn(c.get(3), 0);
        f1.connectOut(c.get(4), 0);
        f1.connectOut(c.get(5), 1);
        lt10.connectIn(c.get(5),0);
        lt10.connectOut(c.get(6), 0);
        f2.connectIn(c.get(6), 0);
        f2.connectOut(c.get(7), 0);
        // back to merge
        f2.connectOut(c.get(0), 1);
        // from forked lt10
        s.connectIn(c.get(7), 0);
        s.connectIn(c.get(4), 1);
        s.connectOut(c.get(8), 0);
        s.connectOut(c.get(9), 1);
        f3.connectIn(c.get(8), 0);
        f3.connectOut(c.get(10), 0);
        f3.connectOut(c.get(11), 1);
        inc.connectIn(c.get(10), 0);
        out.connectIn(c.get(11), 0);
        inc.connectOut(c.get(1), 0);
        inp.connectOut(c.get(2),0);
        Simulation.start();
    }

    private static void reusablePartial() {
        Actor add = Factory.createActor("add");
        Actor out = Factory.createActor("output");
        Actor lt10 = Factory.createActor("<", 10);
        Actor f1 = Factory.createActor("fork");
        Actor f2 = Factory.createActor("fork");
        Actor f3 = Factory.createActor("fork");
        Actor merge = Factory.createActor("merge");
        Actor s = Factory.createActor("switch");
        ArrayList<Channel> c = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            c.add(Factory.createChannel());
        }

    }

    private static void partialSum(int j) {
        Actor add = Factory.createActor("add");
        Actor out = Factory.createActor("output", j);
        Actor f1 = Factory.createActor("fork");
        Actor f2 = Factory.createActor("fork");
        Actor sw = Factory.createActor("switch");
        Actor merge = Factory.createActor("merge");
        Actor zero = Factory.createActor("constant", 0);
        Channel c1 = Factory.createChannelLinkActor(add, f1, 0, 0);
        Channel boolin = Factory.createChannelLinkActor(null, f2, 0, 0);
        Channel c9 = Factory.createChannelLinkActor(f2, merge, 0, 0);
        Channel c10 = Factory.createChannelLinkActor(f2, sw, 0, 0);
        Channel c2 = Factory.createChannelLinkActor(f1, sw, 0, 1);
        Channel c6 = Factory.createChannelLinkActor(sw, merge, 0,1);
        Channel c5 = Factory.createChannelLinkActor(sw, zero, 1,0);
        Channel c7= Factory.createChannelLinkActor(zero, merge, 0, 2);
        Channel c8 = Factory.createChannelLinkActor(merge, add, 0, 0);
        Channel c3 = Factory.createChannelLinkActor(f1, out, 1, 0);
        Channel c4 = Factory.createChannelLinkActor(null, add, 0, 1);
        for (int i = 0; i < j; i++ ) {
            c4.set(1);
            if (Math.random() <= 0.1) {
                boolin.set(0);
            } else  {
                boolin.set(1);
            }
        }
        c8.set(0);
        Simulation.start();
    }

    public static void loopSim(int j, int m) {
        Actor f1 = Factory.createActor("fork");
        Actor one = Factory.createActor("constant", 1);
        Actor output = Factory.createActor("output", m*j);
        Actor inc = Factory.createActor("inc");
        Actor dec = Factory.createActor("dec");
        Actor merge = Factory.createActor("merge");
        Actor f2 = Factory.createActor("fork");
        Actor gt = Factory.createActor(">", 0);
        Actor f3 = Factory.createActor("fork");
        Actor dec2 = Factory.createActor("dec");
        Actor sw = Factory.createActor("switch");
        Channel in = Factory.createChannelLinkActor(null, f1, 0, 0);
        Actor[][] loop1 = Factory.sumSequencer(j);
        Factory.createChannelLinkActor(f1, one, 0, 0);
        Factory.createChannelLinkActor(f1, inc, 1, 0);
        Factory.createChannelLinkActor(f1, dec, 1, 0);
        Factory.createChannelLinkActor(dec, merge, 0, 0);
        Factory.createChannelLinkActor(merge, f2, 0, 0);
        Factory.createChannelLinkActor(f2, gt, 0, 0);
        Factory.createChannelLinkActor(f2, sw, 0, 0);
        Factory.createChannelLinkActor(gt, f3, 0, 0);
        Factory.createChannelLinkActor(f3, sw, 0, 0);
        Channel mergein = Factory.createChannelLinkActor(f3, merge, 0, 0);
        mergein.set(0);
        Factory.createChannelLinkActor(sw, dec2, 0, 0);
        Factory.createChannelLinkActor(sw, null, 1, 0);
        Factory.createChannelLinkActor(dec2, merge, 0, 1);
        Factory.createChannelLinkActor(one, loop1[0][0], 0, 2);
        Factory.createChannelLinkActor(inc, loop1[0][1], 0, 2);
        Actor [][] psum = Factory.partialSum();
        Actor f4 = Factory.createActor("fork");
        Factory.createChannelLinkActor(psum[1][0], f4, 0, 1);
        Factory.createChannelLinkActor(loop1[1][0], psum[0][0], 1, 0);
        Factory.createChannelLinkActor(f3, psum[0][1], 0, 0);
        Actor sw2 = Factory.createActor("switch");
        Factory.createChannelLinkActor(f3, sw2, 0, 0);
        Factory.createChannelLinkActor(f4, sw2, 0, 1);
        Actor zero = Factory.createActor("constant", 0);
        Actor merge2 = Factory.createActor("merge");
        Factory.createChannelLinkActor(sw2, null, 1, 0);
        Channel min2 = Factory.createChannelLinkActor(f3, merge2, 0, 0);
        min2.set(0);
        Factory.createChannelLinkActor(f1, zero, 0, 0);
        Factory.createChannelLinkActor(zero, merge2, 0, 1);
        Factory.createChannelLinkActor(sw2, merge2, 0, 1);
        Actor add = Factory.createActor("add");
        Factory.createChannelLinkActor(merge2, add, 0, 1);
        Factory.createChannelLinkActor(f4, add, 0, 1);
        Factory.createChannelLinkActor(add, output, 0, 0);

        for (int i = 0; i < m; i++) {
            in.set(j);
        }

        long start = System.currentTimeMillis();
        Simulation.start();
        System.out.println("Time: "+ (System.currentTimeMillis() - start));
    }


    public static void main (String [] args) {
        Simulation.main(new String[]{"4"});
        // loopSim();
        // partialSum(10000);
         loopSim(10, 4000);
    }
}
