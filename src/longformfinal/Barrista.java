package longformfinal;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * CREDIT: https://stackoverflow.com/questions/10855045/executing-dependent-tasks-in-parallel-in-java
 */
public class Barrista {

    static class HeatWater implements Callable<String> {
        @Override
        public String call() throws Exception {
            System.out.println("Heating Water");
            Thread.sleep(1000);
            return "hot water";
        }
    }

    static class GrindBeans implements Callable<String> {
        @Override
        public String call() throws Exception {
            System.out.println("Grinding Beans");
            Thread.sleep(2000);
            return "grinded beans";
        }
    }

    static class Brew implements Callable<String> {

        final Future<String> grindedBeans;
        final Future<String> hotWater;

        public Brew(Future<String> grindedBeans, Future<String> hotWater) {
            this.grindedBeans = grindedBeans;
            this.hotWater = hotWater;
        }

        @Override
        public String call() throws Exception
        {
            System.out.println("brewing coffee with " + grindedBeans.get()
                    + " and " + hotWater.get());
            Thread.sleep(1000);
            return "brewed coffee";
        }
    }

    static class FrothMilk implements Callable<String> {

        @Override
        public String call() throws Exception {
            Thread.sleep(1000);
            return "some milk";
        }
    }

    static class Combine implements Callable<String> {

        public Combine(Future<String> frothedMilk, Future<String> brewedCoffee) {
            super();
            this.frothedMilk = frothedMilk;
            this.brewedCoffee = brewedCoffee;
        }

        final Future<String> frothedMilk;
        final Future<String> brewedCoffee;

        @Override
        public String call() throws Exception {
            Thread.sleep(1000);
            System.out.println("Combining " + frothedMilk.get() + " "
                    + brewedCoffee.get());
            return "Final Coffee";
        }

    }

    public static void main(String[] args) {

        ExecutorService executor = Executors.newFixedThreadPool(2);

        FutureTask<String> heatWaterFuture = new FutureTask<String>(new HeatWater());
        FutureTask<String> grindBeans = new FutureTask<String>(new GrindBeans());
        FutureTask<String> brewCoffee = new FutureTask<String>(new Brew(grindBeans, heatWaterFuture));
        FutureTask<String> frothMilk = new FutureTask<String>(new FrothMilk());
        FutureTask<String> combineCoffee = new FutureTask<String>(new Combine(frothMilk, brewCoffee));

        executor.execute(heatWaterFuture);
        executor.execute(grindBeans);
        executor.execute(brewCoffee);
        executor.execute(frothMilk);
        executor.execute(combineCoffee);


        try {

            /**
             *  Warning this code is blocking !!!!!!!
             */
            System.out.println(combineCoffee.get(20, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            System.out.println("20 SECONDS FOR A COFFEE !!!! I am !@#! leaving!!");
            e.printStackTrace();
        } finally{
            executor.shutdown();
        }
    }
}