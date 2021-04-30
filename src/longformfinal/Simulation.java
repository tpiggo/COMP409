package longformfinal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Get the javadoc stuff later
interface Actor extends Runnable {
    void connectIn(Channel c, int i);
    void connectOut(Channel c, int i);
    void act();
    boolean canAct();
}

interface Channel {
    void set(int i);
    void addToken(int i, ExecutorService service);
    int removeToken();
    void setDest(Actor pActor);
    boolean isEmpty();
    int peek();
}

/**
 * Deterministic Channels!
 */
class ConcreteChannel implements Channel {
    private Queue<Integer> tokenQueue = new LinkedList<>();
    private Actor aDest;

    @Override
    public synchronized void set(int i) {
        tokenQueue.add(i);
    }

    private synchronized void add(int i) {
        tokenQueue.add(i);
    }
    @Override
    public void addToken(int i, ExecutorService service) {
        add(i);
        if (aDest != null && aDest.canAct()) {
            service.submit(aDest);
        }
    }

    @Override
    public synchronized int removeToken() {
        return tokenQueue.remove();
    }

    @Override
    public void setDest(Actor pActor) {
        aDest = pActor;
    }

    @Override
    public synchronized boolean isEmpty() {
        return tokenQueue.isEmpty();
    }

    @Override
    public synchronized int peek() {
        if (tokenQueue.isEmpty()) {
            return -1;
        }
        return tokenQueue.peek();
    }

}

abstract class AbstractActor implements Actor {
    protected ArrayList<Channel> channelsIn;
    protected ArrayList<Channel> channelsOut;
    protected boolean acting = false, queued = false;
    protected static  ExecutorService aServ;
    private int aMaxIn = 10;
    private int aMaxOut = 10;
    int consumed = 0;

    public static void setExecutor(ExecutorService s) {
        aServ = s;
    };

    AbstractActor() {
        channelsIn = new ArrayList<>();
        channelsOut = new ArrayList<>();
    }

    AbstractActor(int maxIn) {
        this();
        aMaxIn = maxIn;
    }

    AbstractActor(int maxIn, int maxOut) {
        this();
        aMaxIn = maxIn;
        aMaxOut = maxOut;
    }


    @Override
    public void connectIn(Channel c, int i) {
        if (i >= channelsIn.size())
            channelsIn.add(c);
        else
            channelsIn.add(i,c);
        c.setDest(this);
    }

    @Override
    public void connectOut(Channel c, int i) {
        if (i >= channelsOut.size())
            channelsOut.add(c);
        else
            channelsOut.add(i,c);
    }

    @Override
    public synchronized boolean canAct() {
        for (Channel channel : channelsIn) {
            if (channel.isEmpty()) {
                return false;
            }
        }

        if (!acting && !queued) {
            // If this operation is successful, we will queue the actor.
            queued = true;
            return true;
        }

        return false;
    }

    @Override
    public void run() {
        act();
    }

    protected synchronized void tryActing() {
        for (Channel channel : channelsIn) {
            if (channel.isEmpty()) {
                // fails here
                acting = false;
                break;
            }
        }
    }
    
    protected synchronized void startActing() {
        acting = true;
        queued = false;
    }
}

class AddActor extends AbstractActor {
    AddActor() {
        super(2,1);
    }


    @Override
    public void act() {
        startActing();

        while (acting) {
            int in1 = channelsIn.get(0).removeToken();
            int in2 = channelsIn.get(1).removeToken();
            channelsOut.get(0).addToken(in2 + in1, aServ);
            tryActing();
        }
    }
}

class ForkActor extends AbstractActor {
    static int num = 1;
    int id;
    ForkActor() {
        super(1);
        id = num++;
    }

    @Override
    public void act() {
        startActing();

        while (acting) {
//            System.out.println("Fork " + id + ": consuming : " + ++consumed);
            int in1 = channelsIn.get(0).removeToken();
            for (Channel channel: channelsOut) {
                channel.addToken(in1, aServ);
            }
            tryActing();
        }
    }
}

