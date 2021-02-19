package assignment2;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

enum FacultySem {
    ARTS, SCIENCE, ENGINEERING
}

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
            } catch (InterruptedException e) {
                // Do nothing.
            }
            // try to enter the room.
            try {
                if (aRoom.enter(this)){
                    // you're in!
                    int sleepTime = ThreadLocalRandom.current().nextInt(1,10);
                    Thread.sleep(aW * sleepTime);
                    aRoom.exit(this);
                }
            } catch (InterruptedException e) {
                // Do nothing.

            }
        }
    }
}

class SemBreakoutRoom{
    Semaphore grabPass = new Semaphore(4);
    int aOwnershipChange = 0;
    int curCounter = 0;
    Faculty currentFaculty = null;
    Faculty lastFaculty = null;
    private int artsLast = 0;
    private int scienceLast = 0;
    private int engLast = 0;

    public void printOwnershipChange(){
        String faculty = currentFaculty==null?"empty":currentFaculty + "";
        System.out.println("Ownership change to " + faculty);
    }

    public void printPeople(){
        System.out.println("Num in room= " + curCounter);
    }
    public void printFailure(){
        System.out.println("Failed to enter");
    }

    public void printEntering(int i){
        System.out.println("S "+ i + " from "+ currentFaculty + " entering the room = " + curCounter);
    }

    public void printLeave(int i){
        System.out.println("S "+ i + " from "+ currentFaculty + " leaving the room. Num in = " + curCounter);
    }

    // TODO: not fair.
    public synchronized boolean enter(SemStudent pStudent) {
        boolean entered = false;
        try {
            if (currentFaculty == null && lastFaculty != pStudent.getFaculty()) {
                currentFaculty = pStudent.getFaculty();
                switch (currentFaculty) {
                    case ARTS:
                        artsLast++;
                        break;
                    case ENGINEERING:
                        engLast++;
                        break;
                    case SCIENCE:
                        scienceLast++;
                }
                aOwnershipChange++;
                curCounter++;
                printOwnershipChange();
                entered = true;
                // you've enter.
                grabPass.acquire();
            } else if (currentFaculty == pStudent.getFaculty()) {
                curCounter++;
                entered = true;
                grabPass.acquire();
            }

        } catch (InterruptedException e){
            e.printStackTrace();
        }

        return entered;
    }

    public synchronized void exit(SemStudent pStudent) {
        curCounter--;
        grabPass.release();
        if (grabPass.availablePermits() == 4){
            // its empty, no one left in the room.
            lastFaculty = currentFaculty;
            currentFaculty = null;
            printOwnershipChange();
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
        System.out.println("Done.");

    }
}

