//TODO: REMOVE THIS
package assignment2;

import java.util.concurrent.ThreadLocalRandom;
/**
 * Tile color wrapper enum class.
 */
enum TileColor{
    BROWN, BEIGE
}

/**
 * Wrapper for movement along diagonals within the board.
 */
class Move {
    int x, y;

    /**
     * Creates a new move.
     * @param x movement along the x axis
     * @param y movement along the y axis
     */
    public Move(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

/**
 * Wrapper class around the board of Tiles.
 */
class Board{
    public Tile [][] aBoard;
    public int aHeight;
    public int aWidth;

    /**
     * Creates a new board.
     * @param width the width of the board.
     * @param height the height of the board.
     */
    public Board(int width, int height) {
        aWidth = width;
        aHeight = height;
        // Making the board easier to work with. First element is the y value, second element is x value
        aBoard = new Tile[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++){
                // Setting to brown
                TileColor tileColor = TileColor.BROWN;
                if ((y % 2 ==0 && x % 2 != 0 )|| (y % 2 != 0 && x % 2 == 0)){
                    tileColor = TileColor.BEIGE;
                }
                aBoard[y][x] = new Tile(tileColor, x, y);
            }
        }
    }

    /**
     * Prints out the board for easy visualization.
     */
    public void printBoard(){
        String aString = "========================================================================\n";
        for (int y = 0; y < aHeight; y++) {
            for (int x = 0; x < aWidth; x++) {
                aString+= aBoard[y][x].aColour + aBoard[y][x].toString();
                if (x + 1 < aWidth){
                    aString += " | ";
                }
            }
            if (y + 1 < aHeight){
                aString += "\n------------------------------------------------------------------------\n";
            }
        }
        System.out.println(aString+"\n========================================================================\n");
    }
}


/**
 * Class for synchronization. We build our board around these synchronized tiles.
 */
class Tile {
    TileColor aColour;
    int posX, posY;
    Checker aChecker;
    int lockingId;
    boolean locked = false;

    /**
     * Creates a new tile, given its x and y position.
     * @param colour The tile's color
     * @param x The tile's x position
     * @param y The tile's y position.
     */
    public Tile(TileColor colour, int x, int y){
        aColour = colour;
        posX = x;
        posY = y;
    }

    /**
     * Atomically set a checker on a free (unlocked) checker. A locked tile means someone is trying to use it.
     * We must wait until it is unlocked.
     * @param pChecker The checker we want to set down on the tile
     * @param isRespawning State the tile is in. Either true or false.
     * @throws InterruptedException
     */
    public synchronized void setChecker(Checker pChecker, boolean isRespawning) throws InterruptedException {
        // someones locking
        while (locked)
            wait();

        if (aChecker == null){
            aChecker = pChecker;

            if (isRespawning)
                pChecker.printRespawn(this);
            else
                pChecker.printSpawn(this);
            // sets the tiles.
            pChecker.setCurrentTile(this);
        }
    }

    /**
     * Removes a checker from the tile in an atomic move. Allows a locking move to finish before removing. Returns
     * if the checker you want to remove was removed successfully.
     * @param pChecker The checker we want to remove from the tile
     * @return Removal status, successful or not.
     * @throws InterruptedException
     */
    public synchronized boolean removeChecker(Checker pChecker) throws InterruptedException {
        // Let whomever locked you finish their move before removing.
        while (locked)
            wait();

        // If you are still here, remove yourself.
        if (aChecker == pChecker) {
            System.out.println("Removing T" +aChecker.id + " from " + toString());
            aChecker = null;
            return true;
        }
        return false;
    }

    /**
     * Frees a tile in an atomic move.
     */
    public synchronized void free(){
        aChecker = null;
        locked = false;
        lockingId = 0;
        notify();
    }

