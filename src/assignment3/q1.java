package assignment3;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
    private State start;
    private State end;
    private StringBuilder aString;
    private int pos = 0;
    private int lastPos = 0;
    EncodingType nextPortion;

    public EncodingType(State pStart, State pEnd, String pString) {
        this.start = pStart;
        this.end = pEnd;
        this.aString = new StringBuilder(pString);
    }

    public String getString() {
        EncodingType aType = nextPortion;
        int start = aString.length();
        while (aType != null){
            // go through the list and get the elements;
            aString.append(aType.aString.substring(start));
            start = aType.aString.length();
            end = aType.end;
            lastPos = aType.lastPos;
            aType = aType.nextPortion;
        }
        return aString.toString();
    }
    
    public void replaceFromLast(){
        for (int i = lastPos; i < pos; i++){
            aString.setCharAt(i,'_');
        }
        lastPos = aString.length();
    }

    public void replaceFromIndex(int ind) {
        for (int i = lastPos; i < ind; i++){
            aString.setCharAt(i,'_');
        }
        lastPos = ind;
    }

    public void append(char aChar){
        aString.append(aChar);
    }

    public void appendUnderscore(){
        aString.append("_");
    }

    public int getLastPos() {
        return lastPos;
    }

    public int getPos() {
        return pos;
    }

    public State getEnd() {
        return end;
    }

    public void setEnd(State pEnd) {
        end = pEnd;
    }

    public void setLastPos(int pLastPos) {
        lastPos = pLastPos;
    }

    public void setPos(int i){
        pos = i;
    }

    public void incrementPos() {
        pos++;
    }

    public void decrementPos() {
        pos--;
    }


    public boolean areChanges(){
       return aString.length() > lastPos;
    }

    public void commitChanges() {
        lastPos = pos;
    }

    public State getStart() {
        return start;
    }
}

class DFAReader{
    private ExecutorService executor;
    private DFAGraph dfa;
    int id;
    
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
    DFAGraph dfa;
    String aString;
    int id;
    int maxPos;
    boolean isLast;
    EncodingType[] aEncoding =  new EncodingType[5];
    boolean [] converge;
    
    public DFATask(DFAGraph pDFA, String inputString, int pId) {
        dfa = pDFA;
        aString = inputString;
        id = pId;
        maxPos = 1;
        isLast = false;
        // Create empty encodings
        for (int i = 0; i < aEncoding.length; i++){
            aEncoding[i] = new EncodingType(dfa.getState(i+1), dfa.getState(i+1), "");
        }
        converge = new boolean[]{false,false,false,false,false};
    }

    public DFATask(DFAGraph pDFA, String inputString, int pId, boolean pIsLast){
        this(pDFA,inputString, pId);
        isLast = pIsLast;
    }

    private void simulateDFA(EncodingType pEncoding) {
        State prevState, currentState = pEncoding.getEnd();
        // Main body. Read the string char by char and perform the necessary computations
        int i = pEncoding.getPos();
//        if (id == 1){
//            System.out.println("Here with id 1");
//        }
        boolean checkEncodings = true;
        for (; i < aString.length(); i++) {
            prevState = currentState;
//            if (id == 1){
//                System.out.println("Here with id 1");
//            }
            currentState = currentState.readChar(aString.charAt(i));
            if (currentState == null && prevState.equals(dfa.getState(5))) {
                pEncoding.appendUnderscore();
                currentState = dfa.getState(1);
                pEncoding.setLastPos(i);
                // Logic to remove any weirdness in it.
                if (checkEncodings){
                    int isFalse = 0;
                    for (int j = 0; j < converge.length; j++){
                        if (converge[j] && aEncoding[j].areChanges()){
                            aEncoding[j].commitChanges();
                            converge[j] = false;
                        } else if (!converge[j]){
                            // First one who is not merged you're done.
                            isFalse++;
                        }
                    }
                    // stop checking since all others could be done
                    if (isFalse == 4)
                        checkEncodings = false;
                }
            } else if (currentState == null && !prevState.equals(dfa.getState(5))) {
                pEncoding.replaceFromIndex(i);
                if (aString.charAt(i) > 48 && aString.charAt(i) < 57)
                    i--;
                else
                    pEncoding.appendUnderscore();
                currentState = dfa.getState(1);
                // Logic to remove any weirdness in it.
                if (checkEncodings){
                    int isFalse = 0;
                    for (int j = 0; j < converge.length; j++){
                        if (converge[j] && aEncoding[j].areChanges()){
                            System.out.println("Here1");
                            // Commits the changes without forcing us to.
                            aEncoding[j].replaceFromLast();
                            isFalse++;
                            converge[j] = false;
                        } else if (!converge[j]){
                            // First one who is not merged you're done.
                            isFalse++;
                        }
                    }
                    if (isFalse == 4)
                        checkEncodings = false;
                }
                pEncoding.setLastPos(i);
            } else {
                pEncoding.append(aString.charAt(i));
            }
        }
        pEncoding.setPos(i);
        // Need to make this so that we can check as the modifier
        if (isLast && currentState != null && currentState != dfa.getState(5)){
            pEncoding.replaceFromLast();
        }

        pEncoding.setEnd(currentState);
        pEncoding.setPos(i);
    }

