package assignment2;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO: COMMENT
 */

enum TileColor{
    BROWN, BEIGE
}

class Board {
    private Tile [][] aBoard;
    public int aHeight;
    public int aWidth;
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
     * Moves a checker onto a new tile. Changes the state of the old tile.
     * @param oldTile Tile the checker was previously on.
     * @param aChecker the checker which is moving.
     * @param x Next tile width position
     * @param y Next tile height position
     * @return the new tile
     */
    public Tile moveChecker(Tile oldTile, Checker aChecker, int x, int y) {
        return aBoard[y][x].setCheckerOn(aChecker, oldTile);
    }

    public TileColor getTileColor(int x, int y) {
        return aBoard[y][x].getColor();
    }
    public Tile getTile(int x, int y){
        return aBoard[y][x].get();
    }

    public boolean getFreeTile(int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7 ) {
            return false;
        }
        return aBoard[y][x].getIfFree();
    }

    public void removeCheckerFromBoard(int x, int y) {
        aBoard[y][x].removeChecker();
    }

    public String printBoard() {
        String aString = "========================================================================\n";
        for (int y = 0; y < aHeight; y++) {
            for (int x = 0; x < aWidth; x++) {
                aString+= aBoard[y][x].get().isFree();
                if (x + 1 < aWidth){
                    aString += " | ";
                }
            }
            if (y + 1 < aHeight){
                aString += "\n------------------------------------------------------------------------\n";
            }
        }
        return aString+"\n========================================================================\n";
    }

    public String toString() {
        String aString = "========================================================================\n";
        for (int y = 0; y < aHeight; y++) {
            for (int x = 0; x < aWidth; x++) {
                aString+= aBoard[y][x].getColor() + " (" + x + ", " + y +"): "+ aBoard[y][x].getInfo();
                if (x + 1 < aWidth){
                    aString += " | ";
                }
            }
            if (y + 1 < aHeight){
                aString += "\n------------------------------------------------------------------------\n";
            }
        }
        return aString+"\n========================================================================\n";
    }
}

class Tile {
    private TileColor aColor;
    private boolean aFree = true;
    private Checker aCheckerOn = null;
    public int aPosX, aPosY;

    public Tile(TileColor color, int x, int y) {
        // colour is 1 or 0, indicating the color;
        aColor = color;
        aPosX = x;
        aPosY = y;
    }

    public String getInfo(){
        String out ;
        if (aCheckerOn == null){
            return !aFree + " no checker on. ";
        }
        return !aFree + " checker on: " + aCheckerOn.aId;
    }
    public TileColor getColor() {
        return aColor;
    }

    // Place a checker on the tile. It should both be free.
    public synchronized Tile setCheckerOn(Checker checker, Tile oldTile) {
        if (!aFree){
            return null;
        }

        aFree = false;
        aCheckerOn = checker;

        // Free your previous tile.
        if (oldTile != null){
            oldTile.setFree();
        }
        return this;
    }

    public synchronized void removeChecker() {
        if (!aFree){
            aFree = true;
            aCheckerOn = null;
        }
    }

    // Release the tile!
    public synchronized void setFree() {
        aFree = true;
        aCheckerOn = null;
    }

    public synchronized boolean isFree() {
        return aFree;
    }

    public synchronized boolean getIfFree() {
        // you hold the lock on the tile now.
        if(aFree){
            aFree = false;
            return true;
        }

        return false;
    }

    /**
     * Capture the check on the tile.
     */
    public synchronized void captureCheckerOn(){
        aCheckerOn.capture();
        // Set the tile free
        setFree();
    }

    public synchronized boolean lockCheckerOn(){
        if (aFree){
            // System.out.println("------------- Failed to lock (" + aPosX +"," +aPosY+"). Nothing is here!!");
            return false;
        }

        // Guessing the checker is still here.
        return aCheckerOn.lockChecker(this);
    }

    /**
     * This checker is free to move.
     */
    public synchronized void unlockCheckerOn(){
        aCheckerOn.unlockChecker();
    }

    public synchronized Tile get(){
        return this;
    }
}

class Checker {
    private boolean aAlive = false;
    private volatile boolean aCanBeCapt = false;
    private boolean aCanMove = false;
    public int aId;
    public int aNumMoves;
    public int aCaptures;
    public int aMaxMoves;
    public Board aBoard;
    public Tile aCurrentTile = null;

    public String output;
    public String outputLines = "-------------------------";