    /**
     * Move a checker to this tile and remove it from its old tile.
     * @param pChecker The checker we want to set down on the tile
     * @param oldTile The old tile which the checker was on.
     * @return The new tile.
     */
    public synchronized Tile moveChecker(Checker pChecker, Tile oldTile){
        // Not a state we are allowed to enter. If we do not hold the tile we should not be moving onto it.
        if (locked && (pChecker.id != lockingId && lockingId != 0))
            throw new IllegalStateException("T" + pChecker.id + " is moving onto a tile locked by T" + lockingId);
        else if (aChecker != null)
            throw new IllegalStateException("T" + pChecker.id + " there is someone on this tile.");
        aChecker = pChecker;
        locked = false;
        lockingId = 0;
        pChecker.printMove(this);
        oldTile.free();
        // Set the checkers current tile to the new tile.
        pChecker.setCurrentTile(this);
        // notify everyone waiting on this action to finish.
        notify();
        return this;
    }

    /**
     * Atomic test and performing a lock on the tile. If we lock a tile, we own it and the checker on it
     * cannot move away. If it is empty, no other checker can jump onto it.
     * @param pChecker The checker locking the tile.
     * @return Whether or not the lock was successful.
     * @throws InterruptedException
     */
    public synchronized boolean testAndLock(Checker pChecker) throws InterruptedException {
        // Someone is moving on you. Wait until they're done.
        while (locked)
            wait();

        if (pChecker != aChecker){
            // You are dead.
            return false;
        }
        locked = true;
        lockingId = pChecker.id;
        return true;
    }

    /**
     * Atomic jumping of a checker (capturing). We want to jump from the old tile over the captured tile
     * to this tile.
     * @param capturedTile The tile who's lock we are holding.
     * @param pChecker The checker moving onto this tile from oldTile
     * @param oldTile The tile the checker the tile was on before.
     * @return The success of the move.
     */
    public synchronized boolean testAndCaptureAndMove(Tile capturedTile, Checker pChecker, Tile oldTile) {
        // break a deadlock this way. If someone else is locking the tile (therefore, performing a move). Do
        // Not move onto this tile.
        if (locked || aChecker != null){
            return false;
        }

        if (locked && (pChecker.id != lockingId && lockingId != 0))
            throw new IllegalStateException("T" + pChecker.id + " is moving (during capture) onto a tile locked by T" + lockingId);
        else if (aChecker != null)
            throw new IllegalStateException("T" + pChecker.id + " there is someone on this tile while trying to capture");
        capturedTile.aChecker.printCaptured();
        pChecker.printCapture(capturedTile);
        capturedTile.free();
        moveChecker(pChecker, oldTile);
        return true;
    }

    public synchronized boolean testAndCaptureAndMove2(Tile capturedTile, Checker pChecker, Tile oldTile) throws InterruptedException {
        // break a deadlock this way. If someone else is locking the tile (therefore, performing a move). Do
        // Not move onto this tile.

        if (aChecker != null) {
            return false;
        }

        while (locked)
            wait();

        if (locked || aChecker != null){
            return false;
        }

        if (locked && (pChecker.id != lockingId && lockingId != 0))
            throw new IllegalStateException("T" + pChecker.id + " is moving (during capture) onto a tile locked by T" + lockingId);
        else if (aChecker != null)
            throw new IllegalStateException("T" + pChecker.id + " there is someone on this tile while trying to capture");
        capturedTile.aChecker.printCaptured();
        pChecker.printCapture(capturedTile);
        capturedTile.free();
        moveChecker(pChecker, oldTile);
        return true;
    }

    /**
     * Test and lock a tile in an atomic method. Blocking if there is some other checker trying to move onto this tile.
     * @param id The id of the checker trying to lock the tile.
     * @return The success status of the lock.
     * @throws InterruptedException
     */
    public synchronized int testAndLock(int id) throws InterruptedException {
        // Check if someone is trying to move onto this tile
        while (locked && aChecker == null)
            wait();

        // Wait if there is some OTHER checker moving onto this tile.
        while (locked && aChecker.id != lockingId)
            wait();

        // Deadlock breaker. Do not wait if the person who owns the tile is the one locking it.
        if (locked && aChecker.id ==  lockingId){
            return 2;
        }

        // You can move onto this tile now.
        int lockStat = 0;
        if (aChecker != null){
            lockStat = 1;
        }

        locked = true;
        lockingId = id;
        return lockStat;
    }

    /**
     * Unlock this tile.
     */
    public synchronized void unlock(){
        locked = false;
        lockingId = 0;
        notify();
    }

