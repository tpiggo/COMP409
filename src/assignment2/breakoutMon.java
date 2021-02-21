package assignment2;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

enum Faculty {
    ARTS, SCIENCE, ENGINEERING
}

class Student implements Runnable{
    private Faculty aFaculty;
    private Thread aThread;
    private BreakoutRoomMon aRoom;
    int aK, aW, aId;
    long aN;
    boolean aWaiting = false;

    public Student(BreakoutRoomMon pRoom, Faculty pFaculty, int k, int w, int n, int pId){
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

    public boolean isWaiting(){
        return aWaiting;
    }

    public void nowWaiting(){
        aWaiting = true;
    }

    public void notWaiting(){
        aWaiting = false;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < aN){
            try {
                int sleepBefore = ThreadLocalRandom.current().nextInt(1,10);
                // sleep before entering.
                Thread.sleep(aK * sleepBefore);
                aRoom.enter(this);
                // you're in!
                int sleepTime = ThreadLocalRandom.current().nextInt(1,10);
                // System.out.print("I have entered the room sleeping " + (aW * sleepTime));
                Thread.sleep(aW * sleepTime);
                aRoom.exit(this);
            } catch (InterruptedException e) {
                // Do nothing.

            }
        }
    }
}

class BreakoutRoomMon {
    int aOwnershipChange = 0;
    Faculty currentFaculty = null;
    Faculty lastFaculty = null;
    int curCounter = 0;
    int maxIn = 0;
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

    public synchronized void enter(Student pStudent) throws InterruptedException {
        // Can't get in if the last faculty to hold it was yours.
        // Can't get in if there is a faculty in there already.
        boolean notMyFac = (currentFaculty != null && currentFaculty != pStudent.getFaculty());
        boolean wasLastFac = (currentFaculty == null && lastFaculty == pStudent.getFaculty());
        boolean facultyTurn = getNext()==null?true:pStudent.getFaculty()==getNext();
        int i = 0;
        while (notMyFac || (wasLastFac && !facultyTurn)){
            if (!inQueue(pStudent.getFaculty())){
                push(pStudent.getFaculty());
            }
            //System.out.println("S" + pStudent.aId + " in fac" + pStudent.getFaculty() +  " is waiting. Room is held by" + currentFaculty);
            wait();
            // Update the check
            wasLastFac = (lastFaculty == pStudent.getFaculty() && currentFaculty == null);
            notMyFac = (currentFaculty != null && currentFaculty != pStudent.getFaculty());
            facultyTurn = getNext()==null?true:pStudent.getFaculty()==getNext();
        }

        if (currentFaculty == null) {
            // You're acquiring the room.
            pop();
            currentFaculty = pStudent.getFaculty();
            aOwnershipChange++;
            printOwnershipChange();
            notifyAll();
        }
        curCounter++;
//        System.out.println("Current Fac: " + currentFaculty + " with " +curCounter);
    }


    public synchronized void exit(Student pStudent){
        maxIn = Math.max(maxIn, curCounter);
        curCounter--;
        if (curCounter == 0){
            // New faculty can enter.
            lastFaculty = pStudent.getFaculty();
            currentFaculty = null;
            printOwnershipChange();
            notifyAll();
        }
    }

}


public class breakoutMon {
    public static void main(String [] args){
        int n = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);
        int w = Integer.parseInt(args[2]);
        // Create 12 students.
        Student [] students = new Student[12];
        BreakoutRoomMon breakoutRoom = new BreakoutRoomMon();
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
            students[i] = new Student(breakoutRoom, faculty, k, w, n, i+1);
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