    public Checker(int id, int maxMoves, Board pBoard) {
        aId = id;
        aMaxMoves = maxMoves;
        aBoard = pBoard;
        spawn();
        // You have been placed onto a good tile.
        printSpawn(aCurrentTile.aPosX, aCurrentTile.aPosY);
        output = "------------------------- T" +aId;
    }

    private void printMove(int x, int y) {
        String out = "T"+aId+": moves to (" + x + ","+  y+ ") @ " + aNumMoves;
        System.out.println(out);
    }
    private void printCaptures(int x, int y) {
        String out = "T"+aId+": captures ("+ x + ","+ y+ ")";
        System.out.println(out);
    }
    private void printCaptured() {
        String out = "T"+aId+": captured.";
        System.out.println(out);

    }
    private void printRespawn(int x, int y) {
        String out = "T"+aId+": respawns ("+ x + ","+ y+ ")";
        System.out.println(out);
    }

    public void printPosition(){
        String out;
        if (aCurrentTile != null) {
            out = "T" + aId + " current position is  (" + aCurrentTile.aPosX + "," + aCurrentTile.aPosY + ")";
        } else {
            out = "T" + aId + " is off the table. ";
        }
        System.out.println(out);
    }
    private void printSpawn(int x, int y) {
        System.out.println("T"+aId+": spawned on ("+ x + ","+ y+ ")" );
    }

    public synchronized void revive() {
        aAlive = true;
        aCanMove = true;
    }

    public synchronized void capture() {
        // Indicating you're off the board.
        aCurrentTile = null;
        aAlive = false;
        aCanBeCapt = false;
        aCanMove = true;
        printCaptured();
        notify();
    }

    public synchronized void canBeCapt(){
        aCanBeCapt = true;
    }

    public synchronized void cannotBeCapt() throws InterruptedException {
        while (!aCanMove){
            wait();
        }
        aCanBeCapt = false;
    }


    /**
     * Sets the checker to not be able to be captured. We make a guess as to where the checker is. If the checker has
     * moved, the locking function fails as the checker may have moved. The checker may have also been locked by another
     * before us. Thus we can fail if we are trying to lock the chip after.
     * @param pCurrentTileGuess Current tile guess as to where the checker is.
     * @return A boolean whether the calling object has locked the checker.
     */
    public synchronized boolean lockChecker(Tile pCurrentTileGuess) {
        if (aCanBeCapt && pCurrentTileGuess == aCurrentTile){
            // System.out.println(output+  " locking attempted from Thread-" +Thread.currentThread().getId());
            // The object can be captured. Must lock it into place and not allow it to move.
            aCanBeCapt = false;
            aCanMove = false;
            return true;
        }

        // System.out.println(output + " failed to lock checker on (" + aCurrentTile.aPosX +"," +aCurrentTile.aPosY+")");
        return false;
    }

    public synchronized void unlockChecker() {
        aCanBeCapt = true;
        aCanMove = true;
        // Wake up the checker if it was waiting to be jumped.
        notify();
        // System.out.println(output+  " has been released. Can move and can be jumped.");
    }

    /**
     * Gets the alive status of the checker object.
     * @return the alive status of the checker
     */
    public synchronized boolean isAlive() throws InterruptedException {
        while (!aCanMove){
            // You can't move yet someone is holding you!
            // System.out.println(outputLines+outputLines+output+ " waiting to know if I am alive! ");
            wait();
        }

        return aAlive;
    }

    /**
     * One atomic move operation if it is free. the move is provided since we may be able to capture. Given a move, if
     * tile is not free, instead of returning and giving up the lock, we try to capture.
     * @param x width position
     * @param y height position
     * @return a status 0 if successful, 1 if the tile is taken, 2 if
     * you've been captured, 3 if you can be c and -1 if it is a bad location.
     */
    public synchronized int moveChecker(int x, int y) throws InterruptedException {
        // If you can move, you're clearly not captured. If you cannot move, then someone may be in the process
        // of capturing you. Therefore, wait until they're done.
        while (!aCanMove && aAlive){
            // System.out.println("Someone needs to finish their move!!! =============================== T" + aId + " I can't move :(");
            wait();
        }

        if (!aAlive ){
            // System.out.println(output + " You're dead failing!!!");
            return 1;
        }
        if(x < 0 || x > 7 || y < 0 || y > 7){

            //System.out.println(output + " Bad move");
            return -1;
        }

        // Get the next tile
        // System.out.println(outputLines + outputLines + output + " locking tile and TRYING TO MOVE to (" +x + "," +y + ")");
        Tile nextTile = aBoard.moveChecker(aCurrentTile, this, x, y);
        if (nextTile != null) {
            aCurrentTile = nextTile;
            printMove(x,y);
            return 0;
        }

        //System.out.println(output + " Made no move :(");
        return 2;
    }

