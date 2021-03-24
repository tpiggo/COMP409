package assignment3;
/**
 * NOTE: Always clean the directory out from the stuff you added 
 * Always run rmdir -f assignment3 before committing
 */
import java.io.FileWriter;
import java.nio.channels.AcceptPendingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat.Encoding;


class Transition {
    private Set<Character> characters;
    private State next;

    public Transition(State aNext, Character ...pCharacters) {
        this.next = aNext;
        this.characters = new HashSet<>();
        this.characters.addAll(Arrays.asList(pCharacters));
    }

    public State getNext() {return this.next; }

    public void setNext(State aNext) { this.next = aNext; }
    
    public State performTransition(char aChar) {
        if (!this.characters.contains(aChar)) {
            return null;
        }
        return this.next;
    }

    
    public static Transition fullCTransition() {
        return new Transition(null, '0','1', '2', '3', '4', '5', '6', '7', '8', '9');
    }

    public static Transition fullCTransition(State aNext) {
        return new Transition(aNext, '0','1', '2', '3', '4', '5', '6', '7', '8', '9');
    }

    public static Transition dTransition(State aNext) {
        return new Transition(aNext, '.');
    }
}

class State {
    int id;
    Set<Transition> transitions;
    public State(int id, Transition ...transitions) {
        this.id = id;
        this.transitions = new HashSet<>();
        // All loops have null as the next. Thus set it for each transition
        for (Transition t: transitions) {
            if (t.getNext() == null)
                t.setNext(this);
        }
        this.transitions.addAll(Arrays.asList(transitions));
    }

    /**
     * Greedy choice of the transition
     * @param aChar
     * @return
     */
    public State readChar(char aChar) {
        State next = null;
        for (Transition aTransition: transitions) {
            next  = aTransition.performTransition(aChar);
            if (next != null)
                break;
        }

        return next;
    }
}

class DFAGraph {
    State startState;
    ArrayList<State> states;
    State acceptState;
    public DFAGraph() {
        /**
         * TODO: Make this less ugly. This set up could be improved?
         */
        this.states = new ArrayList<>();
        // Hardcoded state creation since every state is different than the next
        State acceptNode = new State(5, Transition.fullCTransition());
        this.states.add(acceptNode);
        this.acceptState = acceptNode;
        // add all the other nodes.
        // Adding the states and Transitions statically.
        State n4 = new State(4, Transition.fullCTransition(acceptNode));
        this.states.add(n4);
        State n3 = new State(3, Transition.dTransition(n4));
        this.states.add(n3);
        State n2 = new State(2, Transition.dTransition(n4), Transition.fullCTransition());
        this.states.add(n2);
        State start = new State(1, 
            new Transition(n3, '0'), 
            new Transition(n2, '1', '2', '3', '4', '5', '6', '7', '8', '9')
            );
        this.startState = start;
        this.states.add(start);
    }

    public State getState(int id) {
        switch(id){
            case 1: return this.startState;
            case 5: return this.acceptState;
        }

        for (State aState: this.states) {
            if (aState.id == id)
                return aState;
        }

        return null;
    }
}

/**
 * Object for storing the simulation of the DFA
 */
class EncodingType{
    State start;
    State end;
    StringBuilder aString;
    int pos = 0;
    public EncodingType(State pStart, State pEnd, String pString) {
        this.start = pStart;
        this.end = pEnd;
        this.aString = new StringBuilder(pString);
    }
}

class DFAReader{
    private ExecutorService executor;
    private DFAGraph dfa;
    int id;
    int taskLength;
    
    DFAReader(DFAGraph pDfa, int n){
        this.executor = Executors.newFixedThreadPool(n);
        this.dfa = pDfa;
        this.id = 1;
    }
    /**
     * get the list of the simulations
     * @return List of encoded types.
     */
    public Future<EncodingType[]> simulDFA(String aString) {
        return executor.submit(
            new DFATask(this.dfa, aString, ++this.id)
            );
    }

    /**
     * get the list of the simulations
     * @return List of encoded types.
     */
    public Future<EncodingType[]> simulDFA(DFATask aTask) { 
        return executor.submit(aTask);
    }

    public void shutdown() {
        executor.shutdown();
    }

}

class DFATask implements Callable<EncodingType[]> {
    Queue<Character> charQueue;
    DFAGraph dfa;
    String aString;
    int id;
    int maxStartState = 5;
    EncodingType[] aEncoding =  new EncodingType[5];
    
