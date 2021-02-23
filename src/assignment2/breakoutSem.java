//TODO: REMOVE THIS
package assignment2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

// TODO: UNCOMMENT THIS
//enum Faculty {
//    ARTS, SCIENCE, ENGINEERING;
//    @Override
//    public String toString() {
//        return super.toString();
//    }
//}

class SemStudent implements Runnable{
    private SemBreakoutRoom aRoom;
    private Faculty aFaculty;
    private Thread aThread;
    int aK, aW;
    long aN;
    int aId;

    public SemStudent(SemBreakoutRoom pRoom, Faculty pFaculty, int k, int w, int n, int pId){
        aRoom = pRoom;
        aFaculty = pFaculty;
        aK = k;
        aW = w;
        aN = n * 1000;
        aThread = new Thread(this);
        aId = pId;
    }

    public void start(){
        aThread.start();
    }

    public void join(){
        try  {
            aThread.join();
        } catch (InterruptedException e){
            e.printStackTrace();
        }
    }

    public Faculty getFaculty(){
        return aFaculty;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < aN){
            int sleepBefore = ThreadLocalRandom.current().nextInt(1,10);
            try {
                // sleep before entering.
                Thread.sleep(aK * sleepBefore);
                aRoom.enter(this);
                // you're in!
                int sleepTime = ThreadLocalRandom.current().nextInt(1,10);
                Thread.sleep(aW * sleepTime);
                aRoom.exit();
            } catch (InterruptedException e) {
                // Do nothing.
                e.printStackTrace();
            }
        }
    }
}

class SemBreakoutRoom{
    private Semaphore useDoor = new Semaphore(1);
    private Semaphore queue = new Semaphore(1);
    private Semaphore wait = new Semaphore(1);
    private Faculty currentFaculty = null;
    private int curCounter;
    private int maxIn;


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

    public void enter(SemStudent pStudent) {
        // Do nothing
        queue.acquireUninterruptibly();
        boolean entered = false;
        while (!entered){
            useDoor.acquireUninterruptibly();
            if (currentFaculty == pStudent.getFaculty()) {
                // Enter.
                curCounter++;
                entered = true;
                useDoor.release();
            } else if (currentFaculty == null) {
                curCounter++;
                currentFaculty = pStudent.getFaculty();
                printOwnershipChange();
                // block the next person who is not
                wait.acquireUninterruptibly();
                entered = true;
                useDoor.release();
            } else {
                useDoor.release();
                // wait on the condition.
                wait.acquireUninterruptibly();
                wait.release();
            }
        }
        queue.release();
    }

    public void exit() {
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

        Collections.shuffle(students);
        students.forEach(student -> {
            student.start();
        });
        students.forEach(student -> {
            student.join();
        });

        breakoutRoom.printPeople();
        breakoutRoom.printMaxIn();
        System.out.println("Done.");

    }
}

