//TODO: REMOVE THIS
package assignment2;

import java.util.concurrent.ThreadLocalRandom;


/**
 * TODO: COMMENT
 */

enum TileColor{
    BROWN, BEIGE
}


class Move {
    int x, y;
    public Move(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

class Board{
    public Tile [][] aBoard;
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

    public void print(){
        for (int y = 0; y < aHeight; y++) {
            for (int x = 0; x < aWidth; x++){
                // Setting to brown
                if (aBoard[y][x].aColour == TileColor.BEIGE) continue;
                if (aBoard[y][x].aChecker != null){
                    System.out.println(aBoard[y][x].aChecker.id + " is on " + aBoard[y][x].aColour + "("+ x + "," + y + ")");
                } else {
                    System.out.println("No checker is on ("+ x + "," + y + ")");
                }
            }
        }
    }
}


class Tile {
    TileColor aColour;
    int posX, posY;
    Checker aChecker;
    int lockingId;
    boolean locked = false;

    public Tile(TileColor colour, int x, int y){
        aColour = colour;
        posX = x;
        posY = y;
    }

    public synchronized Tile setChecker(Checker pChecker, boolean isRespawning) throws InterruptedException {
        // someones locking
        while (locked)
            wait();

        if (aChecker == null){
            aChecker = pChecker;

            if (isRespawning)
                pChecker.printRespawn(this);
            else
                pChecker.printSpawn(this);
            return this;
        }

        return null;
    }

    public synchronized boolean removeChecker(Checker pChecker) throws InterruptedException {
        // Let whomever locked you finish their move before removing.
        while (locked)
            wait();


        if (aChecker == pChecker) {
            System.out.println("Removing T" +aChecker.id + " from " + toString());
            aChecker = null;
            return true;
        }
        return false;
    }

    public synchronized void free(){
        aChecker = null;
        locked = false;
        lockingId = 0;
        notify();
    }

    public synchronized Tile moveChecker(Checker pChecker, Tile oldTile){
        // Not a state we can enter. But used to check
        if (locked && pChecker.id != lockingId)
            System.out.println("------------------------------T" + pChecker.id + " is moving onto a tile locked by T" + lockingId);
        else if (aChecker != null)
            System.out.println("------------------------------T" + aChecker.id + " is on the tile");
        aChecker = pChecker;
        locked = false;
        lockingId = 0;
        pChecker.printMove(this);
        oldTile.free();
        notify();
        return this;
    }

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

    public synchronized boolean testAndCaptureAndMove(Tile capturedTile, Checker pChecker, Tile oldTile){
        if (locked || aChecker != null){
            return false;
        }
        // Not a state we can enter. But used to check
        if (locked && pChecker.id != lockingId)
            System.out.println("------------------------------T" + pChecker.id + " is moving onto a tile locked by T" + lockingId);
        else if (aChecker != null)
            System.out.println("------------------------------T" + aChecker.id + " is on the tile");
        capturedTile.aChecker.printCaptured();
        pChecker.printCapture(capturedTile);
        capturedTile.free();
        moveChecker(pChecker, oldTile);
        return true;
    }

    public synchronized int testAndLock(int id){
        if (locked){
            return 2;
        }

        int lockStat = 0;
        if (aChecker != null){
            lockStat = 1;
        }
        locked = true;
        lockingId = id;
        return lockStat;
    }

    public synchronized void unlock(){
        locked = false;
        lockingId = 0;
        notify();
    }

    public synchronized boolean checkerIsOn(Checker pChecker){
        if (pChecker != aChecker){
            return false;
        }
        return true;
    }

    public String toString(){
        return "(" + posX + ", " + (7-posY) + ")";
    }
}

class Checker {
    Tile currentTile;
    int id, numCaptures;
    Board aBoard;

    public Checker(Board pBoard, int pId) {
        id = pId;
        aBoard = pBoard;
    }

    public void spawn() throws InterruptedException {
        genericSpawn(false);
    }

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

            currentTile = aBoard.aBoard[y][x].setChecker(this, isRespawn);
            if (currentTile != null){
                spawned = true;
            }
        }
    }

    public void respawn() throws InterruptedException {
        genericSpawn(true);
    }

    protected void printCaptured() {
        String out = "T"+id+": captured" ;
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

    public void remove() throws InterruptedException {
        if (!currentTile.removeChecker(this)){
            System.out.println("T14: Not on the board, thus already removed!");
        }
    }

    public boolean isAlive(){
        return currentTile.checkerIsOn(this);
    }

    private boolean testAndCapture(Tile capturedTile, int x, int y){
        return aBoard.aBoard[y][x].testAndCaptureAndMove(capturedTile, this, currentTile);
    }

    public Move getMove(int dir){
        return switch (dir) {
            case 1 -> new Move(1, 1);
            case 2 -> new Move(1, -1);
            case 3 -> new Move(-1, -1);
            default -> new Move(-1, 1);
        };
    }

    public boolean moveChecker() throws InterruptedException {
        int i = 0;
        int moveDir = ThreadLocalRandom.current().nextInt(0, 4);
        while (i < 4){
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
                        currentTile = aBoard.aBoard[y2][x2];
                        numCaptures++;
                        return true;
                    } else {
                        aBoard.aBoard[y][x].unlock();
                    }
                } else if (locked == 0){
                    currentTile = aBoard.aBoard[y][x].moveChecker(this, currentTile);;
                    return true;
                }
            }

            // unlock and calculate the next move
            currentTile.unlock();
            i++;
            moveDir = (moveDir + i) % 4;
        }
        return false;
    }
}

class CheckerController implements Runnable {
    Thread aThread;
    Checker aChecker;
    int numMoves = 0;
    int k;
    long n;

    public CheckerController(Checker pChecker, int pK, int pN){
        aChecker = pChecker;
        aThread = new Thread(this);
        k = pK;
        n = pN;
    }
    public void start(){
        aThread.start();
    }
    public void join() throws InterruptedException {
        aThread.join();
    }

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
