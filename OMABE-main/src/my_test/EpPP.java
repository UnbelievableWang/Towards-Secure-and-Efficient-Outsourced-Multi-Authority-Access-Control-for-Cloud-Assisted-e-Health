package my_test;

import it.unisa.dia.gas.jpbc.*;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a.TypeACurveGenerator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

public class EpPP {

    public final Pairing pairing;
    public final Field G1, G2, GT, Zq;
    public final BigInteger q;

    public final Element g1, g2, gT;
    public final ElementPowPreProcessing g1PP, g2PP, gTPP;

    public final BigInteger[][] A;
    public final BigInteger[][] B;
    public final BigInteger[] a1;
    public final BigInteger[] b1;

    public final Element[] a1G1;
    public final Element[] Hcal;
    public final BigInteger[] HcalExp;

    private static final SecureRandom RNG = new SecureRandom();
    public final byte[] sd;
    public interface H2Func { Element[] hashVec3(String rid); }
    public final H2Func H2;

    public EpPP(int rBits, int qBits) {
        TypeACurveGenerator gen = new TypeACurveGenerator(rBits, qBits);
        PairingParameters params = gen.generate();
        this.pairing = PairingFactory.getPairing(params);
        this.G1 = pairing.getG1();
        this.G2 = pairing.getG2();
        this.GT = pairing.getGT();
        this.Zq = pairing.getZr();
        this.q  = Zq.getOrder();

        this.g1 = G1.newRandomElement().getImmutable();
        this.g2 = G2.newRandomElement().getImmutable();
        this.gT = pairing.pairing(g1, g2).getImmutable();
        this.g1PP = g1.getElementPowPreProcessing();
        this.g2PP = g2.getElementPowPreProcessing();
        this.gTPP = gT.getElementPowPreProcessing();

        this.A = EpMatrix.randInvertible3x3(this);
        this.B = EpMatrix.inverse3(this, EpMatrix.transpose(A));
        this.a1 = EpMatrix.col(A, 0);
        this.b1 = EpMatrix.col(B, 0);

        this.a1G1 = liftVecToG1(a1);

        BigInteger h = randZqBI();
        BigInteger[] hb1 = new BigInteger[3];
        for (int i = 0; i < 3; i++) hb1[i] = mod(h.multiply(b1[i]));
        this.HcalExp = hb1;
        this.Hcal    = liftVecToG2(hb1);

        this.sd = new byte[16];
        RNG.nextBytes(this.sd);
        this.H2 = this::H2_vec3;
    }


    public BigInteger randZqBI() { return Zq.newRandomElement().toBigInteger(); }

    public BigInteger mod(BigInteger x) {
        x = x.mod(q);
        return x.signum() >= 0 ? x : x.add(q);
    }

    public Element[][] liftMatToG1(BigInteger[][] N) {
        int r = N.length, c = N[0].length;
        Element[][] M = new Element[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                M[i][j] = g1PP.powZn(Zq.newElement(mod(N[i][j]))).getImmutable();
        return M;
    }

    public Element[] liftVecToG1(BigInteger[] v) {
        Element[] out = new Element[v.length];
        for (int i = 0; i < v.length; i++)
            out[i] = g1PP.powZn(Zq.newElement(mod(v[i]))).getImmutable();
        return out;
    }

    public Element[][] liftMatToG2(BigInteger[][] N) {
        int r = N.length, c = N[0].length;
        Element[][] M = new Element[r][c];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                M[i][j] = g2PP.powZn(Zq.newElement(mod(N[i][j]))).getImmutable();
        return M;
    }

    public Element[] liftVecToG2(BigInteger[] v) {
        Element[] out = new Element[v.length];
        for (int i = 0; i < v.length; i++)
            out[i] = g2PP.powZn(Zq.newElement(mod(v[i]))).getImmutable();
        return out;
    }

    public Element[] rightAct_G1(Element[][] G3xn, BigInteger[] gammaCol) {
        int n = G3xn[0].length;
        Element[] out = new Element[3];
        for (int i = 0; i < 3; i++) {
            Element acc = G1.newOneElement();
            for (int k = 0; k < n; k++) {
                BigInteger e = mod(gammaCol[k]);
                if (!e.equals(BigInteger.ZERO)) acc = acc.mul(G3xn[i][k].pow(e));
            }
            out[i] = acc.getImmutable();
        }
        return out;
    }

    public Element[] leftAct_Zq_on_G2(BigInteger[][] W3, Element[] Y3) {
        Element[] out = new Element[3];
        for (int i = 0; i < 3; i++) {
            Element acc = G2.newOneElement();
            for (int j = 0; j < 3; j++) {
                BigInteger e = mod(W3[i][j]);
                if (!e.equals(BigInteger.ZERO)) acc = acc.mul(Y3[j].pow(e));
            }
            out[i] = acc.getImmutable();
        }
        return out;
    }

    public Element[] hadamard(Element[] U3, Element[] V3, Field Gb) {
        Element[] out = new Element[3];
        for (int i = 0; i < 3; i++) out[i] = U3[i].duplicate().mul(V3[i]).getImmutable();
        return out;
    }

    public Element[][] concatColsG1(Element[] col3, Element[][] M3xk) {
        int k = (M3xk == null) ? 0 : M3xk[0].length;
        Element[][] R = new Element[3][k + 1];
        for (int i = 0; i < 3; i++) {
            R[i][0] = col3[i];
            for (int j = 0; j < k; j++) R[i][j+1] = M3xk[i][j];
        }
        return R;
    }

    public Element pairVec(Element[] v1_G1, Element[] v2_G2) {
        Element acc = GT.newOneElement();
        for (int k = 0; k < 3; k++) acc = acc.mul(pairing.pairing(v1_G1[k], v2_G2[k]));
        return acc.getImmutable();
    }


    public BigInteger[] H2_exp3(String rid) {
        byte[] base = rid.getBytes(StandardCharsets.UTF_8);
        BigInteger[] e = new BigInteger[3];
        for (int i = 0; i < 3; i++) {
            byte[] tag = ("|" + i).getBytes(StandardCharsets.UTF_8);
            byte[] buf = new byte[base.length + tag.length];
            System.arraycopy(base, 0, buf, 0, base.length);
            System.arraycopy(tag, 0, buf, base.length, tag.length);
            Element zr = Zq.newElement().setFromHash(buf, 0, buf.length);
            e[i] = zr.toBigInteger();
        }
        return e;
    }

    private Element[] H2_vec3(String rid) { return liftVecToG2(H2_exp3(rid)); }

    public byte[] ext(Element gt, int len) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sd, "HmacSHA256"));
            byte[] prk = mac.doFinal(gt.toBytes());
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            byte[] okm = new byte[len], t = new byte[0];
            int pos = 0, i = 1;
            while (pos < len) {
                mac.reset(); mac.update(t); mac.update("EXT-KEY".getBytes(StandardCharsets.UTF_8)); mac.update((byte)i++);
                t = mac.doFinal();
                int cp = Math.min(32, len - pos);
                System.arraycopy(t, 0, okm, pos, cp);
                pos += cp;
            }
            return okm;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public static byte[] xor(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        byte[] out = Arrays.copyOf(a, a.length);
        for (int i = 0; i < n; i++) out[i] ^= b[i];
        return out;
    }
}