    public boolean captureMove(int x, int y, Move pMove) throws InterruptedException{


        // System.out.println(outputLines + output+" trying to lock checker on (" + x + "," + y +")!");
        if (!aBoard.getTile(x,y).lockCheckerOn()){
            // Failed to lock. Checker either has moved OR has been captured by another player.
            // System.out.println(outputLines + output + " failed to capture. Trying to move onto that spot!");
            return false;
        }

        // Fixed the deadlocking issue, when 2 threads trying to lock each other.
        synchronized (this) {
            // System.out.println(outputLines + output+" trying to capture checker on (" + x + "," + y +")!");
            if (!aCanMove){
                // You fail.
                aBoard.getTile(x,y).unlockCheckerOn();
                return false;
            }

            // Checker is locked, try to jump over
            int x2= x + pMove.x;
            int y2 = y + pMove.y;
            // recursively call the move function and move the chip.
            // System.out.println(outputLines + output + " trying to capture! 2");
            if (moveChecker(x2,y2) == 0) {
                // You've successfully captured.
                aCaptures++;
                // capture the checker and release its tile and block.
                aBoard.getTile(x,y).captureCheckerOn();
                printCaptures(x,y);
                return true;
            }
            // failed, unlock the checker.
            // System.out.println(outputLines + output + " released! Can move.");
            aBoard.getTile(x,y).unlockCheckerOn();
            return false;
        }
    }

    /**
     *
     */
    public synchronized void remove() throws InterruptedException {
        while (!aCanMove) {
            wait();
        }
        System.out.println("Removing T" +aId + " from board");
        if (aAlive){
            aBoard.removeCheckerFromBoard(aCurrentTile.aPosX, aCurrentTile.aPosY);
        }
    }

    /**
     * The spawn action. Choosing and placing a chip onto the board.
     * @param x width position on the board.
     * @param y height position on the board.
     * @return A boolean of spawn validity
     */
    public synchronized boolean spawnChecker(int x, int y) {
        if (x < 0 || x > 7 || y < 0 || y > 7 ) {
            return false;
        }

        // Get the next tile
        aCurrentTile = aBoard.moveChecker(
                aCurrentTile,
                this,
                x,
                y
        );

        if (aCurrentTile != null) {
            // You're now alive.
            revive();
            aCanBeCapt = false;
            // now spawned.
            return true;
        }
        return false;
    }

    /**
     * Spawning onto a tile on the board and saving our tile.
     */
    public synchronized void spawn() {
        // Put the checker piece in a random place! With proper colour
        int y = ThreadLocalRandom.current().nextInt(8);
        int x = ThreadLocalRandom.current().nextInt(8);
        if (aBoard.getTileColor(x,y) != TileColor.BEIGE){
            // move over by 1
            x = (x+1) % 8;
        }
        int i = 0;
        // Your current tile is set inside the move checker
        while (!spawnChecker(x,y)){
            // need to find a new cell, this one is taken.
            // We want to move, diagonal in one of 4 directions
            // You're on a beige tile, now move along a diagonal to stay on beige!
            int nextTile = ThreadLocalRandom.current().nextInt(100) % 4;
            int nextX, nextY;
            if (nextTile == 0) {
                nextX = 1;
                nextY = 1;
            } else if (nextTile == 1) {
                nextX = -1;
                nextY = 1;
            } else if (nextTile == 2) {
                nextX = -1;
                nextY = -1;
            } else {
                nextX = 1;
                nextY = -1;
            }
            x = (nextX + x) % 8;
            y = (nextY + y) % 8;
            x = x < 0 ? 7 : x;
            y = y < 0 ? 7 : y;
        }
    }

    /**
     * Respawn the piece.
     */
    public synchronized void respawn() {
        // Spawn yourself
        spawn();
        printRespawn(aCurrentTile.aPosX, aCurrentTile.aPosY);
    }
}

class Move {
    int x, y;
    public Move(int x, int y) {
        this.x = x;
        this.y = y;
    }
}


class CheckerController implements Runnable {
    // TODO: make these private
    public Checker aChecker;
    public Thread aThread;
    // M from the assignment sleep specifications
    private int m;

