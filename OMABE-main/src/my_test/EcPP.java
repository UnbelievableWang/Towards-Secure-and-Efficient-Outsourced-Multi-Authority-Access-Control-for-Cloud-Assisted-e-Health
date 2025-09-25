package my_test;

import it.unisa.dia.gas.jpbc.Element;
import it.unisa.dia.gas.jpbc.ElementPowPreProcessing;
import it.unisa.dia.gas.jpbc.Field;
import it.unisa.dia.gas.jpbc.Pairing;
import it.unisa.dia.gas.jpbc.PairingParameters;
import it.unisa.dia.gas.plaf.jpbc.pairing.PairingFactory;
import it.unisa.dia.gas.plaf.jpbc.pairing.a1.TypeA1CurveGenerator;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * EcPP (Public Parameters & Primitives)
 * Composite-order (TypeA1) pairing setup and basic primitives.
 *     g1 with ElementPowPreProcessing; h without preprocessing.
 */
public class EcPP {

    // Groups and Pairing
    public final Pairing pairing;
    public final Field G;   // source group (use G1)
    public final Field GT;  // target group
    public final Field Zr;  // exponent field (mod N)
    public final BigInteger N;

    // Generators
    public final Element g1;                       // generator in G
    public final Element h;                        // another base in G
    public final ElementPowPreProcessing g1PP;     // preprocessed g1

    // Extractor salt
    public final byte[] sd;

    // Internals
    private static final SecureRandom RNG = new SecureRandom();
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_NONCE_BYTES = 12;

    public EcPP(int numPrimes, int qBit) {
        TypeA1CurveGenerator gen = new TypeA1CurveGenerator(numPrimes, qBit);
        PairingParameters params = gen.generate();

        this.pairing = PairingFactory.getPairing(params);
        this.G  = pairing.getG1();
        this.GT = pairing.getGT();
        this.Zr = pairing.getZr();
        this.N  = Zr.getOrder();

        this.g1 = G.newRandomElement().getImmutable();
        this.h  = G.newRandomElement().getImmutable();

        this.g1PP = g1.getElementPowPreProcessing();

        this.sd = new byte[16];
        RNG.nextBytes(this.sd);
    }

    public Element e(Element a, Element b) { return pairing.pairing(a, b).getImmutable(); }

    public Element H2(byte[] in) { return G.newElement().setFromHash(in, 0, in.length).getImmutable(); }

    public Element H2_ofRID(String rid) { return H2(rid.getBytes(StandardCharsets.UTF_8)); }


    public Element randZr() { return Zr.newRandomElement().getImmutable(); }

    public Element toZr(BigInteger bi) { return Zr.newElement(bi).getImmutable(); }

    public Element g1PowZn(Element eZr) { return g1PP.powZn(eZr).getImmutable(); }

    public Element powZn(Element base, Element expZr) { return base.powZn(expZr).getImmutable(); }

    public Element powBI(Element base, BigInteger expBI) { return base.pow(expBI).getImmutable(); }


    public byte[] extKey(Element gt, byte[] salt, int len) {
        byte[] ikm = gt.toBytes();
        byte[] s   = (salt != null) ? salt : sd;
        byte[] prk = hmacSha256(s, ikm);
        return hkdfExpand(prk, "EXT-KEY".getBytes(StandardCharsets.UTF_8), len);
    }
    public byte[] extKey(Element gt, int len) { return extKey(gt, sd, len); }

    public byte[] xorBytes(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        byte[] out = Arrays.copyOf(a, a.length);
        for (int i = 0; i < n; i++) out[i] ^= b[i];
        return out;
    }


    public byte[] aesGcmEncrypt(byte[] key, byte[] plaintext, byte[] aad) {
        try {
            byte[] nonce = new byte[GCM_NONCE_BYTES];
            RNG.nextBytes(nonce);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, nonce);
            SecretKeySpec ks = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.ENCRYPT_MODE, ks, spec);
            if (aad != null && aad.length > 0) cipher.updateAAD(aad);
            byte[] ct = cipher.doFinal(plaintext);
            byte[] out = new byte[nonce.length + ct.length];
            System.arraycopy(nonce, 0, out, 0, nonce.length);
            System.arraycopy(ct, 0, out, nonce.length, ct.length);
            return out;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public byte[] aesGcmDecrypt(byte[] key, byte[] nonceCt, byte[] aad) {
        try {
            byte[] nonce = Arrays.copyOfRange(nonceCt, 0, GCM_NONCE_BYTES);
            byte[] ct    = Arrays.copyOfRange(nonceCt, GCM_NONCE_BYTES, nonceCt.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_BITS, nonce);
            SecretKeySpec ks = new SecretKeySpec(key, "AES");
            cipher.init(Cipher.DECRYPT_MODE, ks, spec);
            if (aad != null && aad.length > 0) cipher.updateAAD(aad);
            return cipher.doFinal(ct);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private byte[] hkdfExpand(byte[] prk, byte[] info, int length) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            int hashLen = 32, n = (int)Math.ceil((double)length / hashLen);
            byte[] okm = new byte[length], t = new byte[0]; int pos = 0;
            for (int i = 1; i <= n; i++) {
                mac.reset(); mac.update(t); mac.update(info); mac.update((byte)i);
                t = mac.doFinal();
                int copy = Math.min(hashLen, length - pos);
                System.arraycopy(t, 0, okm, pos, copy); pos += copy;
            }
            return okm;
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private byte[] hmacSha256(byte[] key, byte[] msg) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(msg);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
