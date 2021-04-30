package longformfinal;
import java.util.*;
import java.util.concurrent.*;

class TestChannel {
    private ExecutorService s;
    private Queue<Integer> values = new LinkedList<Integer>();
    private TestActor des;
    public void set(int i) {values.offer(i);}
    public synchronized void  setVal(int i) {
        values.add(i);
        if (des.canAct() && !des.isActing())
            s.execute(des);
    }
    public synchronized int getVal() {
        return values.remove();
    }

    public synchronized boolean hasValues() {
        return !values.isEmpty();
    }

    public void setDes(TestActor des1) {
        des = des1;
    }
    public void linkExecutor(ExecutorService serv) {
        s = serv;
    }
    public TestActor destination () { return des; }

}

interface TestActor extends Runnable{
    void act();
    boolean canAct();
    boolean isActing();
}

class TestIncrement implements TestActor {
    private int id;
    List<TestChannel> i, o;
    boolean acting = false;
    TestIncrement(int id, TestChannel in, TestChannel out){
        this.id = id;
        i = new ArrayList<TestChannel>();
        i.add(in);
        for (TestChannel c: i) {
            c.setDes(this);
        }
        o = new ArrayList<TestChannel>();
        o.add(out);
    }

    @Override
    public void run() {
        act();
    }

    @Override
    public void act() {
        synchronized (this) {
            acting = true;
        }
        while (canAct()) {
            int val = i.get(0).getVal();
            System.out.println("The value is " + val + " passing through T-" + id);
            o.get(0).setVal(val + 1);
        }
        synchronized (this) {
            acting = false;
        }
    }

    @Override
    public boolean canAct() {
        return i.get(0).hasValues();
    }

    @Override
    public synchronized boolean isActing() {
        return acting;
    }
}

class TestAdd implements TestActor {
    private int id;
    List<TestChannel> o;
    List<TestChannel> i;
    boolean acting = false;
    TestAdd(int id, TestChannel out, TestChannel... channelsIn){
        this.id = id;
        i = Arrays.asList(channelsIn.clone());
        for (TestChannel channel: i) {
            channel.setDes(this);
        }
        o = new ArrayList<TestChannel>();
        o.add(out);
    }
    @Override
    public void act() {
        synchronized (this) {
            acting = true;
        }
        while (canAct()) {
            int inp1 = i.get(0).getVal();
            int inp2 = i.get(1).getVal();
            o.get(0).setVal(inp1+inp2);
        }
        synchronized (this) {
            acting = false;
        }
    }

    @Override
    public boolean canAct() {
        for (TestChannel channel: i) {
            if (!channel.hasValues()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public synchronized boolean isActing() {
        return acting;
    }

    @Override
    public void run() {
        act();
    }
}

class TestFork implements TestActor {
    private int id;
    List<TestChannel> i;
    List<TestChannel> o;
    boolean acting = false;
    TestFork(int id, TestChannel in, TestChannel... out){
        this.id = id;
        i = new ArrayList<TestChannel>();
        i.add(in);
        i.get(0).setDes(this);
        o = Arrays.asList(out.clone());
    }
    @Override
    public void act() {
        synchronized (this) {
            acting = true;
        }
        while (canAct()) {
            int inp = i.get(0).getVal();
            for(TestChannel c: o) {
                c.setVal(inp);
            }
        }
        synchronized (this) {
            acting = false;
        }

    }

    @Override
    public boolean canAct() {
        return i.get(0).hasValues();
    }

    @Override
    public synchronized boolean isActing() {
        return acting;
    }

    @Override
    public void run() {
        act();
    }
}

class TestSink implements TestActor {
    private int id;
    TestChannel i, o;
    boolean acting  = false;
    TestSink(int id, TestChannel out, TestChannel channelIn){
        this.id = id;
        i = channelIn;
        i.setDes(this);
        o = out;
    }
    @Override
    public void act() {
        synchronized (this) {
            acting = true;
        }
        while (canAct()) {
            int inp1 = i.getVal();
            System.out.println("Sink reads: " + inp1);
        }
        synchronized (this) {
            acting = false;
        }
    }

    public synchronized boolean isActing(){
        return acting;
    }

    @Override
    public boolean canAct() {
        return i.hasValues();
    }

    @Override
    public void run() {
        act();
    }
}

public class ExecutorTest{

    private static void loopedIncrement(ExecutorService s) {
        TestChannel c1 = new TestChannel();
        TestChannel c2 = new TestChannel();
        TestChannel c3 = new TestChannel();
        c3.set(1);
        TestActor e1 = new TestIncrement(1, c3, c1);
        TestActor e2 = new TestIncrement(2, c1, c2);
        TestActor e3 = new TestIncrement(3, c2, c3);
        c1.linkExecutor(s);
        c2.linkExecutor(s);
        c3.linkExecutor(s);
        s.execute(e1);
    }

    private static void partialSumTest(ExecutorService s) throws ExecutionException, InterruptedException, TimeoutException {
        TestChannel c1 = new TestChannel();
        TestChannel c2 = new TestChannel();
        TestChannel c3 = new TestChannel();
        TestChannel c4 = new TestChannel();
        c1.set(1);
        c2.set(0);
        TestAdd add = new TestAdd(1, c3, c1,c2);
        TestSink sink = new TestSink(2, null, c4);
        TestFork f = new TestFork(3, c3, c2, c4);
        // Link the executor service into the channels
        c1.linkExecutor(s);
        c2.linkExecutor(s);
        c3.linkExecutor(s);
        c4.linkExecutor(s);

        s.execute(add);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Creates a stream of 1s
                while (true) {
                    c1.setVal(1);
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {}
                }
            }
        }).start();
    }

    public static void main(String [] args) throws InterruptedException, ExecutionException, TimeoutException {
        ExecutorService s = Executors.newFixedThreadPool(1);
        // loopedIncrement(s);
        partialSumTest(s);
        // CANT HAVE THIS! s.shutdown();
        System.out.println("DONE ALL");
    }
}
