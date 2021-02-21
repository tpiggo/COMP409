package assignment2;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

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

    public long getTime(){
        return System.currentTimeMillis();
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

            }
        }
    }
}

class SemBreakoutRoom{
    private Semaphore useDoor = new Semaphore(1);
    private int aOwnershipChange = 0;
    private Faculty currentFaculty = null;
    private Faculty lastFaculty = null;
    private int curCounter;
    private int maxIn;
    private ArrayList<Faculty> facultiesWaiting = new ArrayList<>();


    public void printOwnershipChange(){
        String faculty = currentFaculty==null?"empty":currentFaculty + "";
        System.out.println("Ownership change to " + faculty);
    }

    public void printPeople(){
        System.out.println("Num in room= " + curCounter);
    }
    public void printMaxIn(){
        System.out.println("Max Num in room= " + maxIn);
    }

    private boolean inQueue(Faculty pFaculty){
        return facultiesWaiting.contains(pFaculty);
    }

    private void push(Faculty pFaculty){
        facultiesWaiting.add(pFaculty);
    }

    private Faculty pop(){
        if (facultiesWaiting.size() == 0){
            return null;
        }
        return facultiesWaiting.remove(0);
    }

    private Faculty getNext(){
        if (facultiesWaiting.size()==0){
            return null;
        }
        return facultiesWaiting.get(0);
    }

    // TODO: not fair.
    public void enter(SemStudent pStudent) {
        boolean entered = false;
        while (!entered) {
            try {
                // Wait on the semaphore.
                useDoor.acquire();
                if (currentFaculty == pStudent.getFaculty()) {
                    // Enter.
                    curCounter++;
                    entered = true;
                } else {
                    Faculty nextFac = getNext();
                    boolean facultyTurn = nextFac==null?true:pStudent.getFaculty()==nextFac;
                    if (currentFaculty == null && facultyTurn && lastFaculty != pStudent.getFaculty()) {
                        // Enter!
                        pop();
                        currentFaculty = pStudent.getFaculty();
                        aOwnershipChange++;
                        curCounter++;
                        printOwnershipChange();
                        entered = true;
                    } else {
                          if (!inQueue(pStudent.getFaculty())){
                            push((pStudent.getFaculty()));
                        }
                    }
                }
                useDoor.release();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void exit() {
        try {
            // Use the door to leave.
            useDoor.acquire();
            maxIn = Math.max(maxIn, curCounter);
            curCounter--;
            if (curCounter == 0){
                lastFaculty = currentFaculty;
                currentFaculty = null;
                printOwnershipChange();
            }
            useDoor.release();
        } catch (InterruptedException e){
            e.printStackTrace();
        }

    }

}


public class breakoutSem {
    public static void main(String [] args){
        int n = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);
        int w = Integer.parseInt(args[2]);
        // Create 12 students.
        SemStudent [] students = new SemStudent[12];
        SemBreakoutRoom breakoutRoom = new SemBreakoutRoom();
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
            students[i] = new SemStudent(breakoutRoom, faculty, k, w, n, i+1);
        }

        for (int i = 0; i < 12; i++) {
            students[i].start();
        }

        for (int i = 0; i < 12; i++) {
            students[i].join();
        }
        breakoutRoom.printPeople();
        breakoutRoom.printMaxIn();
        System.out.println("Done.");

    }
}