    public CheckerController(Checker pChecker, int k){
        aChecker = pChecker;
        aThread = new Thread(this);
        m = k;
    }

    public void start(){
        aThread.start();
    }

    public void join(){
        try {
            aThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted Exception on join for T" + aChecker.aId);
            System.out.println("Interrupted! Done4");
        }
    }

    /**
     * Creates a new Move given by the direction.
     * @param nextMoveDir
     * @return Returns a move.
     */
    private Move getNextTile(int nextMoveDir) {
        Move aMove ;
        if (nextMoveDir == 0) {
            aMove = new Move(1,1);
        } else if (nextMoveDir == 1) {
            aMove = new Move(-1,1);
        } else if (nextMoveDir == 2) {
            aMove = new Move(-1,-1);
        } else {
            aMove = new Move(1,-1);
        }
        return aMove;
    }

    @Override
    public void run() {
        System.out.println("T" + aChecker.aId + " is on Thread-" + Thread.currentThread().getId());
        while (aChecker.aNumMoves < aChecker.aMaxMoves) {
            int nextDir = ThreadLocalRandom.current().nextInt(100) % 4;
            Move nextMove = getNextTile(nextDir);
            int i = 0;
            while (i < 4) {
                // Move returns a status about the move.
                int x = aChecker.aCurrentTile.aPosX + nextMove.x;
                int y = aChecker.aCurrentTile.aPosY + nextMove.y;
                try {
                    int moveStatus = aChecker.moveChecker(x, y);
                    if (moveStatus == 0) {
                        aChecker.aNumMoves++;
                        break;
                    } else if (moveStatus == 2 && aChecker.captureMove(x,y, nextMove)){
                        // made a kill!
                        aChecker.aNumMoves++;
                    } else {
                        // You've been captured :(
                        // System.out.println(aChecker.output + " making new move after bad move");
                        i++;
                        nextMove = getNextTile((nextDir + i) % 4);
                    }
                } catch (InterruptedException e) {
                    // e.printStackTrace();
                    System.out.println("Interrupted! Done1");
                    // TODO: take out
                    return;
                }
            }

            try {
                // Can only be captured when you're sleeping.
                aChecker.canBeCapt();
                Thread.sleep(m);
                // Now you can't be captured anymore.
                aChecker.cannotBeCapt();
                // If you aren't dead then you can simply go on with life.
                if (!aChecker.isAlive()) {
                    // You're dead, sleep!
                    int timeSleep = ThreadLocalRandom.current().nextInt(2, 5);
                    Thread.sleep(timeSleep * m);
                    // aChecker.respawn();
                    break;
                }
            } catch (InterruptedException e) {
                //e.printStackTrace();
                System.out.println("Interrupted! Done2");
                // TODO: take out
                return;
            }

        }
        // Remove yourself from the board. Need to wait until you can move to do this.

        try {
            aChecker.remove();
        } catch (InterruptedException e) {
            e.printStackTrace();

            System.out.println("Interrupted! Done3");
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
        Checker [] checkers = new Checker[t];
        CheckerController [] checkerControllers = new CheckerController[t];

        // Spawn the checker objects, per requirements.
        for (int i = 0; i < t; i++){
            checkers[i] = new Checker(i+1, n, tBoard);
        }

        // assign a thread to each checker, per requirements.
        for (int i = 0; i<t; i++){
            checkerControllers[i] = new CheckerController(checkers[i], k);
        }

        System.out.print(tBoard.toString());
        // start the threads
        for (int i = 0; i<t; i++){
            checkerControllers[i].start();
        }

        long start = System.currentTimeMillis();
        long last = System.currentTimeMillis();
        System.out.println("Time passed: " + (last-start));
        while (last - start < 15000){
            last = System.currentTimeMillis();
            try {
                System.out.println("Time passed: " + (last-start));
                Thread.sleep(1000);
            } catch (InterruptedException e){
                for (int i = 0; i<t; i++){
                    checkerControllers[i].aThread.interrupt();
                }
            }
        }

        // join the threads.
        for (int i = 0; i<t; i++){
            checkerControllers[i].aChecker.printPosition();
        }

        System.out.print(tBoard.toString());

        // join the threads.
        for (int i = 0; i<t; i++){
            checkerControllers[i].join();
        }

        for (int i = 0; i<t; i++){
            System.out.println("T" + checkers[i].aId + " had " + checkers[i].aCaptures + " captures.");
        }
        // Done.
    }
}