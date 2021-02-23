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

    /**
     * Create a new student
     * @param pRoom The breakout room
     * @param pFaculty The students faculty
     * @param k sleep time outside the room
     * @param w sleep time inside the room
     * @param n max running time
     * @param pId id of the student.
     */
    public Student(BreakoutRoomMon pRoom, Faculty pFaculty, int k, int w, int n, int pId){
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

class BreakoutRoomMon {
    Faculty currentFaculty = null;
    ReentrantLock doorLock = new ReentrantLock(true);
    Condition isFree = doorLock.newCondition();
    Condition queueWait = doorLock.newCondition();
    Faculty firstFaculty = null;
    int curCounter = 0;
    int maxIn = 0;

    /**
     * Printing functions
     */
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

    /**
     * Atomic entry into the breakout room. Fairness based on lock and implicit queue built on the lock.
     * @param pStudent Student which intends to enter the room.
     * NOTE: Catching the interrupted exceptions during the sleeping loop. Therefore, we do not need to force throw on
     * the function.
     */
    public void enter(Student pStudent) {

        doorLock.lock();
        while (firstFaculty != pStudent.getFaculty() && firstFaculty != null)
            try {queueWait.await();}catch (InterruptedException e) { e.printStackTrace();}

        // should only get through here if you are the first faculty in the line.
        // This works through the fairness of the lock.
        while (currentFaculty != pStudent.getFaculty() && currentFaculty != null) {
            firstFaculty = pStudent.getFaculty();
            try { isFree.await(); }catch (InterruptedException e) { e.printStackTrace();}
        }

        if (currentFaculty == null){
            currentFaculty = pStudent.getFaculty();
            printOwnershipChange();
            // Now we can
            firstFaculty = null;
        }

        queueWait.signal(); // signal the first person in the queue.

        curCounter++;
        // Let the next student in
        doorLock.unlock();
    }

    /**
     * Atomic exit from the breakout room.
     */
    public void exit() {
        doorLock.lock();
        maxIn = Math.max(maxIn, curCounter);
        curCounter--;
        // Atomically release the room if it is empty.
        if (curCounter == 0){
            currentFaculty = null;
            printOwnershipChange();
            isFree.signalAll(); // tell every person in the implicit queue that its free.
        }
        doorLock.unlock();
    }
}


public class breakoutMon {
    public static void main(String [] args){
        if (args.length < 3){
            throw new IllegalArgumentException("Missing command line arguments");
        }

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
