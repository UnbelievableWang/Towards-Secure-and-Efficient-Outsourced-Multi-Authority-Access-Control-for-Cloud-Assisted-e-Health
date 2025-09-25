package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.util.*;


public class EcEncTransDec {

    /**
     * Enc({APKi}, µ, (Γ,ρ)):
     *   - Sample s in Zr; set vA[0]=s, vB[0]=-s; others random.
     *   - C0 = mu XOR Ext(e(g1^s, h)).
     *   - For each row x, create C1/C2 components as above.
     */
    public static EcCiphertext enc(EcPP pp,
                                   Map<Integer, EcAuthority> authorities,
                                   EcPolicy Arho,
                                   EcMatrix Gamma,
                                   byte[] muBytes) {

        final int l = Arho.l, n = Arho.n;

        // s, vA, vB ∈ Zr
        Element s = pp.randZr();
        Element[] vA = new Element[n], vB = new Element[n];
        vA[0] = s;
        vB[0] = s.duplicate().negate().getImmutable();
        for (int j = 1; j < n; j++) { vA[j] = pp.randZr(); vB[j] = pp.randZr(); }

        // C0
        Element W = pp.e(pp.g1PowZn(s), pp.h);
        byte[] stream = pp.extKey(W, 32);
        if (stream.length < muBytes.length) {
            byte[] ext2 = new byte[muBytes.length];
            for (int i = 0; i < muBytes.length; i++) ext2[i] = stream[i % stream.length];
            stream = ext2;
        }
        byte[] C0 = pp.xorBytes(muBytes, stream);

        // Components
        Element[] C1A = new Element[l], C2A = new Element[l], C1B = new Element[l], C2B = new Element[l];

        for (int x = 0; x < l; x++) {
            // rA_x, rB_x ∈ Zr
            Element rAx = pp.randZr();
            Element rBx = pp.randZr();

            // C1,* = g1^{r}
            C1A[x] = pp.g1PowZn(rAx);
            C1B[x] = pp.g1PowZn(rBx);

            // dotA/dotB ∈ Zr: Γ_x · vA / vB
            Element dotA = pp.Zr.newZeroElement().getImmutable();
            Element dotB = pp.Zr.newZeroElement().getImmutable();
            for (int j = 0; j < n; j++) {
                Element g_xj = pp.toZr(Gamma.get(x, j));
                dotA = dotA.add(g_xj.mul(vA[j])).getImmutable();
                dotB = dotB.add(g_xj.mul(vB[j])).getImmutable();
            }

            // g1^{dot*}
            Element g1dotA = pp.g1PowZn(dotA);
            Element g1dotB = pp.g1PowZn(dotB);

            // attribute for row x
            int iAttr = Arho.rho(x);
            EcAuthority aut = authorities.get(iAttr);

            // APKA/APKB
            Element leftA = aut.APKA_i.powZn(rAx).getImmutable();
            Element leftB = aut.APKB_i.powZn(rBx).getImmutable();

            C2A[x] = leftA.mul(g1dotA).getImmutable();
            C2B[x] = leftB.mul(g1dotB).getImmutable();
        }

        return new EcCiphertext(Arho, C0, C1A, C2A, C1B, C2B);
    }

    /**
     * Trans(RID, TUK, CT_K) -> PDCT
     * EN:
     *   - With reconstruction vector c, aggregate into T1 and per-row T2_x.
     */
    public static EcPDCT trans(EcPP pp,
                               String RID,
                               Map<Integer, EcUser.TukPair> tukByAttr,
                               EcCiphertext ct,
                               EcMatrix reconC) {
        final int l = ct.Arho.l;

        // Select rows with nonzero c_x and available TUK
        List<Integer> rows = new ArrayList<>();
        List<Element> cxs  = new ArrayList<>();
        for (int x = 0; x < l; x++) {
            int iAttr = ct.Arho.rho(x);
            EcUser.TukPair tuk = tukByAttr.get(iAttr);
            BigInteger cxBI = reconC.get(0, x);
            if (tuk != null && cxBI != null && !cxBI.equals(BigInteger.ZERO)) {
                rows.add(x);
                cxs.add(pp.toZr(cxBI));
            }
        }

        Element Hr = pp.H2_ofRID(RID);
        Element Hr_mul_h = Hr.mul(pp.h).getImmutable();

        // T1
        Element T1 = pp.GT.newOneElement().getImmutable();
        for (int idx = 0; idx < rows.size(); idx++) {
            int x = rows.get(idx);
            Element cx = cxs.get(idx);
            Element e1 = pp.e(ct.C2A[x], Hr_mul_h);
            Element e2 = pp.e(ct.C2B[x], Hr);
            Element prod = e1.mul(e2).getImmutable();
            T1 = T1.mul(prod.powZn(cx)).getImmutable();
        }

        EcPDCT pd = new EcPDCT(ct.C0, T1);

        // T2_x
        for (int idx = 0; idx < rows.size(); idx++) {
            int x = rows.get(idx);
            Element cxNeg = cxs.get(idx).duplicate().negate().getImmutable();
            int iAttr = ct.Arho.rho(x);
            EcUser.TukPair tuk = tukByAttr.get(iAttr);
            Element partA = pp.e(ct.C1A[x], tuk.SKp_R_A_i);
            Element partB = pp.e(ct.C1B[x], tuk.SKp_R_B_i);
            Element prod = partA.mul(partB).getImmutable();
            Element T2x  = prod.powZn(cxNeg).getImmutable();
            pd.putT2(x, T2x);
        }

        return pd;
    }

    /**
     * Dec(T1, {T2_i}, z_i) -> µ
     *   - Aggregate per-row T2 into per-attribute T2_i; recover u and µ.
     */
    public static byte[] dec2(EcPP pp,
                              byte[] C0,
                              Element T1,
                              Map<Integer, Element> T2_byAttr,
                              Map<Integer, Element> zMap) {

        Element u = T1.duplicate().getImmutable();
        for (Map.Entry<Integer, Element> ent : T2_byAttr.entrySet()) {
            int attr = ent.getKey();
            Element T2i = ent.getValue();
            Element zi  = zMap.get(attr);
            if (zi == null) throw new IllegalArgumentException("Missing z for attr " + attr);
            u = u.mul(T2i.powZn(zi)).getImmutable();
        }

        byte[] stream = pp.extKey(u, 32);
        if (stream.length < C0.length) {
            byte[] ext2 = new byte[C0.length];
            for (int i = 0; i < C0.length; i++) ext2[i] = stream[i % stream.length];
            stream = ext2;
        }
        return pp.xorBytes(C0, stream);
    }
}
