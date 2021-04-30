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
    private int maxLength = 1000;

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
//                System.out.println("LessThan: " +in1 + " < " +  in2);
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
//            System.out.println("GEQ 0 consuming : " + ++consumed);
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

    public synchronized void pushQueue(int i) {
        aQueue.add(i);
    }
    public void input(int i) {
        pushQueue(i);
        if (aServ != null && !aQueue.isEmpty()) {
            aServ.submit(this);
        }
    }

    public int sequenceLength() {
        int length = 0;
        for (Integer i : aQueue)
            length += i;
        return length;
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
//             System.out.println("Merge "+ id + "; " + ++consumed);
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

    // need 8 forks
    public static void squareSequence(String fileName) {
        InputActor inputActor = (InputActor) Factory.createActor("input", fileName);
        Actor forkTop =  Factory.createActor("fork");
        Actor constOne =  Factory.createActor("constant", 1);
        Actor constZero =  Factory.createActor("constant", 0);
        Actor incrementMid = Factory.createActor("inc");
        Actor decrementRight = Factory.createActor("dec");
        Actor mergeLeft = Factory.createActor("merge");
        Actor mergeMiddle = Factory.createActor("merge");
        Actor mergeRight = Factory.createActor("merge");
        Actor forkLeftLoop =  Factory.createActor("fork");
        Actor forkMidLoop =  Factory.createActor("fork");
        Actor forkRightLoop =  Factory.createActor("fork");
        Actor lessThan = Factory.createActor("<");
        Actor greaterThan0 = Factory.createActor(">", 0);
        Actor forkBMidLoop =  Factory.createActor("fork");
        Actor forkBRightLoop =  Factory.createActor("fork");
        Actor switchLeft = Factory.createActor("switch");
        Actor switchMiddle = Factory.createActor("switch");
        Actor switchRight = Factory.createActor("switch");
        Actor forkBLeftLoop =  Factory.createActor("fork");
        Actor incrementLoop = Factory.createActor("inc");
        Actor decrementLoop = Factory.createActor("dec");
        Actor pSumAdd = Factory.createActor("add");
        Actor pSumFork = Factory.createActor("fork");
        Actor pSumSwitch = Factory.createActor("switch");
        Actor pSumZero = Factory.createActor("constant", 0);
        Actor pSumMerge = Factory.createActor("merge");
        Actor finalSwitch = Factory.createActor("switch");
        Actor finalMerge = Factory.createActor("merge");
        Actor finalAdd = Factory.createActor("add");
        Actor output = Factory.createActor("output", inputActor.sequenceLength());
        Factory.createChannelLinkActor(inputActor, forkTop, 0, 0);
        Factory.createChannelLinkActor(forkTop, constOne, 0, 0);
        Factory.createChannelLinkActor(forkTop, incrementMid, 1, 0);
        Factory.createChannelLinkActor(forkTop, decrementRight, 2, 0);
        Factory.createChannelLinkActor(forkTop, constZero, 3, 0);
        Factory.createChannelLinkActor(constOne, mergeLeft, 0, 0);
        Factory.createChannelLinkActor(incrementMid, mergeMiddle, 0, 0);
        Factory.createChannelLinkActor(decrementRight, mergeRight, 0, 0);
        Factory.createChannelLinkActor(mergeLeft, forkLeftLoop, 0, 0);
        Factory.createChannelLinkActor(mergeMiddle, forkMidLoop, 0, 0);
        Factory.createChannelLinkActor(mergeRight, forkRightLoop, 0, 0);
        Factory.createChannelLinkActor(forkLeftLoop, switchLeft, 0, 0);
        Factory.createChannelLinkActor(forkMidLoop, switchMiddle, 0, 0);
        Factory.createChannelLinkActor(forkRightLoop, switchRight, 0, 0);
        Factory.createChannelLinkActor(forkLeftLoop, lessThan, 0, 0);
        Factory.createChannelLinkActor(forkMidLoop, lessThan, 0, 1);
        Factory.createChannelLinkActor(lessThan, forkBMidLoop, 0, 1);
        Factory.createChannelLinkActor(forkRightLoop, greaterThan0, 0, 0);
        Factory.createChannelLinkActor(greaterThan0, forkBRightLoop, 0, 0);
        Factory.createChannelLinkActor(forkBMidLoop, switchLeft, 0, 0);
        Factory.createChannelLinkActor(forkBMidLoop, switchMiddle, 0, 0);
        // Create the merge contacts and set the token to false
        Factory.createChannelLinkActor(forkBMidLoop, mergeMiddle, 0, 0).set(0);
        Factory.createChannelLinkActor(forkBMidLoop, mergeLeft, 0, 0).set(0);
        Factory.createChannelLinkActor(forkBRightLoop, mergeRight, 0, 0).set(0);
        Factory.createChannelLinkActor(forkBRightLoop, finalMerge, 0, 0).set(0);
        // connection to the switches and merges without needing start tokens
        Factory.createChannelLinkActor(forkBRightLoop, switchRight, 0, 0);
        Factory.createChannelLinkActor(forkBRightLoop, finalSwitch, 0, 0);
        Factory.createChannelLinkActor(forkBRightLoop, pSumSwitch, 0, 0);
        Factory.createChannelLinkActor(forkBRightLoop, pSumMerge, 0, 0);
        //connecting the loops
        Factory.createChannelLinkActor(switchMiddle, mergeMiddle, 0, 1);
        Factory.createChannelLinkActor(switchLeft, forkBLeftLoop, 0, 0);
        Factory.createChannelLinkActor(switchRight, decrementLoop, 0, 0);
        Factory.createChannelLinkActor(decrementLoop, mergeRight, 0, 1);
        // Need sink actors HERE
        Factory.createChannelLinkActor(switchMiddle, null, 1, 0);
        Factory.createChannelLinkActor(switchLeft, null, 1, 0);
        Factory.createChannelLinkActor(switchRight, null, 1, 0);
        // Connecting the left hand loop to the pSUm and loop
        Factory.createChannelLinkActor(forkBLeftLoop, incrementLoop, 0, 0);
        Factory.createChannelLinkActor(incrementLoop, mergeLeft, 0, 1);
        Factory.createChannelLinkActor(forkBLeftLoop, pSumAdd, 0, 0);
        // creating reusable pSum
        Factory.createChannelLinkActor(pSumMerge, pSumAdd, 0, 0).set(0);
        Factory.createChannelLinkActor(pSumAdd, pSumFork, 0, 0);
        Factory.createChannelLinkActor(pSumFork, pSumSwitch, 0, 1);
        Factory.createChannelLinkActor(pSumSwitch, pSumMerge, 0, 1);
        Factory.createChannelLinkActor(pSumSwitch, pSumZero, 1, 0);
        Factory.createChannelLinkActor(pSumZero, pSumMerge, 0, 2);
        // fork the output from pSum to the final part
        Factory.createChannelLinkActor(pSumFork, finalSwitch, 0, 1);
        Factory.createChannelLinkActor(finalSwitch, finalMerge, 0, 1);
        Factory.createChannelLinkActor(finalSwitch, null, 1, 0);
        Factory.createChannelLinkActor(constZero, finalMerge, 0, 2);
        Factory.createChannelLinkActor(finalMerge, finalAdd, 0, 0);
        Factory.createChannelLinkActor(pSumFork, finalAdd, 0, 1);
        Factory.createChannelLinkActor(finalAdd, output, 0, 0);

        long start = System.currentTimeMillis();
        Simulation.start();
        System.out.println("Time: "+ (System.currentTimeMillis() - start));
    }

    public static void main(String [] args) {
        if (args.length >= 1) {
            n = Integer.parseInt(args[0]);
        } else {
            throw new IllegalArgumentException("Missing input!");
        }
        // ADD YOUR CODE HERE!
        squareSequence("C:\\Users\\piggo\\IdeaProjects\\COMP409\\src\\longformfinal\\input.txt");

    }
}