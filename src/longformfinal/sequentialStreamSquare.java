package longformfinal;

import java.util.ArrayList;
import java.util.List;

public class sequentialStreamSquare {
    private static long decrement(long k){ return --k; }
    private static long increment(long k){ return ++k; }
    private static long add(long k, long j){ return k + j;}
    private static void output(long i, long k){ System.out.print(k + ", ");}
    private static void recalc(int n) {
        System.out.print("recal Start: ");
        for (int i = 1; i <= n; i++) {
            long pSum = 0;
            for (int j = 1;  j <= i; j++) {
                pSum += add(j, decrement(j));
            }
            output(i, pSum);
        }
        System.out.println("DONE");
    }

    private static void noRecalc(int n) {
        long partialSum = 0;
        System.out.print("no recalc Start: ");
        for (int i = 1; i <= n; i++) {
            partialSum = add(partialSum, add(decrement(i), i));
            output(i, partialSum);
        }
        System.out.println("DONE");
    }
    public static void main(String [] args){
        List a = new ArrayList();
        a.add("abc");
        a.add(2);
        System.out.println(a.get(0) instanceof Object);
        System.out.println(a.get(1) instanceof Integer);
    }
}
