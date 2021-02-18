package assignment1;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Name: Timothy Piggott
 * StudentID: 260855765
 */


/**
 * Friend class:
 * This class controls the Friends which are operating the doors. They hold their own id and the
 * leader communicates with the Friends
 * All communication and counting methods are synchronized. Both the aNumZombies and doorClosed
 * are protected by a synchronized method.
 */
class Friend implements Runnable{
    private int aId;
    private int aNumZombies;
    private Thread aThread;
    private Boolean doorClosed = false;
    private Boolean aThreadTerminate = false;

    public Friend(int id){
        super();
        aThread = new Thread(this);
        aNumZombies = 0;
        aId = id;
    }

    public synchronized void zombieIn(){
        // If you're terminated, skip this instruction.
        if (isTerminated() || doorClosed) return;
        // Checking if a door is closed must be made atomic since we can have some other thread writing it
        if (ThreadLocalRandom.current().nextFloat() < 0.1){
            aNumZombies++;
        }
    }

    public synchronized void terminate(){
        aThreadTerminate = true;
        doorClosed = true;
    }

    public synchronized int getZombieCount(){
        int temp = aNumZombies;
        aNumZombies = 0;
        // close the door
        doorClosed = true;
        return temp;
    }

    public synchronized Boolean isTerminated() {
        return aThreadTerminate;
    }

    // Communicate the door is open
    public synchronized void openDoor(){ doorClosed = false; }

    // Start the underlying thread
    public void startThread(){ this.aThread.start(); }

    // Join the underlying thread
    public void join() throws InterruptedException{ aThread.join(); }


    @Override
    public void run() {
        // Letting in the zombies and other random shit
        // Use a semaphore to stop everyone when the poll bit is 1
        try {
            while (true){
                // Let a zombie in possibly
                zombieIn();
                // TODO: Set this back to 10 @ 8ms its closer to a second.
                Thread.sleep(10);
                // check if you've been told to terminate.
                if (isTerminated()){
                    break;
                }
            }
        } catch (InterruptedException e){
            System.out.println(e);
        }
        System.out.println("Friend " + aId + " is taking a break!");
    }
}

class Leader implements Runnable {
    private int aZombies, aKills, aMaxZombies, aTotKills, aTotZombies;
    private ArrayList<Friend> aFriends;
    private Boolean doorsOpen = true;
    private Thread aThread;
    private long aMaxTime;
    private Boolean isTerminated = false;
    /**
     * Leader class
     */
    public Leader(int pMaxZombies, ArrayList<Friend> pFriends, long pMaxTime) {
        super();
        aTotKills = 0;
        aKills = 0;
        // Zombies in the room
        aZombies = 0;
        aMaxZombies = pMaxZombies;
        aFriends = pFriends;
        aTotZombies = 0;
        aThread = new Thread(this);
        // Max Time in minutes converted to milliseconds.
        aMaxTime = pMaxTime * 60;
    }

    // Start the underlying thread.
    public void startThread(){
        aThread.start();
    }

    // Join the internal thread.
    public void join() throws InterruptedException{ aThread.join(); }

    // Tell your friends to open the doors, one by one.
    public void openDoors(){
        doorsOpen = true;
        aFriends.forEach((friend) -> {
            friend.openDoor();
        });
    }

    // Accessor for the total number of zombies
    public int getTotalZombies() {
        return aTotZombies;
    }

    // Accessor for the total number of kills
    public int getTotKills(){ return aTotKills; }

    // Accessor for the throughput.
    public int throughPut() { return (int)(aTotKills/aMaxTime); }

    /**
     * Body of the thread. The leader is the main member who does most of the heavy lifting for the
     * team of zombie fighters. The leader checks every second and gets the count from the friends holding
     * each door open, tells them to close it and then sums all counts. Decides to open the doors or not.
     *
     * @Assumption Here we are counting time without making system calls. An assumption is that the instructions below
     * negligable time therefore, we count time through the time counter.
     *
     *
     *
     */

    @Override
    public void run() {
        // Start your friend
        for (int i = 0; i < aFriends.size(); i++) {
            // Start the friends
            this.aFriends.get(i).startThread();
        }
        // Time counter
        int totalTime = 0;
        try {
            while (true) {
                if (totalTime/1000 >= this.aMaxTime){
                    // terminate
                    isTerminated = true;
                    aFriends.forEach(Friend::terminate);
                }

                if (totalTime%1000 == 0 && totalTime != 0) {
                    // Call your friends
                    this.aFriends.forEach((friend) -> {
                        // get the number of zombies and close the door
                        int count = friend.getZombieCount();
                        aZombies += count;
                        aTotZombies += count;
                    });

                    // Fix our kills (swinging does not count as a kill)
                    if (aKills > aZombies){
                        // More swings than zombies, chill out.
                        aKills = aZombies;
                        aZombies = 0;
                    } else {
                        // Otherwise, clearly we have killed this many, keep going
                        aZombies -= aKills;
                    }

                    // Check if the number of zombies in the room is too great
                    if (!isTerminated && aZombies < aMaxZombies) {
                        // Reopen the doors
                        openDoors();
                    } else {
                        // Doors are closed
                        doorsOpen = false;
                    }

                    aTotKills+=aKills;
                    // Reset our values
                    aKills = 0;
                }
                // Making sure there is at least a zombie.
                if (ThreadLocalRandom.current().nextFloat() < 0.4) {
                    aKills++;
                    // If the number of zombies is below our threshold/2, we can let zombies again.
                    if (!isTerminated && !doorsOpen && (aZombies - aKills) < aMaxZombies/2) {
                        openDoors();
                    }
                } else if (isTerminated && aTotKills == aTotZombies){
                    break;
                }

                // Sleep
                Thread.sleep(10);
                totalTime += 10;
            }

            // Graceful termination code
            aFriends.forEach(friend -> {
                // Join our friends
                try{
                    friend.join();
                } catch (InterruptedException e){
                    System.out.println(e);
                }
            });
        } catch (InterruptedException e) {
            System.out.println(e);
        }
    }
}

public class Zombie{
    public static void main(String [] args){
        try {
            int friends=0, zombiesMax=0, maxTime = 1;
            if (args.length >= 2) {
                friends = Integer.parseInt(args[0]);
                zombiesMax = Integer.parseInt(args[1]);
                if (args.length == 3) {
                    // Allowing input for max running time.
                    maxTime = Integer.parseInt(args[2]);
                }
            } else {
                // Error
                throw new IllegalArgumentException("Missing input in command line!");
            }
            ArrayList<Friend> mFriends = new ArrayList<>();
            for (int i= 0; i < friends; i++){
                mFriends.add(new Friend(i+1));
            }
            // Create leader and start thread
            System.out.println("Starting the zombie invasion, lasting " + maxTime +" minutes!");

            Leader aLead = new Leader(zombiesMax, mFriends, maxTime);
            aLead.startThread();
            // Join the leader thread.
            aLead.join();
            String output = "=====================================\n"+
                    "\nNumber of zombies total: " + aLead.getTotalZombies() +
                    "\nNumber of kills total: " + aLead.getTotKills()+
                    "\nThroughtput: " + aLead.throughPut();
            System.out.println(output);
        } catch (InterruptedException e) {
            System.out.println(e);
        } catch (IllegalArgumentException e) {
            System.out.println(e);
        }
        System.out.print("Done!");
    }
}