    public DFATask(DFAGraph pDFA, String inputString, int pId) {
        charQueue = new LinkedList<>();
        dfa = pDFA;
        aString = inputString;
        id = pId;
        // Create empty encodings
        for (int i = 0; i < aEncoding.length; i++){
            aEncoding[i] = new EncodingType(dfa.getState(i+1), dfa.getState(i+1), "");
        }
    }

    private void pushEndState(State currentState, EncodingType eType) {
        if (currentState != null && currentState.equals(dfa.getState(5))) {
            while (!charQueue.isEmpty()) {
                eType.aString.append(charQueue.poll());
            }
        } else if (currentState != null && currentState != dfa.getState(5)) {
            while (!charQueue.isEmpty()) {
                charQueue.poll();
                eType.aString.append("_");
            }
        }
    }

    private EncodingType simulateDFA(int startPos, State startState) {
        State prevState = null;
        State currentState = startState;
        StringBuilder fBuilder = new StringBuilder();
        // Main body. Read the string char by char and perform the necessary computations
        for (int i = startPos; i < aString.length(); i++) {
            prevState = currentState;
            currentState = currentState.readChar(aString.charAt(i));
            if (currentState == null && prevState.equals(dfa.getState(5))) {
                while (!charQueue.isEmpty()) {
                    fBuilder.append(charQueue.poll());
                }
                fBuilder.append("_");
                currentState = dfa.getState(1);
            } else if (currentState == null && !prevState.equals(dfa.getState(5))) {
                while (!charQueue.isEmpty()) {
                    charQueue.poll();
                    fBuilder.append("_");
                }
                if (aString.charAt(i) > 48 && aString.charAt(i) < 57)
                    i--;                   
                else 
                    fBuilder.append("_");
                currentState = dfa.getState(1);
            } else {
                charQueue.add(aString.charAt(i));
            }
        }
        EncodingType aEncoding = new EncodingType(startState, currentState, fBuilder.toString());
        pushEndState(currentState, aEncoding);
        
        return aEncoding;
    }

    private boolean readToNextBreak(EncodingType pEncoding, int id){
        State prevState = null;
        State currentState = pEncoding.end;
        StringBuilder fBuilder = new StringBuilder();
        int pos = pEncoding.pos;
        for (; pos < aString.length(); pos++) {
            prevState = currentState;
            currentState = currentState.readChar(aString.charAt(pos));
            if (currentState == null) {
                if (prevState.equals(dfa.getState(5))) {
                    while (!charQueue.isEmpty()) {
                        fBuilder.append(charQueue.poll());
                    }
                    fBuilder.append("_");
                } else {
                    while (!charQueue.isEmpty()) {
                        charQueue.poll();
                        fBuilder.append("_");
                    }
    
                    if (aString.charAt(pos) > 48 && aString.charAt(pos) < 57)
                        pos--;
                    else 
                    fBuilder.append("_");
                }
                if (pos < 0) {
                    pos = 0;
                }

                pos++;
                pEncoding.end = dfa.getState(1);
                break;
            } else {
                charQueue.add(aString.charAt(pos));
            }
        }
        
        for (int i = 0; i < aEncoding.length; i++) {
            if (i != id && pEncoding.pos != 0 && aEncoding[i].pos == pEncoding.pos && aEncoding[i].end == pEncoding.end ) {
                // This element was at the same place!
                aEncoding[i].aString.append(fBuilder.toString());
                aEncoding[i].pos = pos;
            }
        }

        pEncoding.aString.append(fBuilder.toString());
        pEncoding.pos = pos;
        // Reached the end of the string :0
        if (pos >= aString.length()){
            if (charQueue.isEmpty())
                pushEndState(currentState, pEncoding);
            return true;
        }

        return false;
    }


    public EncodingType[] call() throws Exception {
        // Read only small bits until ALL machines converge, and read it once.
        int j = 0;
        int d = 0;
        while (d < 5) {
            if (readToNextBreak(aEncoding[j], j)) {
                d++;
            }
            if (aEncoding[j].aString.length() >= 49){
                System.out.println("here " + aEncoding[j].aString.length());
            }
            j = (j+1)%5;
        }

        EncodingType finalEncoding = simulateDFA(aEncoding[0].pos, aEncoding[0].end);

        for (int i = 0; i < aEncoding.length; i++) {
            aEncoding[i].aString.append(finalEncoding.aString.toString());
            aEncoding[i].end = finalEncoding.end;
            aEncoding[i].pos = finalEncoding.pos;
        }

        return aEncoding;
    }
}

