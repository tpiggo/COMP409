//TODO: REMOVE THIS
package assignment2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

enum Faculty {
    ARTS, SCIENCE, ENGINEERING;
    @Override
    public String toString() {
        return super.toString();
    }
}

class Student implements Runnable{
    private Faculty aFaculty;
    private Thread aThread;
    private BreakoutRoomMon aRoom;
    int aK, aW, aId;
    long aN;

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
                aRoom.exit();
            } catch (InterruptedException e) {
                // Do nothing.

            }
        }
    }
}

class BreakoutRoomMon {
    Faculty currentFaculty = null;
    ReentrantLock doorLock = new ReentrantLock(true);
    ReentrantLock knobLock = new ReentrantLock(true);
    Condition isFree = doorLock.newCondition();
    Condition queueWait = doorLock.newCondition();
    ArrayList<Student> students = new ArrayList<>();
    int curCounter = 0;
    int maxIn = 0;

    public void printOwnershipChange(){
        String faculty = currentFaculty==null?"empty":currentFaculty.name();
        System.out.println("Ownership change to " + faculty);
    }

    public void printPeople(){
        System.out.println("Num in room = " + curCounter);
    }
    public void printMaxIn(){
        System.out.println("Max Num in room = " + maxIn);
    }

    public void enter(Student pStudent) throws InterruptedException {
        knobLock.lock();
        doorLock.lock();

        // You're the head of the line
        while (currentFaculty != pStudent.getFaculty() && currentFaculty != null)
            isFree.await();

        if (currentFaculty == null){
            currentFaculty = pStudent.getFaculty();
            printOwnershipChange();
        }

        curCounter++;
        // Let the next student in
        doorLock.unlock();
        knobLock.unlock();
    }

    public void exit(){
        doorLock.lock();
        maxIn = Math.max(maxIn, curCounter);
        curCounter--;
        if (curCounter == 0){
            currentFaculty = null;
            printOwnershipChange();
            isFree.signalAll();
        }
        doorLock.unlock();
    }
}


public class breakoutMon {
    public static void main(String [] args){
        int n = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);
        int w = Integer.parseInt(args[2]);
        // Create 12 students.
        BreakoutRoomMon breakoutRoom = new BreakoutRoomMon();

        List<Student> students = new ArrayList<>(12);
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
            students.add(new Student(breakoutRoom, faculty, k, w, n, i+1));
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
