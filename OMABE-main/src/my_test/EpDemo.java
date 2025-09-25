package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EpDemo {


    private static String ms(long nanos) {
        return String.format("%.3f ms", nanos / 1_000_000.0);
    }

    private static void printRow(String label, long nanos) {
        System.out.println(String.format("%-7s : %s", label, ms(nanos)));
    }

    private static void warmup(EpPP pp) {
        Element g1 = pp.g1, g2 = pp.g2;
        for (int i = 0; i < 8; i++) {
            BigInteger e = pp.randZqBI();
            pp.g1PP.powZn(pp.Zq.newElement(e));
            pp.g2PP.powZn(pp.Zq.newElement(e));
        }
        pp.pairing.pairing(g1, g2);
    }

    public static void main(String[] args) {
        long T0 = System.nanoTime();

        // === GSetup ===
        long t_gs_s = System.nanoTime();
        EpPP pp = new EpPP(256, 1536);
        warmup(pp);
        long t_gs_e = System.nanoTime();

        // === ASetup (two attributes 0,2) ===
        long t_as_s = System.nanoTime();
        Map<Integer, EpAuthority> AAs = new HashMap<>();
        AAs.put(0, new EpAuthority(pp, 0));
        AAs.put(2, new EpAuthority(pp, 2));
        long t_as_e = System.nanoTime();

        // Policy: Γ = [[1,1],[1,2]], rho: row0->0, row1->2 (AND)
        BigInteger[][] Gamma = new BigInteger[][]{
                { BigInteger.ONE, BigInteger.ONE },
                { BigInteger.ONE, BigInteger.valueOf(2) }
        };
        Map<Integer,Integer> rho = new HashMap<>();
        rho.put(0, 0); rho.put(1, 2);
        EpPolicy policy = new EpPolicy(Gamma, rho);

        // === KGen ===
        long t_kg_s = System.nanoTime();
        EpUser u = new EpUser("RID-ALICE");
        u.receiveFromAuthority(pp, 0, AAs.get(0));
        u.receiveFromAuthority(pp, 2, AAs.get(2));
        long t_kg_e = System.nanoTime();

        // === TKGen ===
        long t_tk_s = System.nanoTime();
        u.tkGen(pp, Arrays.asList(0,2));
        long t_tk_e = System.nanoTime();

        // === Enc ===
        long t_enc_s = System.nanoTime();
        byte[] mu = "hello-ehealth".getBytes(StandardCharsets.UTF_8);
        EpCiphertext ct = EpEncTransDec.enc(pp, AAs, policy, mu);
        long t_enc_e = System.nanoTime();

        // === Trans ===
        long t_tr_s = System.nanoTime();
        EpPDCT pd = EpEncTransDec.trans(pp, u, AAs, ct);
        long t_tr_e = System.nanoTime();

        // === Dec ===
        long t_dec_s = System.nanoTime();
        byte[] dec = EpEncTransDec.dec(pp, u, pd);
        long t_dec_e = System.nanoTime();

        long T1 = System.nanoTime();

        // ==== Output in the requested format ====
        System.out.println(String.format("µ(dec)%4s= %s", "", new String(dec, StandardCharsets.UTF_8)));
        System.out.println("---- per-stage ----");
        printRow("GSetup", t_gs_e - t_gs_s);
        printRow("ASetup", t_as_e - t_as_s);
        printRow("KGen",   t_kg_e - t_kg_s);
        printRow("TKGen",  t_tk_e - t_tk_s);
        printRow("Enc",    t_enc_e - t_enc_s);
        printRow("Trans",  t_tr_e - t_tr_s);
        printRow("Dec",    t_dec_e - t_dec_s);
        System.out.println("-------------------");
        printRow("Total",  T1 - T0);
    }
}
