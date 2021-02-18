package assignment1;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * Name: Timothy Piggott
 * StudentID: 260855765
 */

/**
 * Terrain class: Encapulates the information about terrain. We have an atomic integer array in order
 * to protect against data races and have atomic operations for adding height to each cell.
 */
class Terrain{
    public int height;
    public int width;
    public AtomicInteger [][] aTerrain;
    public AtomicInteger cutsLeft;
    public int k;

    public Terrain(int pWidth, int pHeight, int k){
        width = pWidth;
        height = pHeight;
        aTerrain = new AtomicInteger[pWidth][pHeight];
        // Initialize the array
        for (int x = 0; x < aTerrain.length; x++){
            for (int y = 0; y < aTerrain[x].length; y++){
                aTerrain[x][y] = new AtomicInteger(0);
            }
        }
        cutsLeft = new AtomicInteger(k);
        this.k = k;
    }

    public int canFault(){ return cutsLeft.getAndDecrement(); }

    public float findMaxHeight(){
        int max = 0;
        for (AtomicInteger[] atomicIntegers : aTerrain) {
            for (AtomicInteger atomicInteger : atomicIntegers) {
                if (max < atomicInteger.get()) {
                    max = atomicInteger.get();
                }
            }
        }
        return max;
    }

    public float findMinHeight(){
        int min = 10000;
        for (AtomicInteger[] atomicIntegers : aTerrain) {
            for (AtomicInteger atomicInteger : atomicIntegers) {
                if (min > atomicInteger.get()) {
                    min = atomicInteger.get();
                }
            }
        }
        return min;
    }

    public BufferedImage terrainToImage( float minHeight, float maxHeight) {
        BufferedImage aTerrainImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        // Another n^2 operation. :(
        int maxColour = new Color(255,255,255,1).getRGB();
        int minColour = new Color(0,0,0,0).getRGB();
        int colourRange = maxColour - minColour;
        for (int x = 0; x < aTerrain.length; x++){
            for (int y = 0; y < aTerrain[x].length; y++){
                // Colour mapping to heights.
                float augmentColor = (aTerrain[x][y].get()-minHeight)/(maxHeight - minHeight);
                int colour = minColour + (int)((colourRange-1) * augmentColor);
                Color c = new Color(colour);
                aTerrainImage.setRGB(x, y, c.getRGB());
            }
        }
        return aTerrainImage;
    }
}

/**
 * Points are used for encapsulating point data for easy packaging and used in vector operations
 */
class Point{
    public int x, y;
    Point(int pX, int pY) {
        x = pX;
        y = pY;
    }
}

/**
 * TerrainWorkers are runnable workers which are the workers which are adding height to each cell. 
 * My workers add height if the point is to the right of the fault line. Adding random height using
 * the ThreadLocalRandom for extra speed up.
 */
class TerrainWorker implements Runnable{
    private Terrain aTerrain;
    private Thread aThread;
    private int numCuts = 0;
    int randHeight;
    Point aPoint0, aPoint1;

    public TerrainWorker(Terrain pTerrain){
        aTerrain = pTerrain;
        aThread = new Thread(this);
    }

    public int getCuts(){ return numCuts; }

    public void start(){ aThread.start(); }

    public void join() throws InterruptedException { aThread.join(); }

    private void createFaultLine(){
        // Create point 0
        aPoint0 = new Point(
                ThreadLocalRandom.current().nextInt(aTerrain.width),
                ThreadLocalRandom.current().nextInt(aTerrain.height)
        );
        // Create point 1
        aPoint1 = new Point(
                ThreadLocalRandom.current().nextInt(aTerrain.width),
                ThreadLocalRandom.current().nextInt(aTerrain.height)
        );
        //  New random number [0,10], new random height
        randHeight = ThreadLocalRandom.current().nextInt(11);
    }
    @Override
    public void run() {
        // We want to perform a cut.
        while (aTerrain.canFault() > 0){
            numCuts++;
            createFaultLine();
            // Check every point. This is the slowest part of the code
            for (int x = 0; x < aTerrain.width; x++){
                for (int y = 0; y < aTerrain.height; y++){
                    // Calculating position of the point relative to p1 and p0
                    int mLinearity = (aPoint1.x - aPoint0.x)*(y - aPoint0.y) - (x - aPoint0.x)*(aPoint1.y - aPoint0.y);
                    // Augmenting the right side of the line
                    if (mLinearity < 0 || mLinearity == 0){
                        aTerrain.aTerrain[x][y].addAndGet(randHeight);
                    }
                }
            }
        }
    }
}

public class fault {
    // Parameters
    public static int n = 1;
    public static int width;
    public static int height;
    public static int k;

    public static void main(String[] args) {
        try {
            int threads;
            // example of reading/parsing an argument
            if (args.length == 4) {
                threads = Integer.parseInt(args[2]);
                // Get the width and height as well
                width = Integer.parseInt(args[0]);
                height = Integer.parseInt(args[1]);
                k = Integer.parseInt(args[3]);
                // As per assignment requirements
                if (k <= 8 && k < threads && threads > 8) {
                    k = threads + 1;
                } else if (k < threads && threads < 8) {
                    k = 9;
                }
            } else {
                throw new IllegalArgumentException("Not enough arguments.");
            }

            // Create the terrain object for all workers
            Terrain aTerrain = new Terrain(width, height, k);
            TerrainWorker [] terrainWorkers = new TerrainWorker[threads];

            // Spawn worker threads
            for (int i = 0; i < terrainWorkers.length; i++){
                terrainWorkers[i] = new TerrainWorker(aTerrain);
            }

            long startTime = System.currentTimeMillis();
            // Start the threads. Spawning takes non trivial time, thus start them after all spawned.
            for (int i = 0; i < terrainWorkers.length; i++){
                terrainWorkers[i].start();
            }

            // join all threads!
            for (int i = 0; i < terrainWorkers.length; i++){
                terrainWorkers[i].join();
            }

            long totalTime = System.currentTimeMillis() - startTime;

            // Get the number of fault lines
            int numCuts = 0;
            for (int i = 0; i < terrainWorkers.length; i++){
                numCuts += terrainWorkers[i].getCuts();
            }
//            // Make sure the count is okay, sanity check
//            if (numCuts != k){
//                throw new IllegalStateException("Wrong number of fault lines! " + numCuts +" != " + k);
//            }

            System.out.println("Total time for " + numCuts + " fault lines by " + terrainWorkers.length + " threads was " + totalTime);

            // Find the min and max heights
            float minHeight = aTerrain.findMinHeight();
            float maxHeight = aTerrain.findMaxHeight();
            // once we know what size we want we can create an empty image
            BufferedImage bufferedImage = aTerrain.terrainToImage(minHeight, maxHeight);
            // Write out the image
            ImageIO.write(bufferedImage, "png", new File("outputimage.png"));
        } catch (Exception e) {
            System.out.println("ERROR " + e);
            e.printStackTrace();
        }
    }
}