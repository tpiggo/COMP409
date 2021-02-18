package assignment2;
import java.util.concurrent.ThreadLocalRandom;

/**
 * TODO: COMMENT
 */

enum TileColor{
    BROWN, BEIGE;
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
     * @param x
     * @param y
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
                aString+= aBoard[y][x].getColor() + " (" + x + ", " + y +"): "+ !aBoard[y][x].isFree();
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
    private Checker checkerOn = null;
    public int aPosX, aPosY;

    public Tile(TileColor color, int x, int y) {
        // colour is 1 or 0, indicating the color;
        aColor = color;
        aPosX = x;
        aPosY = y;
    }

    public TileColor getColor() {
        return aColor;
    }

    // Place a checker on the tile. It should both be free but not available at this point.
    public synchronized Tile setCheckerOn(Checker checker, Tile oldTile) {
        if (!aFree){
            return null;
        }
        aFree = false;
        checkerOn = checker;

        // Free your previous tile.
        if (oldTile != null){
            oldTile.setFree();
        }
        return this;
    }

    public synchronized void removeChecker() {
        if (!aFree){
            aFree = true;
            checkerOn = null;
        }
    }

    // Release the tile!
    public synchronized void setFree() {
        aFree = true;
        checkerOn = null;
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

    public synchronized boolean lockCheckerOn(){
        // Guessing the checker is still here.
        return checkerOn.lockChecker(this);
    }
    public synchronized Tile get(){
        return this;
    }
}

class Checker {
    private boolean aAlive = false;
    private boolean aCanBeCapt = false;
    private boolean aCanMove = false;
    public int aId;
    public int aNumMoves;
    public int aCaptures;
    public int aMaxMoves;
    public Board aBoard;
    public Tile aCurrentTile = null;

    public Checker(int id, int maxMoves, Board pBoard) {
        aId = id;
        aMaxMoves = maxMoves;
        aBoard = pBoard;
        spawn();
        // You have been placed onto a good tile.
        printSpawn(aCurrentTile.aPosX, aCurrentTile.aPosY);
    }

    private void printMove(int x, int y) {
        String out = "T"+aId+": moves to (" + x + ","+  y+ ")";
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

    private void printSpawn(int x, int y) {
        System.out.println("T"+aId+": spawned on ("+ x + ","+ y+ ")");
    }

    /**
     * One atomic move operation if it is free. the move is provided since we may be able to capture. Given a move, if
     * tile is not free, instead of returning and giving up the lock, we try to capture.
     * @param x width position
     * @param y height position
     * @param captureMove boolean if we are capturing. Since we do not want to perform 2 captures, we can capture on
     *                    the first move, cannot on the second.
     * @return a status 0 if successful, 1 if the tile is taken, 2 if
     * you've been captured, 3 if you can be c and -1 if it is a bad location.
     */
    public synchronized boolean moveChecker(int x, int y, boolean captureMove, Move pMove) throws InterruptedException {
        while (!aCanMove){
            wait();
            if (!aAlive){
                return false;
            }
        }
        if (!aAlive) {
            return false;
        }
        // You are now in the move phase. You now cannot be stopped.
        aCanBeCapt = false;

        if (x < 0 || x > 7 || y < 0 || y > 7 ) {
            return false;
        }

        // Get the next tile
        Tile nextTile = aBoard.moveChecker(
               aCurrentTile,
                this,
                x,
                y
        );

        if (nextTile != null) {
            aCurrentTile = nextTile;
            printMove(x,y);
            return true;
        }
        // We want to perform an atomic capture here!
        if (captureMove){
            // lock the checker on the other tile.
            if (!aBoard.getTile(x,y).lockCheckerOn()){
                // Failed to lock, fail.
                return false;
            }
            // Checker is locked, try to jump over
            int x2= x + pMove.x;
            int y2 = y + pMove.y;
            // recursively call the move function and move the chip.
            if (moveChecker(x2,y2, false, null)) {
                // You've successfully captured.
                aCaptures++;
                // capture the checker and release its tile and block.

            }

        }
        return false;
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
    }

    public synchronized void capturable() {
        aCanBeCapt = true;
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
            // The object can be captured. Must lock it into place and not allow it to move.
            aCanBeCapt = false;
            aCanMove = false;
            return true;
        }
        return false;
    }

    /**
     * Gets the alive status of the checker object.
     * @return the alive status of the checker
     */
    public synchronized boolean isAlive() {
        return aAlive;
    }

    /**
     *
     */
    public synchronized void remove() {
        System.out.println("Removing T" +aId + " from board");
        aBoard.removeCheckerFromBoard(aCurrentTile.aPosX, aCurrentTile.aPosY);
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
            // now spawned.
            return true;
        }

        return false;
    }

    /**
     * Respawn the piece.
     */
    public synchronized void respawn() {
        // Spawn yourself
        spawn();
        printRespawn(aCurrentTile.aPosX, aCurrentTile.aPosY);
    }

    /**
     * We are spawning a tile into the board keeping track of the current tile we are on.
     * @return Tile which we have placed the checker on.
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
            int nextX = 0, nextY = 0;
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
}

class Move {
    int x, y;
    public Move(int x, int y) {
        this.x = x;
        this.y = y;
    }
}


class CheckerController implements Runnable {
    private Checker aChecker;
    private Thread aThread;
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
        while (aChecker.aNumMoves < aChecker.aMaxMoves) {
            // calculate next move
            if (!aChecker.isAlive()) {
                // You're dead, sleep!
                int timeSleep = ThreadLocalRandom.current().nextInt(2, 5);
                try {
                    Thread.sleep(timeSleep * m);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // respawn
                aChecker.respawn();
            }

            int nextDir = ThreadLocalRandom.current().nextInt(100) % 4;
            Move nextMove = getNextTile(nextDir);
            int i = 0;
            while (i < 4) {
                // loop to test all diagonals.
                int x = aChecker.aCurrentTile.aPosX + nextMove.x;
                int y = aChecker.aCurrentTile.aPosY + nextMove.y;
                // Move returns a status about the move.
                try {
                    if (aChecker.moveChecker(x, y, true, nextMove)) {
                        aChecker.aNumMoves++;
                        break;
                    } else {
                        // You've been captured :(
                        if (!aChecker.isAlive()){
                            break;
                        }
                        i++;
                        nextMove = getNextTile((nextDir + i) % 4);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            // You've failed to find a good move. sleep and let others move
            try {
                // Set can be captured
                aChecker.capturable();
                // sleep for a few miliseconds in order to let other work since you cannot move.
                Thread.sleep(m);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Remove yourself from the board.
        aChecker.remove();
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
        // join the threads.
        for (int i = 0; i<t; i++){
            checkerControllers[i].join();
        }
        // Done.
    }
}