    private void readOneCharacter(EncodingType pEncoding, int pId){
        State curState = pEncoding.getEnd();
        State prevState = curState;
        curState = curState.readChar(aString.charAt(pEncoding.getPos()));
        if (curState == null && prevState.equals(dfa.getState(5))){
            pEncoding.appendUnderscore();
            curState = dfa.getState(1);
            // Fix any of the converged ones to not erase!
            for (int j = pId-1; j >= 0; j--) {
                if (converge[j] && aEncoding[j].areChanges()){
                    aEncoding[j].commitChanges();
                } else if (!converge[j]){
                    // First one who is not merged you're done.
                    break;
                }
            }
            pEncoding.setLastPos(pEncoding.getPos()+1);
        } else if (curState == null && !prevState.equals(dfa.getState(5))) {
            pEncoding.replaceFromLast();
            curState = dfa.getState(1);
            if (aString.charAt(pEncoding.getPos()) > 48 && aString.charAt(pEncoding.getPos()) < 57 && pEncoding.getPos() >0 )
                pEncoding.decrementPos();
            else
                pEncoding.appendUnderscore();
            // Need to fix those who are merged with you.
            for (int j = pId-1; j >= 0; j--) {
                if (converge[j] && aEncoding[j].areChanges()){
                    System.out.println("Here1");
                    aEncoding[j].replaceFromLast();
                } else if (!converge[j]){
                    // First one who is not merged you're done.
                    break;
                }
            }
            pEncoding.setLastPos(pEncoding.getPos()+1);
        } else {
            pEncoding.append(aString.charAt(pEncoding.getPos()));
        }
        // increment the length
        pEncoding.incrementPos();
        pEncoding.setEnd(curState);
    }

    private void endStringFix(EncodingType pEncoding, int pId) {
        if (isLast && pEncoding.getEnd() != null && !pEncoding.getEnd().equals(dfa.getState(5))){
            for (int i = pId-1; i >= 0; i--) {
                if (aEncoding[i].nextPortion != null && aEncoding[i].nextPortion.equals(pEncoding)) {
                    // Must fix it!
                    aEncoding[i].replaceFromLast();
                }
            }
            pEncoding.replaceFromLast();
        }
    }


    public EncodingType[] call() throws Exception {
        // Read only small bits until ALL machines converge, and read it once.
        int j = 0;
        for (; j < aString.length(); j++) {
            int fourTrue = 0;
            for (int i = 0; i < converge.length; i++){
                if (!converge[i]) {
                    readOneCharacter(aEncoding[i], i);
                } else {
                    fourTrue++;
                }
            }

            if (fourTrue == 4){
                break;
            }

            for (int i = 0; i < converge.length-1; i++){
                if (!converge[i]) {
                    for (int k = i+1; k < converge.length; k++){
                        if (aEncoding[i].getEnd().equals(aEncoding[k].getEnd()) && !converge[k]){
                            converge[i] = true;
                            aEncoding[i].nextPortion = aEncoding[k];
                        }
                    }
                }
            }
        }

        int x = 0;

        if (j >= aString.length()) {
            // read the entire string with at least 2 states in parallel. This is for defense against
            // a string which just didn't merge fully. Corner case strings.
            for (int i = 0; i < converge.length; i++) {
                if (!converge[i]){
                    endStringFix(aEncoding[i], i);
                }
            }
        } else {
            // Have forced everyone to converge into aEncoding[4] thus use it.
            simulateDFA(aEncoding[4]);
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


    public NormalRunnable(int oThreads, String pString, DFAGraph dfa) {
        int partition = pString.length()/(oThreads+1);
        
        if (pString.length()%(oThreads+1) != 0) {
            partition++;
        }
        
        aString = pString.substring(0, partition);
        aFutures = new ArrayList<>();
        subtasks = new DFATask[oThreads];

        int i = 1;
        for (; i < oThreads+1; i++) {
            int strLen = partition*(i+1)<pString.length()?partition*(i+1):pString.length();
            String str = pString.substring((i)*partition, strLen);
            if (i+1 >= oThreads + 1){
                subtasks[i-1] = new DFATask(dfa, str, i, true);
            } else {
                subtasks[i-1] = new DFATask(dfa, str, i);
            }
        }

        if (oThreads != 0)
            aReader = new DFAReader(dfa, oThreads);
        aDFA = dfa;
        finalString = new StringBuilder();
    }

    private String createUnderscores(int num){
        StringBuilder aBuilder = new StringBuilder();
        for (int i = 0; i < num; i++)
            aBuilder.append("_");
        return aBuilder.toString();
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

        if (aReader == null){
            if (currentState != null && currentState != aDFA.getState(5)) {
                String a = "";
                for (int j = 0; j < (finalString.length()-lastPos); j++){
                    a = a+ "_";
                }
                finalString.replace(lastPos, finalString.length(), a);
            }
        } else  {
            System.out.print("We are here\n");
        }
        // Set the end state
        endState = currentState;
        // join them all!
        for (Future<EncodingType[]> future: aFutures) {
            try {  
                EncodingType[] eArr = future.get();
                for (int i = 0; i < eArr.length; i++) {
                    if (eArr[i].getStart().equals(endState)) {
                        // Only want to build this string once.
                        String fString = eArr[i].getString();
                        if (fString.charAt(0) == '_' && !endState.equals(aDFA.getState(5))) {
                            String a = createUnderscores(finalString.length() - lastPos);
                            finalString.replace(lastPos, finalString.length(), a);
                        }

                        // check if the string was valid from the beginning.
                        // Then the string appended may not be valid thus only change it if it is.
                        if (eArr[i].getLastPos() > 0) {
                            lastPos = finalString.length() + eArr[i].getLastPos();
                        }

                        finalString.append(fString);
                        endState = eArr[i].getEnd();
                        break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (aReader != null)
            aReader.shutdown();
    }

    public String getString() {
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
