package assignment3;

public class testq1 {
    public static void main(String [] args) throws Exception {
        String a = "123a12345.654.434.456a432a122.3a12";
        System.out.println("String: " + a);
        System.out.println("length: " + a.length());
        DFAGraph aDFA = new DFAGraph();
        DFATask aTask = new DFATask(aDFA, a, 0);
        NormalRunnable n = new NormalRunnable(0, a, aDFA);
        Thread t = new Thread(n);
        t.start();
        t.join();
        String b = n.getString();
        System.out.println("String From DFA: " + b);
        System.out.println("Length: " + b.length());
        EncodingType[] e = aTask.call();
        for (int i = 0; i < e.length; i++ ){
            System.out.println("String: " + e[i].aString.toString());
            System.out.println("length: " + e[i].aString.length());
        }
    }
}