class IncrementActor extends AbstractActor {

    IncrementActor() {
        super(1,1);
    }

    @Override
    public void act() {
        startActing();

        while (acting) {
            int in1 = channelsIn.get(0).removeToken();
            channelsOut.get(0).addToken(++in1, aServ);
            tryActing();
        }
    }
}

class DecrementActor extends AbstractActor {

    DecrementActor() {
        super(1,1);
    }

    @Override
    public void act() {
        startActing();

        while (acting) {
            int in1 = channelsIn.get(0).removeToken();
            channelsOut.get(0).addToken(--in1, aServ);
            tryActing();
        }
    }
}

class EqualsActor extends AbstractActor {
    Integer value;

    EqualsActor() {
        super(2,1);
        value = null;
    }

    /**
     * Hardcoded less than actor.
     * @param setVal
     */
    EqualsActor(int setVal) {
        super(1,1);
        value = setVal;
    }

    @Override
    public void act() {
        startActing();

        while (acting) {
            int ret = 0;
            int in1 = channelsIn.get(0).removeToken();
            if (value != null && in1 == value) {
                ret = 1;
            } else if (value == null) {
                int in2 = channelsIn.get(1).removeToken();
                if (in1 == in2) {
                    ret = 1;
                }
            }
            channelsOut.get(0).addToken(ret, aServ);

            tryActing();
        }
    }

    @Override
    public synchronized boolean canAct() {
        if (!channelsIn.get(0).isEmpty() && !acting && !queued) {
            queued = true;
            return true;
        }

        return false;
    }

    @Override
    protected synchronized void tryActing() {
        if (channelsIn.get(0).isEmpty()) {
            acting = false;
        }
    }
}

class LessThanActor extends AbstractActor {
    Integer value;

    LessThanActor() {
        super(2,1);
        value = null;
    }

    /**
     * Hardcoded less than actor.
     * @param setVal
     */
    LessThanActor(int setVal) {
        super(1,1);
        value = setVal;
    }

    @Override
    public void act() {
        startActing();

        while (acting) {

            int ret = 0;
            int in1 = channelsIn.get(0).removeToken();
//            System.out.println("LessThan: " + ++consumed);
            if (value != null && in1 < value) {
                ret = 1;
            } else if (value == null)  {
                int in2 = channelsIn.get(1).removeToken();
                if (in1 < in2) {
                    ret = 1;
                }
            }

            channelsOut.get(0).addToken(ret, aServ);
            tryActing();
        }

    }
}

class GreaterThanActor extends AbstractActor {
    Integer value;

    GreaterThanActor() {
        super(2,1);
        value = null;
    }

    /**
     * Hardcoded less than actor.
     * @param setVal
     */
    GreaterThanActor(int setVal) {
        super(1,1);
        value = setVal;
    }

    @Override
    public void act() {
        startActing();

        while (acting) {

            int ret = 0;
            int in1 = channelsIn.get(0).removeToken();
            if (value != null && in1 > value) {
                ret = 1;
            } else if (value == null)  {
                int in2 = channelsIn.get(1).removeToken();
                if (in1 > in2) {
                    ret = 1;
                }
            }

            channelsOut.get(0).addToken(ret, aServ);
            tryActing();
        }

    }
}

class CDRActor extends AbstractActor {
    private boolean hasConsumed = false;
    CDRActor () {
        super(1,1);
    }

    @Override
    public void act() {
        startActing();

        while (acting) {
            int in1 = channelsIn.get(0).removeToken();
            // throw away the first token
            if (!hasConsumed) {
                hasConsumed = true;
            } else {
                channelsOut.get(0).addToken(in1, aServ);
            }

            tryActing();
        }
    }