    /**
     * Checks if the checker is currently on this tile.
     * @param pChecker The checker which is supposed to be on this tile.
     * @return Whether or not the checker is still here.
     */
    public synchronized boolean checkerIsOn(Checker pChecker){
        if (pChecker != aChecker){
            return false;
        }
        return true;
    }

    /**
     * The position of the tile on the board.
     * @return the position of the tile.
     */
    public String toString(){
        return "(" + posX + ", " + (7-posY) + ")";
    }
}

class Checker {
    Tile currentTile;
    int id, numCaptures;
    Board aBoard;

    /**
     * Creates a new checker.
     * @param pBoard The board which the game is held on.
     * @param pId the id of the checker.
     */
    public Checker(Board pBoard, int pId) {
        id = pId;
        aBoard = pBoard;
    }

    /**
     * Spawns a checker.
     * @throws InterruptedException
     */
    public void spawn() throws InterruptedException {
        genericSpawn(false);
    }

    /**
     * Spawning a checker generically onto the board by picking random tiles until it finds a free tile.
     * @param isRespawn
     * @throws InterruptedException
     */
    private void genericSpawn(boolean isRespawn) throws InterruptedException {
        boolean spawned = false;
        while(!spawned){
            int x = ThreadLocalRandom.current().nextInt(0, 8);
            int y = ThreadLocalRandom.current().nextInt(0, 4);
            if (x % 2 == 0){
                y = (y * 2);
            } else {
                y = (y * 2) + 1;
            }

            aBoard.aBoard[y][x].setChecker(this, isRespawn);
            if (currentTile != null){
                spawned = true;
            }
        }
    }

    /**
     * Respawns a checker onto the board.
     * @throws InterruptedException
     */
    public void respawn() throws InterruptedException {
        genericSpawn(true);
    }

    /**
     * Print methods required by the assignment
     */
    protected void printCaptured() {
        String out = "T"+id+": captured on " + currentTile.toString() ;
        System.out.println(out);
    }

    protected void printSpawn(Tile pTile) {
        System.out.println("T"+ id+": spawns on " + pTile.toString());
    }

    protected void printRespawn(Tile pTile) {
        System.out.println("T"+ id+": respawns on " + pTile.toString());
    }

    public void printMove(Tile pTile){
        System.out.println("T"+id+ ": moved to " + pTile.toString());
    }

    public void printCapture(Tile pTile){
        System.out.println("T"+id+ ": captures " + pTile.toString());
    }

    public void printCaptures(){
        System.out.println("T"+id+ " has " + numCaptures + " captures");
    }

    /**
     * Remove a checker from the board.
     * @throws InterruptedException
     */
    public void remove() throws InterruptedException {
        if (!currentTile.removeChecker(this)){
            System.out.println("T14: Not on the board, thus already removed!");
        }
    }

    /**
     * Set the current tile of the checker. Used during moves.
     * @param pTile The new tile which we need to set.
     */
    public void setCurrentTile(Tile pTile) {
        currentTile = pTile;
    }

    /**
     * Checks the liveliness of the checker.
     * @return Whether the checker is where you left it.
     */
    public boolean isAlive(){
        return currentTile.checkerIsOn(this);
    }

    /**
     * Test and capture atomic action wrapper used to make a capture move on the board.
     * @param capturedTile The captured tile
     * @param x the x position of the tile which we need to jump to.
     * @param y the y position of the tile which we need to jump to.
     * @return Whether it was successful or not.
     */
    private boolean testAndCapture(Tile capturedTile, int x, int y){
        return aBoard.aBoard[y][x].testAndCaptureAndMove(capturedTile, this, currentTile);
    }

    /**
     * Calculate the next move.
     * @param dir the randomly selected direction.
     * @return The new move.
     */
    public Move getMove(int dir){
        return switch (dir) {
            case 1 -> new Move(1, 1);
            case 2 -> new Move(1, -1);
            case 3 -> new Move(-1, -1);
            default -> new Move(-1, 1);
        };
    }

