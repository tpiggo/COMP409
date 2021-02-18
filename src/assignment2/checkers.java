package assignment2;
import java.util.concurrent.ThreadLocalRandom;

enum TileColor{
    BROWN, BEIGE;
}


class Board{
    private Tile [][] aBoard;
    public int aHeight;
    public int aWidth;
    public Board(int width, int height){
        aWidth = width;
        aHeight = height;
        // Making the board easier to work with. First element is the y value, second element is x value
        aBoard = new Tile[height][width];
        for (int y = 0; y < height; y++){
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
    public Tile moveChecker(Tile oldTile, Checker aChecker, int x, int y){
        return aBoard[y][x].setCheckerOn(aChecker, oldTile);
    }

    public TileColor getTileColor(int x, int y){
        return aBoard[y][x].getColor();
    }
    public Tile getTile(int x, int y){
        return aBoard[y][x].get();
    }

    public boolean getFreeTile(int x, int y){
        if (x < 0 || x > 7 || y < 0 || y > 7 ) {
            return false;
        }
        return aBoard[y][x].getIfFree();
    }

    public void removeCheckerFromBoard(int x, int y){
        aBoard[y][x].removeChecker();
    }
    public String printBoard(){
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

class Tile{
    private TileColor aColor;
    private boolean aFree = true;
    private Checker checkerOn = null;
    public int aPosX, aPosY;

    public Tile(TileColor color, int x, int y){
        // colour is 1 or 0, indicating the color;
        aColor = color;
        aPosX = x;
        aPosY = y;
    }

    public TileColor getColor(){
        return aColor;
    }

    // Place a checker on the tile. It should both be free but not available at this point.
    public synchronized Tile setCheckerOn(Checker checker, Tile oldTile) {
        if (!aFree){
            return null;
        }
        aFree = false;
        checkerOn = checker;
        if (oldTile != null){
            oldTile.setFree();
        }
        return this;
    }

    public synchronized void removeChecker(){
        if (!aFree){
            aFree = true;
            checkerOn = null;
        }
    }

    // Release the tile!
    public synchronized void setFree(){
        aFree = true;
        checkerOn = null;
    }

    public synchronized boolean isFree(){
        return aFree;
    }

    public synchronized boolean getIfFree(){
        // you hold the lock on the tile now.
        if(aFree){
            aFree = false;
            return true;
        }

        return false;
    }

    public synchronized Tile get(){
        return this;
    }
}

class Checker {
    private boolean aAlive = false;
    private boolean aCanBeCapt = false;
    public int aId;
    public int aNumMoves;
    public int aNumKills;
    public int aMaxMoves;
    public Board aBoard;
    public Tile aCurrentTile;

    public Checker(int id, int maxMoves, Board pBoard){
        aId = id;
        aMaxMoves = maxMoves;
        aBoard = pBoard;
        spawn();
    }

    private void printMove(int x, int y){
        String out = "T"+aId+": moves to (" + x + ","+  y+ ")";
        System.out.println(out);
    }
    private void printCaptures(int x, int y){
        String out = "T"+aId+": captures ("+ x + ","+ y+ ")";
        System.out.println(out);
    }
    private void printCaptured(){
        String out = "T"+aId+": captured.";
        System.out.println(out);

    }
    private void printRespawn(int x, int y){
        String out = "T"+aId+": respawns ("+ x + ","+ y+ ")";
        System.out.println(out);
    }

    private void printSpawn(int x, int y){
        System.out.println("T"+aId+": spawned on ("+ x + ","+ y+ ")");
    }

    /**
     * One atomic move operation if it is free.
     * @param x width position
     * @param y height position
     * @return a status 0 if successful, 1 if the tile is taken, 2 if
     * you've been captured, 3 if you can be c and -1 if it is a bad location.
     */
    public synchronized int moveChecker(int x, int y){
        if (!aAlive){
            return 2;
        }
        // You are now in the move phase. You now cannot be stopped.
        aCanBeCapt = false;
        if (x < 0 || x > 7 || y < 0 || y > 7 ) {
            return -1;
        }
        // Get the next tile
        Tile nextTile = aBoard.moveChecker(
               aCurrentTile,
                this,
                x,
                y
        );

        if (nextTile != null){
            aCurrentTile = nextTile;
            printMove(x,y);
            return 0;
        }
        // We want to perform an atomic capture here!
        return 1;
    }

    public synchronized void revive() {
        aAlive = true;
    }

    public synchronized void capture(){
        aAlive = false;
    }

    public synchronized void capturable(){
        aCanBeCapt = true;
    }

    public synchronized void notCapturable(){
        aCanBeCapt = true;
    }

    public synchronized boolean isAlive() {
        return aAlive;
    }

    public synchronized void remove(){
        System.out.println("Removing T" +aId + " from board");
        aBoard.removeCheckerFromBoard(aCurrentTile.aPosX, aCurrentTile.aPosY);
    }

    /**
     * Respawn the piece.
     */
    public synchronized void respawn(){
        spawn();
        printRespawn(aCurrentTile.aPosX, aCurrentTile.aPosY);
    }
    /**
     * We are spawning a tile into the board keeping track of the current tile we are on.
     * @return Tile which we have placed the checker on.
     */
    public synchronized void spawn(){
        // Put the checker piece in a random place! With proper colour
        int y = ThreadLocalRandom.current().nextInt(8);
        int x = ThreadLocalRandom.current().nextInt(8);
        if (aBoard.getTileColor(x,y) != TileColor.BEIGE){
            // move over by 1
            x = (x+1) % 8;
        }
        int i = 0;
        // Your current tile is set inside the move checker
        revive();
        while (moveChecker(x,y) != 0){
            // need to find a new cell, this one is taken.
            // We want to move, diagonal in one of 4 directions
            // You're on a beige tile, now move along a diagonal to stay on beige!
            int nextTile = ThreadLocalRandom.current().nextInt(100) % 4;
            int nextX = 0, nextY = 0;
            if (nextTile == 0){
                nextX = 1;
                nextY = 1;
            } else if (nextTile == 1){
                nextX = -1;
                nextY = 1;
            } else if (nextTile == 2){
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
        // You have found a good tile and we are moving ourselves onto the tile.
        printSpawn(x,y);
    }
}

class Point{
    int x, y;
    public Point(int x, int y){
        this.x = x;
        this.y = y;
    }
}


class CheckerController implements Runnable{
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
        try{
            aThread.join();
        } catch (InterruptedException e){
            System.out.println("Interrupted Exception on join for T" + aChecker.aId);
        }
    }

    private Point getNextTile(int nextTile){
        Point aPoint ;
        if (nextTile == 0){
            aPoint = new Point(1,1);
        } else if (nextTile == 1){
            aPoint = new Point(-1,1);
        } else if (nextTile == 2){
            aPoint = new Point(-1,-1);
        } else {
            aPoint = new Point(1,-1);
        }
        return aPoint;
    }

    @Override
    public void run() {
        while (aChecker.aNumMoves < aChecker.aMaxMoves){
            // calculate next move
            if (!aChecker.isAlive()){
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

            int nextTile = ThreadLocalRandom.current().nextInt(100) % 4;
            Point nextMove = getNextTile(nextTile);
            int i = 0;
            while (i < 4){
                // loop to test all diagonals.
                int x = aChecker.aCurrentTile.aPosX + nextMove.x;
                int y = aChecker.aCurrentTile.aPosY + nextMove.y;
                // Move returns a status about the move.
                int moveStatus = aChecker.moveChecker(x, y);
                if (moveStatus == 0){
                    aChecker.aNumMoves++;
                    break;
                } else if (moveStatus == 1){
                    // Want to capture or retry move!
                    i++;
                    nextMove = getNextTile((nextTile + i) % 4);
                } else {
                    i++;
                    nextMove = getNextTile((nextTile + i) % 4);
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
    public static void main(String [] args){
        int t, k, n;
        if (args.length >= 3){
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

        // Spawn the checker objects
        for (int i = 0; i < t; i++){
            checkers[i] = new Checker(i+1, n, tBoard);
        }

        // assign a thread to each checker
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