    @Override
    public synchronized boolean canAct() {
        if (!channelsIn.get(0).isEmpty() && !acting && !queued) {
            queued = true;
            return true;
        }
        return false;
    }

    @Override
    protected synchronized void tryActing() {
        if (channelsIn.get(0).isEmpty()) {
            acting = false;
        }
    }
}

class InputActor extends AbstractActor {
    private Queue<Integer> aQueue;

    InputActor () {
        super(0, 1);
        aQueue = new LinkedList<>();
    }

    InputActor(String fileName) {
        this();
        File file = new File(fileName);

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));

            String string;
            while ((string = br.readLine()) != null) {
                aQueue.add(Integer.parseInt(string));
            }
            System.out.println("Done reading file.");

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

    }

    @Override
    public void act() {
        startActing();

        while (!aQueue.isEmpty()) {
            synchronized (this) {
                channelsOut.get(0).addToken(aQueue.remove(), aServ);
            }

            System.out.println("INPUT PUSHED SOMETHING");
        }
    }
}

class OutputActor extends AbstractActor {
    int maxReads = 1, consumed;
    boolean terminal = false;
    boolean terminated = false;

    OutputActor () {
        super(1, 0);
    }

    OutputActor(int pMaxReads) {
        super(1,0);
        maxReads = pMaxReads;
        terminal = true;
    }

    @Override
    public void act() {
        startActing();

        while(acting) {
            consumed++;
            System.out.println(channelsIn.get(0).removeToken());
            tryActing();
            if (terminal) {
                if (consumed >= maxReads){
                    terminated = true;
                    synchronized (this) {
                        notifyAll();
                    }
                }
            }
        }
    }

    @Override
    public synchronized boolean canAct() {
        if (!channelsIn.get(0).isEmpty() && !acting && !queued) {
            queued = true;
            return true;
        }

        return false;
    }

    @Override
    public synchronized void tryActing() {
        if (channelsIn.get(0).isEmpty()) {
            acting = false;
        }
    }

    public synchronized boolean isTerminated() {
        if (!terminal) {
            throw new IllegalCallerException("Cannot call this method on this object!");
        }

        while (!terminated) {
            try {
                wait(3000);
                int x = 0;
            } catch (Exception e) { e.printStackTrace();}
        }

        return true;
    }
}

class SwitchActor extends AbstractActor {
    SwitchActor () {
        super(2,2);
    }

    @Override
    public void act() {
        startActing();

        while(acting) {

            int bool = channelsIn.get(0).removeToken();
            int input = channelsIn.get(1).removeToken();
            if (bool == 1) {
                channelsOut.get(0).addToken(input, aServ);
            } else  {
                channelsOut.get(1).addToken(input, aServ);
            }
            tryActing();
        }
    }
}

class MergeActor extends AbstractActor {
    static int num = 1;
    int id ;
    MergeActor () {
        super(3, 1); id = num++;
    }

    @Override
    public void act() {
        startActing();

        while(acting) {
            // System.out.println("Merge "+ id + "; " + ++consumed);
            int bool = channelsIn.get(0).removeToken();
            int input;
            if (bool == 0) {
                input = channelsIn.get(2).removeToken();
            } else  {
                input  = channelsIn.get(1).removeToken();
            }
            channelsOut.get(0).addToken(input, aServ);
            tryActing();
        }
    }

    @Override
    protected synchronized void tryActing(){
        int bool = channelsIn.get(0).peek();

        if (bool == -1) {
            acting = false;
        }

        if (bool == 0 && channelsIn.get(2).isEmpty()) {
            acting = false;
        } else if(bool == 1 && channelsIn.get(1).isEmpty())  {
            acting = false;
        }
    }

    @Override
    public synchronized boolean canAct() {
        int bool = channelsIn.get(0).peek();
        if (bool == -1 || acting || queued) {
            return false;
        }

        if (bool == 0 && !channelsIn.get(2).isEmpty()) {
            queued = true;
            return true;
        } else if(bool == 1 && !channelsIn.get(1).isEmpty())  {
            queued = true;
            return true;
        }

        return false;
    }
}