    /**
     * Moving a checker around the board. The checker controller accesses this method in order to make the move.
     * Nothing atomic about this move. However, it is thread safe since we use the embedded thread safe tiles for all
     * movement thus guaranteeing thread safety.
     * @return The success of the move.
     * @throws InterruptedException
     */
    public boolean moveChecker() throws InterruptedException {
        int i = 0;
        int moveDir = ThreadLocalRandom.current().nextInt(0, 4);
        while (i < 4){
            // Check if you are captured.
            if (!currentTile.testAndLock(this)){
                // You're captured.
                return false;
            }

            // You hold the lock ot your own tile.
            Move nextMove = getMove(moveDir);
            int x = currentTile.posX + nextMove.x;
            int y = currentTile.posY + nextMove.y;
            if (!(x < 0 || x > 7 || y < 0 || y > 7)) {
                int locked = aBoard.aBoard[y][x].testAndLock(id);
                if (locked == 1){
                    int x2 = x + nextMove.x;
                    int y2 = y + nextMove.y;
                    if (!(x2 < 0 || x2 > 7 || y2 < 0 || y2 > 7) && testAndCapture(aBoard.aBoard[y][x], x2, y2)){
                        numCaptures++;
                        return true;
                    } else {
                        aBoard.aBoard[y][x].unlock();
                    }
                } else if (locked == 0){
                    aBoard.aBoard[y][x].moveChecker(this, currentTile);
                    return true;
                }
            }

            // unlock and calculate the next move
            currentTile.unlock();
            i++;
            moveDir = (moveDir + i) % 4;
        }
        // failed to move. return.
        return false;
    }
}

class CheckerController implements Runnable {
    Thread aThread;
    Checker aChecker;
    int numMoves = 0;
    int k;
    long n;

    /**
     * Creates a new Checker controller which is the thread which will control a checker on the board.
     * @param pChecker The checker which is to be controlled.
     * @param pK sleep time
     * @param pN number of moves
     */
    public CheckerController(Checker pChecker, int pK, int pN){
        aChecker = pChecker;
        aThread = new Thread(this);
        k = pK;
        n = pN;
    }

    /**
     * Start the thread
     */
    public void start(){
        aThread.start();
    }

    /**
     * Join the threads.
     * @throws InterruptedException
     */
    public void join() throws InterruptedException {
        aThread.join();
    }

    /**
     * The main method on which the controller operates. All movement, is called from this method, sleeping and respawning.
     */
    @Override
    public void run() {

        while (numMoves < n){
            // Make sure you aren't dead before moving forward;
            try {
                if (aChecker.moveChecker()){
                    numMoves++;
                }
                Thread.sleep(k);

                if (!aChecker.isAlive()){
                    Thread.sleep(ThreadLocalRandom.current().nextInt(2,5) * k);
                    aChecker.respawn();
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Try to remove the checker.
        try {
            aChecker.remove();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


public class checkers{
    public static void main(String [] args) {
        int t, k, n;
        if (args.length >= 3) {
            t = Integer.parseInt(args[0]);
            k = Integer.parseInt(args[1]);
            n = Integer.parseInt(args[2]);
        } else {
            // Throw this error if missing arguments.
            throw new IllegalArgumentException("Missing arguments!");
        }

        Board tBoard = new Board(8,8);
        tBoard.printBoard();
        Checker[] checkers = new Checker[t];

        for (int i = 0; i < t; i++){
            checkers[i] = new Checker(tBoard, i+1);
            try {
                checkers[i].spawn();
            } catch (InterruptedException e) {
                System.out.println("Failed to spawn C" + (i+1));
            }
        }
        CheckerController[] controllers = new CheckerController[t];

        for (int i = 0; i < t; i++){
            controllers[i] = new CheckerController(checkers[i], k, n);
        }

        for (int i = 0; i < t; i++){
            controllers[i].start();
        }

        // join
        for (int i = 0; i < t; i++){
            try {
                controllers[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        int totalCap = 0;
        for (int i = 0; i < t; i++){
            // print number of captures done by each checker!
            checkers[i].printCaptures();
            totalCap += checkers[i].numCaptures;
        }

        System.out.println("Total number of captures: "+ totalCap + ". Done");
    }
}
