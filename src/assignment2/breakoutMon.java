package assignment2;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Queue;

enum Faculty {
    ARTS, SCIENCE, ENGINEERING
}

class Student implements Runnable{
    private Faculty aFaculty;
    private Thread aThread;

    public Student(Faculty pFaculty){
        aFaculty = pFaculty;
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

    }
}

class BreakoutRoom {
    boolean isFree = true;
    int aOwnershipChange = 0;
    Faculty currentFaculty = null;
    private int artsLast = 0;
    private int scienceLast = 0;
    private int engLast = 0;

    public synchronized boolean enter(Student pStudent) throws InterruptedException {
        return false;
    }

    public synchronized void exit(Student pStudent){

    }

}


public class breakoutMon {
    public static void main(String [] args){
        int n = Integer.parseInt(args[0]);
        int k = Integer.parseInt(args[1]);
        int w = Integer.parseInt(args[2]);
        // Create 12 students.
        Student [] students = new Student[12];
        BreakoutRoom breakoutRoom = new BreakoutRoom();
        for (int i = 0; i< 12; i++){
            int j = i % 3;
            switch (j){
                case 1:
                    students[i] = new Student(Faculty.ARTS);
                    break;
                case 2:
                    students[i] = new Student(Faculty.ENGINEERING);
                    break;
                default:
                    students[i] = new Student(Faculty.SCIENCE);
                    break;
            }
        }
        students[0].start();

        students[0].join();
        System.out.println("Done.");

    }
}
