package assignment3;
/**
 * NOTE: Always clean the directory out from the stuff you added 
 * Always run rmdir -f assignment3 before committing
 */
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * TODO: Rebuild the object for encoding an output, give it a better name for that ADT
 *       and allow for merging with other DFA start positions within the path.
 *
 */

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
    int maxPos;
    EncodingType[] aEncoding =  new EncodingType[5];
    int [] converge;
    
    public DFATask(DFAGraph pDFA, String inputString, int pId) {
        charQueue = new LinkedList<>();
        dfa = pDFA;
        aString = inputString;
        id = pId;
        maxPos = 1;
        // Create empty encodings
        for (int i = 0; i < aEncoding.length; i++){
            aEncoding[i] = new EncodingType(dfa.getState(i+1), dfa.getState(i+1), "");
        }
        converge = new int[]{1,2,3,4,5};
    }

    private EncodingType simulateDFA(int startPos, State startState) {
        State prevState;
        State currentState = startState;
        StringBuilder fBuilder = new StringBuilder();
        int j = 0;
        // Main body. Read the string char by char and perform the necessary computations
        int i = startPos;
        for (; i < aString.length(); i++) {
            prevState = currentState;
            currentState = currentState.readChar(aString.charAt(i));
            if (currentState == null && prevState.equals(dfa.getState(5))) {
                fBuilder.append("_");
                currentState = dfa.getState(1);
                j = i-startPos;
            } else if (currentState == null && !prevState.equals(dfa.getState(5))) {
                String a = "";
                for (int k = 0; k < ((i-startPos)-j); k++){
                    a = a+ "_";
                }
                fBuilder.replace(j, (i-startPos), a);
                if (aString.charAt(i) > 48 && aString.charAt(i) < 57)
                    i--;
                else
                    fBuilder.append("_");
                currentState = dfa.getState(1);
                j = i-startPos >= 0?i:0;
            } else {
                fBuilder.append(aString.charAt(i));
            }
        }

        if (currentState != null && currentState != dfa.getState(5)) {
            String a = "";
            for (int k = 0; k < ((i-startPos) - j); k++){
                a = a+ "_";
            }
            fBuilder.replace(j, fBuilder.length(), a);
        }
        EncodingType aEncoding = new EncodingType(startState, currentState, fBuilder.toString());

        return aEncoding;
    }

    private boolean readToNextBreak(EncodingType pEncoding, int pId){
        State prevState = null;
        State currentState = pEncoding.end;
        StringBuilder fBuilder = new StringBuilder();
        int pos = pEncoding.pos;
        for (; pos < aString.length(); pos++) {
            prevState = currentState;
            currentState = currentState.readChar(aString.charAt(pos));
            if (currentState == null) {
                if (prevState.equals(dfa.getState(5))) {
                    fBuilder.append("_");
                    currentState = dfa.getState(1);
                } else {
                    String a = "";
                    for (int j = 0; j < (pos-pEncoding.pos); j++){
                        a = a + "_";
                    }
                    fBuilder.replace(0, a.length(), a);

                    if (aString.charAt(pos) > 48 && aString.charAt(pos) < 57)
                        pos--;
                    else
                        fBuilder.append("_");
                    currentState = dfa.getState(1);
                }

                if (pos < 0) {
                    pos = 0;
                }
                pos++;
                break;
            } else {
                fBuilder.append(aString.charAt(pos));
            }
        }
        pEncoding.end = currentState;
        maxPos = pos;

        pEncoding.aString.append(fBuilder.toString());
        boolean returnable = false;
        if (pos >= 4 && (!currentState.equals(dfa.getState(5)) ||  !prevState.equals(dfa.getState(5)))){
            String a = "";
            for (int j = 0; j < (pos-pEncoding.pos); j++){
                a = a+ "_";
            }
            pEncoding.aString.replace(pEncoding.pos, pos, a);
            returnable = true;
        } else if (pos >= 4) {
            returnable = true;
        }

        pEncoding.pos = pos;
        return returnable;
    }


    public EncodingType[] call() throws Exception {
        // Read only small bits until ALL machines converge, and read it once.
        int j = 0;
        int d = 0;
        while (true) {
            if (converge[j] == j+1) {
                if (readToNextBreak(aEncoding[j], j)) {
                    break;
                }
            }
            j = (j+1)%aEncoding.length;
        }

//        EncodingType finalEncoding = simulateDFA(aEncoding[0].pos, aEncoding[0].end);
//
//        for (int i = 0; i < aEncoding.length; i++) {
//            aEncoding[i].aString.append(finalEncoding.aString.toString());
//            aEncoding[i].end = finalEncoding.end;
//            aEncoding[i].pos = finalEncoding.pos;
//        }

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


    public NormalRunnable(int oThreads, String pString, DFAGraph dfa) {
        int partition = pString.length()/(oThreads+1);
        
        if (pString.length()%(oThreads+1) != 0) {
            partition++;
        }
        
        aString = pString.substring(0, partition);
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
        int lastPos = 0;
        State prevState = null;
        State currentState = aDFA.getState(1);
        // Main body. Read the string char by char and perform the necessary computations
        for (int i = 0; i < aString.length(); i++) {
            prevState = currentState;
            currentState = currentState.readChar(aString.charAt(i));
            if (currentState == null && prevState.equals(aDFA.getState(5))) {
                finalString.append("_");
                currentState = aDFA.getState(1);
                lastPos = i;
            } else if (currentState == null && !prevState.equals(aDFA.getState(5))) {
                String a = "";
                for (int j = 0; j < (i-lastPos); j++){
                    a = a+ "_";
                }
                finalString.replace(lastPos, i, a);
                if (aString.charAt(i) > 48 && aString.charAt(i) < 57)
                    i--;
                else
                    finalString.append("_");
                currentState = aDFA.getState(1);
                lastPos = i >= 0?i:0;
            } else {
                finalString.append(aString.charAt(i));
            }
        }

        if (currentState != null && currentState != aDFA.getState(5)) {
            String a = "";
            for (int j = 0; j < (finalString.length()-lastPos); j++){
                a = a+ "_";
            }
            finalString.replace(lastPos, finalString.length(), a);
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
            NormalRunnable nRun = new NormalRunnable(0, aString, mDFA);
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
