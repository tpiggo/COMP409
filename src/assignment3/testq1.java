package assignment3;

public class testq1 {
    public static void main(String [] args) throws Exception {
        // String a = "031772238a0a60621391a1338114a24951133636.672336320";
        // String a = "89443852a1329a955.11.918042881481.4.64972a8572a809";
        // String a = "203120.a15714..479a4a19.73394.24.0.a44.9966a..0481";
        // String a = "07418460262.72009a8802700382717781799279690a.17a39";
        // String a = "641a7519841066.9829072aa965209137.885350.48762069.4a9205a348161159020955278218829.44684a65562.994.a7";
        // String a = ".8362a353.22aa81a52a697.1a49.528.4363666488403a1762a3548a1.2a48073832a273578765274525789144.947.20.1";
        String a = q1.generateString(200);
        System.out.println("String: " + a);
        System.out.println("length: " + a.length());
        DFAGraph aDFA = new DFAGraph();
        NormalRunnable n = new NormalRunnable(0, a, aDFA);
        NormalRunnable n2 = new NormalRunnable(4, a, aDFA);
        Thread t = new Thread(n);
        Thread t2 = new Thread(n2);
        long start = System.currentTimeMillis();
        t.start();
        try {
            t.join();
            long end = System.currentTimeMillis();
            String b = n.getString();
            System.out.println("String From DFA: " + b);
            System.out.println("Length: " + b.length());
            System.out.println("Time taken: " + (end - start));
            long start2 = System.currentTimeMillis();
            t2.start();
            t2.join();
            long end2 = System.currentTimeMillis();
            String c = n2.getString();
            System.out.println("String From DFA2: " + c);
            System.out.println("Length: " + c.length());
            System.out.println("Time taken2: " + (end2 - start2));
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
