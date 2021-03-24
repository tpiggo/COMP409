package assignment3;
/**
 * NOTE: Always clean the directory out from the stuff you added 
 * Always run rmdir -f assignment3 before committing
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

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
    Queue<Character> charQueue;
    StringBuilder finalString;
    public DFAGraph() {
        /**
         * TODO: Make this less ugly. This set up could be improved?
         */
        this.states = new ArrayList<>();
        charQueue = new LinkedList<>();
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
        finalString = new StringBuilder();
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
    
    public String runOn(String aString) {
        State prevState = null;
        State currentState = this.startState;
        for (int i = 0; i < aString.length(); i++) {
            prevState = currentState;
            currentState = currentState.readChar(aString.charAt(i));
            if (currentState == null && prevState.equals(acceptState)) {
                while (!charQueue.isEmpty()) {
                    finalString.append(charQueue.poll());
                }
                finalString.append("_");
                currentState = this.startState;
            } else if (currentState == null && !prevState.equals(acceptState)) {
                while (!charQueue.isEmpty()) {
                    charQueue.poll();
                    finalString.append("_");
                }

                if (aString.charAt(i) > 48 && aString.charAt(i) < 57)
                    i--;
                else 
                    finalString.append("_");
                currentState = this.startState;
            } else {
                charQueue.add(aString.charAt(i));
            }
        }
        
        if (currentState != null && currentState.equals(acceptState)) {
            while (!charQueue.isEmpty()) {
                finalString.append(charQueue.poll());
            }
        } else if (currentState != null && currentState != acceptState) {
            while (!charQueue.isEmpty()) {
                charQueue.poll();
                finalString.append("_");
            }
        }
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
        System.out.println("Running DFA on " + aString);
        aString = mDFA.runOn(aString);
        for (int i = 0; i < 5; i++) {
            State a = mDFA.getState(i+1);
            System.out.println("State id = " + a.id);
        }
        System.out.println("OUTPUT: " + aString);
    }
}