class ConstantActor extends AbstractActor {
    Integer value;
    ConstantActor(int pValue) {
        value = pValue;
    }

    @Override
    public void act() {
        startActing();

        while(acting) {
//            System.out.println("Constant: " + ++consumed);
            channelsIn.get(0).removeToken();
            channelsOut.get(0).addToken(value, aServ);
            tryActing();
        }
    }
}

class Factory {
    private static List<AbstractActor> actorsCreated = new LinkedList<>();
    public static List<AbstractActor> getActors() {
        return actorsCreated;
    }

    public static Actor createActor(String name) {
        AbstractActor actor;
        switch(name){
            case "add":
                actor = new AddActor();
                actorsCreated.add(actor);
                return actor;
            case "fork":
                actor = new ForkActor();
                actorsCreated.add(actor);
                return actor;
            case "merge":
                actor = new MergeActor();
                actorsCreated.add(actor);
                return actor;
            case "switch":
                actor = new SwitchActor();
                actorsCreated.add(actor);
                return actor;
            case "inc":
                actor = new IncrementActor();
                actorsCreated.add(actor);
                return actor;
            case "dec":
                actor = new DecrementActor();
                actorsCreated.add(actor);
                return actor;
            case "==":
                actor = new EqualsActor();
                actorsCreated.add(actor);
                return actor;
            case "<":
                actor = new LessThanActor();
                actorsCreated.add(actor);
                return actor;
            case ">":
                actor = new GreaterThanActor();
                actorsCreated.add(actor);
                return actor;
            case "cdr":
                actor = new CDRActor();
                actorsCreated.add(actor);
                return actor;
            case "input":
                actor = new InputActor();
                actorsCreated.add(actor);
                return actor;
            case "output":
                actor = new OutputActor();
                actorsCreated.add(actor);
                return actor;
            default:
                return null;
        }
    }

    public static Actor createActor(String name, int i) {
        AbstractActor actor;
        switch(name){
            case "==":
                actor = new EqualsActor(i);
                actorsCreated.add(actor);
                return actor;
            case "<":
                actor = new LessThanActor(i);
                actorsCreated.add(actor);
                return actor;
            case ">":
                actor = new GreaterThanActor(i);
                actorsCreated.add(actor);
                return actor;
            case "output":
                actor = new OutputActor(i);
                actorsCreated.add(actor);
                return actor;
            case "constant":
                actor = new ConstantActor(i);
                actorsCreated.add(actor);
                return actor;
            default:
                return createActor(name);
        }
    }

    public static Actor createActor(String name, String fileName) {
        if (name.equals("input")) {
            AbstractActor actor = new InputActor(fileName);
            actorsCreated.add(actor);
            return actor;
        } else {
            return createActor(name);
        }
    }

    public static Channel createChannel() {
        return new ConcreteChannel();
    }

    public static Channel createChannelLinkActor(Actor out, Actor in, int posOut, int posIn) {
        ConcreteChannel c = new ConcreteChannel();
        if (in != null) {
            in.connectIn(c, posIn);
        }
        if (out != null) {
            out.connectOut(c, posOut);
        }
        return c;
    }

    // Row 1 is the actors which require input lines
    // Row 2 is the actors which require output lines connected.
    public static Actor[][] partialSum() {
        Actor add = Factory.createActor("add");
        Actor f1 = Factory.createActor("fork");
        Actor f2 = Factory.createActor("fork");
        Actor sw = Factory.createActor("switch");
        Actor merge = Factory.createActor("merge");
        Actor zero = Factory.createActor("constant", 0);
        Factory.createChannelLinkActor(add, f1, 0, 0);
        Factory.createChannelLinkActor(f2, merge, 0, 0);
        Factory.createChannelLinkActor(f2, sw, 0, 0);
        Factory.createChannelLinkActor(f1, sw, 0, 1);
        Factory.createChannelLinkActor(sw, merge, 0,1);
        Factory.createChannelLinkActor(sw, zero, 1,0);
        Factory.createChannelLinkActor(zero, merge, 0, 2);
        Channel c = Factory.createChannelLinkActor(merge, add, 0, 0);
        c.set(0);
        return new Actor[][] {{add,f2},{f1}};
    }

