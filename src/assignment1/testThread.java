package assignment1;


import java.util.Random;

public class testThread {
    public static void main(String args[]){
        long throughput;
        Random k = new Random();
        Random r = new Random(k.nextInt());
        int divisible = 0;
        int count = 0;
        int secondCount = 0;
        int misses = 0;
        long start = System.currentTimeMillis();
        while (true){
            if (divisible%1000 == 0 && divisible != 0){
                count += secondCount;
                secondCount = 0;
                misses = 0;
            }
            if(Math.random() < 0.4){
                secondCount++;
            } else {
                misses++;
            }

            if (divisible%60000 == 0 && divisible != 0){
                throughput = count/(divisible/1000);
                System.out.println("Final Throughput = " + throughput );
                break;
            }

            try {
                Thread.sleep(10);
                divisible += 10;
            } catch (Exception e){
                System.out.print(e);
                break;
            }
        }
        long end = System.currentTimeMillis();
        System.out.println("DONE! Throughput="+ count/((end-start)/1000));
    }

}
