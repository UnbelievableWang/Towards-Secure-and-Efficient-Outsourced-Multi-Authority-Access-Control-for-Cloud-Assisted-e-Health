package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EcUserDemo {

    private static String ms(long nanos) {
        return String.format("%.3f ms", nanos / 1_000_000.0);
    }


    private static void printRow(String label, long nanos) {
        System.out.println(String.format("%-7s : %s", label, ms(nanos)));
    }


    private static EcMatrix buildGamma2() {
        EcMatrix Gamma = new EcMatrix(2, 2);
        Gamma.set(0, 0, BigInteger.ONE);
        Gamma.set(0, 1, BigInteger.ONE);
        Gamma.set(1, 0, BigInteger.ONE);
        Gamma.set(1, 1, BigInteger.valueOf(2));
        return Gamma;
    }


    private static EcMatrix buildC2(EcPP pp) {
        EcMatrix c = new EcMatrix(1, 2);
        c.set(0, 0, BigInteger.valueOf(2));
        c.set(0, 1, pp.N.subtract(BigInteger.ONE)); 
        return c;
    }

    public static void main(String[] args) {

        final int ATTR_DOMAIN_SIZE = 2;         
        int[] userCounts = new int[]{2, 100, 500, 1000};

        int numPrimes = 3;
        int qBit = 512;

        for (int N_USERS : userCounts) {

            System.out.println();
            System.out.println("========== EC | ATTR_DOMAIN = 2, NUM_USERS = " + N_USERS + " ==========");

            long T0 = System.nanoTime();

            long t_gs_s = System.nanoTime();
            EcPP pp = new EcPP(numPrimes, qBit);
            long t_gs_e = System.nanoTime();

   
            long t_as_s = System.nanoTime();
            Map<Integer, EcAuthority> AAs = new HashMap<>();
            for (int a = 0; a < ATTR_DOMAIN_SIZE; a++) {
                AAs.put(a, new EcAuthority(pp, a));
            }
            long t_as_e = System.nanoTime();

 
            EcMatrix Gamma = buildGamma2();
            HashMap<Integer, Integer> rho = new HashMap<>();
            rho.put(0, 0);
            rho.put(1, 1);
            EcPolicy Arho = new EcPolicy(Gamma, rho);


            byte[] mu = "hello-ehealth".getBytes(StandardCharsets.UTF_8);
            long t_enc_s = System.nanoTime();
            EcCiphertext ct = EcEncTransDec.enc(pp, AAs, Arho, Gamma, mu);
            long t_enc_e = System.nanoTime();


            long sum_kg = 0L;
            long sum_tk = 0L;
            long sum_tr = 0L;
            long sum_dec = 0L;

            byte[] lastDec = null;

            for (int i = 0; i < N_USERS; i++) {

                String RID = "RID-USER-" + i;
                EcUser u = new EcUser(RID);


                long t_kg_s = System.nanoTime();
                for (int a = 0; a < ATTR_DOMAIN_SIZE; a++) {
                    u.receiveKey(a, AAs.get(a).keyGen(pp, RID));
                }
                long t_kg_e = System.nanoTime();
                sum_kg += (t_kg_e - t_kg_s);

 
                long t_tk_s = System.nanoTime();
                List<Integer> tkAttrs = Arrays.asList(0, 1);
                Map<Integer, EcUser.TukPair> tuks = u.tkGen(pp, tkAttrs);
                long t_tk_e = System.nanoTime();
                sum_tk += (t_tk_e - t_tk_s);

     
                long t_tr_s = System.nanoTime();
                EcMatrix c = buildC2(pp);
                EcPDCT pd = EcEncTransDec.trans(pp, RID, tuks, ct, c);
                long t_tr_e = System.nanoTime();
                sum_tr += (t_tr_e - t_tr_s);

         
                Map<Integer, Element> T2_byAttr = new HashMap<>();
                for (Map.Entry<Integer, Element> e : pd.T2_byRow.entrySet()) {
                    int row = e.getKey();
                    int attr = Arho.rho(row);
                    Element prev = T2_byAttr.get(attr);
                    T2_byAttr.put(attr,
                            prev == null ? e.getValue() : prev.mul(e.getValue()).getImmutable());
                }

        
                long t_dec_s2 = System.nanoTime();
                byte[] dec = EcEncTransDec.dec2(pp, pd.C0, pd.T1, T2_byAttr, u.zMap);
                long t_dec_e2 = System.nanoTime();
                sum_dec += (t_dec_e2 - t_dec_s2);

                lastDec = dec;
            }

            long T1 = System.nanoTime();

            System.out.println(
                    String.format("µ(dec)%4s= %s", "",
                            lastDec == null ? "<null>" : new String(lastDec, StandardCharsets.UTF_8))
            );
            System.out.println("---- per-stage (TOTAL over all users) ----");
            printRow("GSetup", t_gs_e - t_gs_s);
            printRow("ASetup", t_as_e - t_as_s);
            printRow("Enc",    t_enc_e - t_enc_s);
            printRow("KGen",   sum_kg);
            printRow("TKGen",  sum_tk);
            printRow("Trans",  sum_tr);
            printRow("Dec",    sum_dec);
            System.out.println("-------------------");
            printRow("Total",  T1 - T0);
        }
    }
}
