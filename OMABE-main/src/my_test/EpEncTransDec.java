package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.util.*;

public class EpEncTransDec {

    public static EpCiphertext enc(EpPP pp,
                                   Map<Integer, EpAuthority> AAs,
                                   EpPolicy policy,
                                   byte[] mu) {

        int l = policy.l, n = policy.n;

        // s, V_A, V_B
        BigInteger s = pp.randZqBI();
        BigInteger sNeg = pp.mod(pp.q.subtract(pp.mod(s)));
        BigInteger[][] V_A = new BigInteger[3][n-1];
        BigInteger[][] V_B = new BigInteger[3][n-1];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < n-1; j++) {
                V_A[i][j] = pp.randZqBI();
                V_B[i][j] = pp.randZqBI();
            }

        // C0 = μ ⊕ Ext( ê([[a1]]_1 ⊙ s, 𝓗) )
        BigInteger[] a1_s_exp     = EpMatrix.scalarVecMod(pp, pp.a1, s);
        Element[]    a1_s_vec     = pp.liftVecToG1(a1_s_exp);
        Element      u0           = pp.pairVec(a1_s_vec, pp.Hcal);
        byte[]       keystream    = pp.ext(u0, Math.max(32, mu.length));
        byte[]       C0           = EpPP.xor(mu, keystream);

        // MAexp = [a1*s | V_A], MBexp = [a1*(-s) | V_B]
        BigInteger[] a1_neg_s_exp = EpMatrix.scalarVecMod(pp, pp.a1, sNeg);
        BigInteger[][] MAexp = new BigInteger[3][n];
        BigInteger[][] MBexp = new BigInteger[3][n];
        for (int i = 0; i < 3; i++) {
            MAexp[i][0] = a1_s_exp[i];
            MBexp[i][0] = a1_neg_s_exp[i];
            for (int j = 1; j < n; j++) {
                MAexp[i][j] = V_A[i][j-1];
                MBexp[i][j] = V_B[i][j-1];
            }
        }

        // per-row components
        Element[][] C1A = new Element[l][3];
        Element[][] C1B = new Element[l][3];
        Element[][] C2A = new Element[l][3];
        Element[][] C2B = new Element[l][3];

        for (int x = 0; x < l; x++) {
            BigInteger rAx = pp.randZqBI();
            BigInteger rBx = pp.randZqBI();

            // C1_{X,x} = [[a1]]_1 ⊙ r_{X,x}
            Element[] C1Ax = pp.liftVecToG1(EpMatrix.scalarVecMod(pp, pp.a1, rAx));
            Element[] C1Bx = pp.liftVecToG1(EpMatrix.scalarVecMod(pp, pp.a1, rBx));
            C1A[x] = C1Ax; C1B[x] = C1Bx;

            // J_X = APK_{X,ρ(x)} ⊙ r_{X,x}
            int attr = ctAttr(policy, x);
            EpAuthority aa = AAs.get(attr);
            if (aa == null) throw new IllegalArgumentException("missing AA for attr "+attr);
            Element[] JA = pp.liftVecToG1(EpMatrix.scalarVecMod(pp, aa.APK_A_exp, rAx));
            Element[] JB = pp.liftVecToG1(EpMatrix.scalarVecMod(pp, aa.APK_B_exp, rBx));

            BigInteger[] gammaCol = policy.gammaColFromRow(x);
            Element[] MA_gx = pp.liftVecToG1(EpMatrix.rightActExp(pp, MAexp, gammaCol));
            Element[] MB_gx = pp.liftVecToG1(EpMatrix.rightActExp(pp, MBexp, gammaCol));

            C2A[x] = pp.hadamard(MA_gx, JA, pp.G1);
            C2B[x] = pp.hadamard(MB_gx, JB, pp.G1);
        }

        return new EpCiphertext(policy, C0, C1A, C1B, C2A, C2B);
    }

    private static int ctAttr(EpPolicy policy, int row) { return policy.rho.get(row); }


    public static EpPDCT trans(EpPP pp,
                               EpUser user,
                               Map<Integer, EpAuthority> AAs,
                               EpCiphertext ct) {

        Set<Integer> attrs = new HashSet<>();
        attrs.addAll(user.SKpA.keySet());
        attrs.addAll(user.SKpB.keySet());
        List<Integer> X = ct.policy.rowsForAttrs(attrs);
        if (X.isEmpty()) throw new IllegalArgumentException("no satisfiable rows");

        Map<Integer, BigInteger> cMap = ct.policy.solveRecon(pp, X);

        Element[] Hr = pp.H2.hashVec3(user.RID);
        Element[] Hr_otimes_H = pp.hadamard(Hr, pp.Hcal, pp.G2);

        Element T1 = pp.GT.newOneElement();
        for (Map.Entry<Integer, BigInteger> e : cMap.entrySet()) {
            int x = e.getKey();
            BigInteger cx = pp.mod(e.getValue());
            Element tA = pp.pairVec(ct.C2A[x], Hr_otimes_H);
            Element tB = pp.pairVec(ct.C2B[x], Hr);
            Element prod = tA.mul(tB).getImmutable();
            T1 = T1.mul(prod.pow(cx));
        }
        T1 = T1.getImmutable();

        Element T2 = pp.GT.newOneElement();
        for (Map.Entry<Integer, BigInteger> e : cMap.entrySet()) {
            int x = e.getKey();
            BigInteger cx = pp.mod(e.getValue());
            int attr = ct.policy.rho.get(x);
            Element[] skpA = user.SKpA.get(attr);
            Element[] skpB = user.SKpB.get(attr);
            if (skpA == null || skpB == null) continue;
            Element uA = pp.pairVec(ct.C1A[x], skpA);
            Element uB = pp.pairVec(ct.C1B[x], skpB);
            Element prod = uA.mul(uB).getImmutable();
            BigInteger neg = pp.mod(pp.q.subtract(cx));
            T2 = T2.mul(prod.pow(neg));
        }
        T2 = T2.getImmutable();

        EpPDCT pd = new EpPDCT(ct.C0, T1, T2);
        for (Map.Entry<Integer, BigInteger> e : cMap.entrySet()) {
            pd.cxByRow.put(e.getKey(), pp.GT.newOneElement());
        }
        return pd;
    }

    public static byte[] dec(EpPP pp, EpUser user, EpPDCT pd) {
        BigInteger zR = pp.mod(user.zR);
        Element u = pd.T1.mul(pd.T2.pow(zR)).getImmutable();
        byte[] stream = pp.ext(u, pd.C0.length);
        return EpPP.xor(pd.C0, stream);
    }
}
