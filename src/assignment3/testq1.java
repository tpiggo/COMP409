package assignment3;

public class testq1 {
    public static void main(String [] args) throws Exception {
        // String a = "031772238a0a60621391a1338114a24951133636.672336320";
        // String a = "89443852a1329a955.11.918042881481.4.64972a8572a809";
        // String a = "203120.a15714..479a4a19.73394.24.0.a44.9966a..0481";
        // String a = "07418460262.72009a8802700382717781799279690a.17a39";
        // String a = "1724237254591744264447aa18607143a198.855.139241381.88136299355062981884.1.786083.03412aa44a649343407";
        // String a = "831496339984295.20998822703a969a738800a69613986210";
        String a = q1.generateString(100000);
        // System.out.println("String: " + a);
        System.out.println("length: " + a.length());
        DFAGraph aDFA = new DFAGraph();
        NormalRunnable n = new NormalRunnable(0, a, aDFA);
        NormalRunnable n2 = new NormalRunnable(1, a, aDFA);
        Thread t = new Thread(n);
        Thread t2 = new Thread(n2);
        long start = System.currentTimeMillis();
        t.start();
        try {
            t.join();
            long end = System.currentTimeMillis();
            long start2 = System.currentTimeMillis();
            t2.start();
            t2.join();
            long end2 = System.currentTimeMillis();
            String b = n.getString();
            String c = n2.getString();
            //System.out.println("String From DFA: " + b);
            System.out.println("Length: " + b.length());
            System.out.println("Time taken: " + (end - start));
            // System.out.println("String From DFA2: " + c);
            System.out.println("Length: " + c.length());
            System.out.println("Time taken2: " + (end2 - start2));

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
