package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class EcDemo {

    private static double ms(long t0, long t1) { return (t1 - t0) / 1_000_000.0; }
    private static void printMs(String k, double v) { System.out.printf("%-10s: %.3f ms%n", k, v); }

    public static void main(String[] args) {
        int numPrimes = 3, qBit = 512;
        String RID = "RID-ALICE";
        int[] attrs = {0,2};
        byte[] mu = "hello-ehealth".getBytes(StandardCharsets.UTF_8);

        long T0 = System.nanoTime(), t0, t1;

        // GSetup
        t0 = System.nanoTime();
        EcPP pp = new EcPP(numPrimes, qBit);
        t1 = System.nanoTime();
        double tG = ms(t0,t1);

        // ASetup
        t0 = System.nanoTime();
        Map<Integer, EcAuthority> AAs = new HashMap<>();
        for (int a : attrs) AAs.put(a, new EcAuthority(pp, a));
        t1 = System.nanoTime();
        double tA = ms(t0,t1);

        // KGen
        t0 = System.nanoTime();
        EcUser u = new EcUser(RID);
        for (int a : attrs) u.receiveKey(a, AAs.get(a).keyGen(pp, RID));
        t1 = System.nanoTime();
        double tKG = ms(t0,t1);

        // TKGen
        t0 = System.nanoTime();
        Map<Integer, EcUser.TukPair> tuks = u.tkGen(pp, Arrays.asList(0,2));
        t1 = System.nanoTime();
        double tTK = ms(t0,t1);

        // LSSS
        EcMatrix Gamma = new EcMatrix(2,2);
        Gamma.set(0,0, BigInteger.ONE); Gamma.set(0,1, BigInteger.ONE);
        Gamma.set(1,0, BigInteger.ONE); Gamma.set(1,1, BigInteger.valueOf(2));
        HashMap<Integer,Integer> rho = new HashMap<>(); rho.put(0,0); rho.put(1,2);
        EcPolicy Arho = new EcPolicy(Gamma, rho);

        // Enc
        t0 = System.nanoTime();
        EcCiphertext ct = EcEncTransDec.enc(pp, AAs, Arho, Gamma, mu);
        t1 = System.nanoTime();
        double tE = ms(t0,t1);

        // Trans
        EcMatrix c = new EcMatrix(1,2);
        c.set(0,0, BigInteger.valueOf(2));
        c.set(0,1, pp.N.subtract(BigInteger.ONE));
        t0 = System.nanoTime();
        EcPDCT pd = EcEncTransDec.trans(pp, RID, tuks, ct, c);
        t1 = System.nanoTime();
        double tT = ms(t0,t1);

        //  T2
        Map<Integer, Element> T2_byAttr = new HashMap<>();
        for (Map.Entry<Integer, Element> e : pd.T2_byRow.entrySet()) {
            int row = e.getKey(); int attr = Arho.rho(row);
            Element prev = T2_byAttr.get(attr);
            T2_byAttr.put(attr, prev==null ? e.getValue() : prev.mul(e.getValue()).getImmutable());
        }

        // Dec
        t0 = System.nanoTime();
        byte[] muDec = EcEncTransDec.dec2(pp, pd.C0, pd.T1, T2_byAttr, u.zMap);
        t1 = System.nanoTime();
        double tD = ms(t0,t1);

        long T1 = System.nanoTime();
        double T = ms(T0,T1);

        System.out.println("µ(dec)   = " + new String(muDec, StandardCharsets.UTF_8));
        System.out.println("---- per-stage ----");
        printMs("GSetup", tG);
        printMs("ASetup", tA);
        printMs("KGen",   tKG);
        printMs("TKGen",  tTK);
        printMs("Enc",    tE);
        printMs("Trans",  tT);
        printMs("Dec",    tD);
        System.out.println("-------------------");
        printMs("Total",  T);
    }
}