class NormalRunnable implements Runnable {
    private String aString;
    private State endState = null;
    private DFAReader aReader = null;
    private StringBuilder finalString;
    private ArrayList<Future<EncodingType[]>> aFutures;
    private DFATask[] subtasks;
    private DFAGraph aDFA;
    private Queue<Character> charQueue;


    public NormalRunnable(int oThreads, String pString, DFAGraph dfa) {
        int partition = pString.length()/(oThreads+1);
        
        if (pString.length()%(oThreads+1) != 0) {
            partition++;
        }
        
        aString = pString.substring(0, partition);
        
        charQueue = new LinkedList<>();
        aFutures = new ArrayList<>();
        subtasks = new DFATask[oThreads];
        
        for (int i = 1; i < oThreads+1; i++) {
            int strLen = partition*(i+1)>pString.length()?partition*(i+1):pString.length();
            subtasks[i-1] = new DFATask(dfa, pString.substring((i)*partition, strLen), i);
        }

        if (oThreads != 0)
            aReader = new DFAReader(dfa, oThreads);
        aDFA = dfa;
        finalString = new StringBuilder();
    }

    @Override
    public void run() {
        // Start all the sub tasks
        for (int i = 0; i < subtasks.length; i++) {
            aFutures.add(aReader.simulDFA(subtasks[i]));
        }

        State prevState = null;
        State currentState = aDFA.getState(1);
        // Main body. Read the string char by char and perform the necessary computations
        for (int i = 0; i < aString.length(); i++) {
            prevState = currentState;
            currentState = currentState.readChar(aString.charAt(i));
            if (currentState == null && prevState.equals(aDFA.getState(5))) {
                while (!charQueue.isEmpty()) {
                    finalString.append(charQueue.poll());
                }
                finalString.append("_");
                currentState = aDFA.getState(1);
            } else if (currentState == null && !prevState.equals(aDFA.getState(5))) {
                while (!charQueue.isEmpty()) {
                    charQueue.poll();
                    finalString.append("_");
                }

                if (aString.charAt(i) > 48 && aString.charAt(i) < 57)
                    i--;
                else 
                    finalString.append("_");
                currentState = aDFA.getState(1);
            } else {
                charQueue.add(aString.charAt(i));
            }
        }
        
        if (currentState != null && currentState.equals(aDFA.getState(5))) {
            while (!charQueue.isEmpty()) {
                finalString.append(charQueue.poll());
            }
        } else if (currentState != null && currentState != aDFA.getState(5)) {
            while (!charQueue.isEmpty()) {
                charQueue.poll();
                finalString.append("_");
            }
        }
        // Set the end state
        endState = prevState;
        // join them all!
        for (Future<EncodingType[]> future: aFutures) {
            try {  
                EncodingType[] eArr = future.get();
                for (int i = 0; i < eArr.length; i++) {
                    if (eArr[i].start.equals(endState)) {
                        finalString.append(eArr[i].aString.toString());
                        endState = eArr[i].end;
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public String getString() {
        if (aReader != null)
            aReader.shutdown();
        return finalString.toString();
    }
}

public class q1 {
    /**
     * String generator per assignment requirements.
     * @param n length of the string
     * @return Randomly generated string
     */
    public static String generateString(int n) {
        char possible[] = new char[]{'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '.', 'a'};
        StringBuilder aStringBuilder = new StringBuilder();
        Random aRandom = new Random();
        for (int i = 0; i < n; i++){
            aStringBuilder.append(possible[aRandom.nextInt(possible.length)]);
        }
        return aStringBuilder.toString();
    }
    public static void main(String [] args) {
        DFAGraph mDFA = new DFAGraph();
        String aString = q1.generateString(100);
        try {

            FileWriter fileWriter = new FileWriter("output.txt");
            // System.out.println("Running DFA on " + aString);
            fileWriter.write("Running DFA on: " + aString + "\n");
            System.out.println("Starting executor");
            NormalRunnable nRun = new NormalRunnable(1, aString, mDFA);
            Thread t = new Thread(nRun);
            // Start the task
            long start = System.currentTimeMillis();
            t.start();
            t.join();
            long totalTime = System.currentTimeMillis()-start;
            aString = nRun.getString();
            // System.out.println("OUTPUT: " + aString);
            fileWriter.write("OUTPUT: " + aString);
            System.out.println("Time taken: " + totalTime);
            System.out.println("Length of the output string: " + aString.length());
            fileWriter.close();
        } catch (Exception e) {
            e.printStackTrace(); 
            System.out.println("ERROR: you broke.");
        }
    }
}
