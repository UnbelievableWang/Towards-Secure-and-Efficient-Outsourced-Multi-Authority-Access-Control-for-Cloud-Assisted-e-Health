package my_test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class EpUserDemo {


    private static String ms(long nanos) {
        return String.format("%.3f ms", nanos / 1_000_000.0);
    }


    private static void printRow(String label, long nanos) {
        System.out.println(String.format("%-7s : %s", label, ms(nanos)));
    }


    private static BigInteger[][] buildGamma2() {
        BigInteger[][] Gamma = new BigInteger[2][2];
        Gamma[0][0] = BigInteger.ONE;
        Gamma[0][1] = BigInteger.ONE;
        Gamma[1][0] = BigInteger.ONE;
        Gamma[1][1] = BigInteger.valueOf(2);
        return Gamma;
    }

    public static void main(String[] args) {

        final int ATTR_DOMAIN_SIZE = 2;


        int[] userCounts = new int[]{2, 100, 500, 1000};

        for (int N_USERS : userCounts) {
            System.out.println();
            System.out.println("========== ATTR_DOMAIN = 2, NUM_USERS = " + N_USERS + " ==========");

            long T0 = System.nanoTime();


            long t_gs_s = System.nanoTime();

            EpPP pp = new EpPP(256, 1536);
            long t_gs_e = System.nanoTime();

            long t_as_s = System.nanoTime();
            Map<Integer, EpAuthority> AAs = new HashMap<>();
            for (int a = 0; a < ATTR_DOMAIN_SIZE; a++) {
                AAs.put(a, new EpAuthority(pp, a));
            }
            long t_as_e = System.nanoTime();

            BigInteger[][] Gamma = buildGamma2();
            Map<Integer, Integer> rho = new HashMap<>();
            rho.put(0, 0); 
            rho.put(1, 1); 
            EpPolicy policy = new EpPolicy(Gamma, rho);

            byte[] mu = "hello-ehealth".getBytes(StandardCharsets.UTF_8);
            long t_enc_s = System.nanoTime();
            EpCiphertext ct = EpEncTransDec.enc(pp, AAs, policy, mu);
            long t_enc_e = System.nanoTime();

            long sum_kg = 0L;
            long sum_tk = 0L;
            long sum_tr = 0L;
            long sum_dec = 0L;

            byte[] lastDec = null;  

            for (int i = 0; i < N_USERS; i++) {
                String rid = "RID-USER-" + i;
                EpUser u = new EpUser(rid);

   
                long t_kg_s = System.nanoTime();
                for (int a = 0; a < ATTR_DOMAIN_SIZE; a++) {
                    u.receiveFromAuthority(pp, a, AAs.get(a));
                }
                long t_kg_e = System.nanoTime();
                sum_kg += (t_kg_e - t_kg_s);

    
                long t_tk_s = System.nanoTime();
                List<Integer> tkAttrs = Arrays.asList(0, 1);
                u.tkGen(pp, tkAttrs);
                long t_tk_e = System.nanoTime();
                sum_tk += (t_tk_e - t_tk_s);

      
                long t_tr_s = System.nanoTime();
                EpPDCT pd = EpEncTransDec.trans(pp, u, AAs, ct);
                long t_tr_e = System.nanoTime();
                sum_tr += (t_tr_e - t_tr_s);

   
                long t_dec_s = System.nanoTime();
                byte[] dec = EpEncTransDec.dec(pp, u, pd);
                long t_dec_e = System.nanoTime();
                sum_dec += (t_dec_e - t_dec_s);

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