    public static Actor[][] sumSequencer(int i) {
        Actor inc = Factory.createActor("inc");
        Actor lt = Factory.createActor("<");
        Actor f2 = Factory.createActor("fork");
        Actor f3 = Factory.createActor("fork");
        Actor f4 = Factory.createActor("fork");
        Actor f5 = Factory.createActor("fork");
        Actor merge1 = Factory.createActor("merge");
        Actor sw1 = Factory.createActor("switch");
        Actor merge2 = Factory.createActor("merge");
        Actor sw2 = Factory.createActor("switch");
        // out of switch inc and then merge
        Factory.createChannelLinkActor(sw1, f2, 0, 0);
        Factory.createChannelLinkActor(f2, inc, 0, 0);
        Factory.createChannelLinkActor(inc, merge1, 0, 0);
        Factory.createChannelLinkActor(sw2, merge2, 0, 0);
        // false channel out of switch
        Factory.createChannelLinkActor(sw1, null, 1, 1);
        Factory.createChannelLinkActor(sw2, null, 1, 1);
        // connect the two forks after the merge
        Factory.createChannelLinkActor(merge1, f3, 0, 0);
        Factory.createChannelLinkActor(merge2, f4, 0, 0);
        Factory.createChannelLinkActor(f3, lt, 0, 0);
        Factory.createChannelLinkActor(f4, lt, 0, 1);
        Factory.createChannelLinkActor(f3, sw1, 1, 1);
        Factory.createChannelLinkActor(f4, sw2, 1, 1);
        Factory.createChannelLinkActor(lt, f5, 0, 0);
        Channel merge1in = Factory.createChannelLinkActor(f5, merge1, 0, 0);
        Factory.createChannelLinkActor(f5, sw1, 0, 0);
        Channel merge2in = Factory.createChannelLinkActor(f5, merge2, 0, 0);
        Factory.createChannelLinkActor(f5, sw2, 0, 0);
        merge2in.set(0);
        merge1in.set(0);

        return new Actor[][] {{merge1, merge2},{f2, f5, sw1, sw2}};
    }
}

public class Simulation {
    private static int n;
    private static Actor terminalActor;

    public static void start() {
        ExecutorService pService = Executors.newFixedThreadPool(n);
        AbstractActor.setExecutor(pService);
        List<Actor> readyActors = new ArrayList<>();
        // Get the list of actors
        for (AbstractActor actor: Factory.getActors()) {
            if (actor.canAct()){
                readyActors.add(actor);
            }
        }
        for (AbstractActor actor: Factory.getActors()) {
            if (actor instanceof OutputActor && ((OutputActor) actor).terminal) {
                terminalActor = actor;
                break;
            }
        }

        if (readyActors.isEmpty()) {
            throw new Error("Failed to find any node which has enough input to run.");
        }
        // Submit the root.
        for (Actor actor: readyActors) {
            pService.submit(actor);
        }

        // If it cannot find a
        if (terminalActor != null) {
            if (terminalActor instanceof OutputActor && ((OutputActor) terminalActor).isTerminated()) {
                pService.shutdown();
            }
        }
    }

    public static void main(String [] args) {
        if (args.length >= 1) {
            n = Integer.parseInt(args[0]);
        } else {
            throw new IllegalArgumentException("Missing input!");
        }
        // ADD YOUR CODE HERE!
    }
}
