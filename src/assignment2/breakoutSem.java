//TODO: REMOVE THIS
package assignment2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;


enum Faculty {
    ARTS, SCIENCE, ENGINEERING;
    @Override
    public String toString() {
        return super.toString();
    }
}

class SemStudent implements Runnable{
    private SemBreakoutRoom aRoom;
    private Faculty aFaculty;
    private Thread aThread;
    int aK, aW;
    long aN;
    int aId;

    /**
     * Create a new student
     * @param pRoom The breakout room
     * @param pFaculty The students faculty
     * @param k sleep time outside the room
     * @param w sleep time inside the room
     * @param n max running time
     * @param pId id of the student.
     */
    public SemStudent(SemBreakoutRoom pRoom, Faculty pFaculty, int k, int w, int n, int pId){
        aRoom = pRoom;
        aFaculty = pFaculty;
        aK = k;
        aW = w;
        aN = n * 1000;
        aThread = new Thread(this);
        aId = pId;
    }

    /**
     * Start the threads.
     */
    public void start(){
        aThread.start();
    }

    /**
     * Join the threads.
     */
    public void join(){
        try  {
            aThread.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    // accessor
    public Faculty getFaculty(){
        return aFaculty;
    }

    /**
     * Main student thread method.
     */
    @Override
    public void run() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < aN){
            int sleepBefore = ThreadLocalRandom.current().nextInt(1,10);
            // sleep before entering.
            try {
                Thread.sleep(aW * sleepBefore);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            aRoom.enter(this);
            // you're in!
            int sleepTime = ThreadLocalRandom.current().nextInt(1,10);
            // System.out.print("I have entered the room sleeping " + (aW * sleepTime));
            try {
                Thread.sleep(aW * sleepTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            aRoom.exit();
        }
    }
}

class SemBreakoutRoom{
    // lock
    private Semaphore useDoor = new Semaphore(1);
    private Semaphore queue = new Semaphore(1);
    // condition variables
    private Semaphore wait = new Semaphore(1);
    private Faculty currentFaculty = null;
    private int curCounter;
    private int maxIn;

    /**
     * Printing functions
     */
    public void printOwnershipChange() {
        String faculty = currentFaculty==null?"empty":currentFaculty + "";
        System.out.println("Ownership change to " + faculty);
    }

    public void printPeople(){
        System.out.println("Num in room = " + curCounter);
    }
    public void printMaxIn(){
        System.out.println("Max Num in room = " + maxIn);
    }

    /**
     * Atomic entry into the room. Enter a queue in order to ensure fairness, not an explicit queue however.
     * @param pStudent Student trying to enter.
     */
    public void enter(SemStudent pStudent) {

        // enter the implicit queue.
        queue.acquireUninterruptibly();
        useDoor.acquireUninterruptibly();

        while (currentFaculty != pStudent.getFaculty() && currentFaculty != null){
            useDoor.release();
            // wait on the condition. blocks here, failed to get in.
            wait.acquireUninterruptibly();
            wait.release();
            useDoor.acquireUninterruptibly();
        }

        if (currentFaculty == null) {
            currentFaculty = pStudent.getFaculty();
            printOwnershipChange();
            // block the next person who is not
            wait.acquireUninterruptibly();
        }
        curCounter++;

        // You can let the next person behind you to test.
        useDoor.release();
        queue.release();
    }

    /**
     * Atomically leaving the room and releasing the room if it is free.
     */
    public void exit() {
        // Use the door, only one person can use it at one time.
        useDoor.acquireUninterruptibly();
        maxIn = Math.max(curCounter, maxIn);
        curCounter--;
        if (curCounter == 0){
            currentFaculty = null;
            // System.out.println("Trying to enter an empty room. Give me the pass");
            printOwnershipChange();
            wait.release();
        }
        useDoor.release();
    }

}


public class breakoutSem {
    public static void main(String [] args){
        if (args.length < 3){
            throw new IllegalArgumentException("Missing command line arguments");
        }

        int n = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);
        int w = Integer.parseInt(args[2]);

        SemBreakoutRoom breakoutRoom = new SemBreakoutRoom();
        // Create 12 students.
        List<SemStudent> students = new ArrayList<>(12);
        for (int i = 0; i< 12; i++){
            int j = i % 3;
            Faculty faculty;
            switch (j){
                case 1:
                    faculty = Faculty.ARTS ;
                    break;
                case 2:
                    faculty = Faculty.ENGINEERING ;
                    break;
                default:
                    faculty = Faculty.SCIENCE ;
                    break;
            }
            students.add(new SemStudent(breakoutRoom, faculty, k, w, n, i+1));
        }
        // Shuffle them!
        Collections.shuffle(students);
        students.forEach(student -> {
            student.start();
        });

        // join them
        students.forEach(student -> {
            student.join();
        });

        breakoutRoom.printPeople();
        breakoutRoom.printMaxIn();
        System.out.println("Done.");

    }
}

