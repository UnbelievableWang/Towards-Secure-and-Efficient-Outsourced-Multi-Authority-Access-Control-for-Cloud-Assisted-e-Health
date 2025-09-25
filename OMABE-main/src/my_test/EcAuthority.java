package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;

/**
 * EcAuthority (Attribute Authority)
 * Authority that sets per-attribute public keys and issues user keys.
 */
public class EcAuthority {

    // Attribute id
    public final int attr;

    // Secret exponents in Zr
    public final Element aiZr;  // a_i ∈ Zr
    public final Element biZr;  // b_i ∈ Zr

    // Public keys
    public final Element APKA_i; // g1^{a_i}
    public final Element APKB_i; // g1^{b_i}

    // Optional BI forms / BigInteger
    public final BigInteger aiBI;
    public final BigInteger biBI;


    public EcAuthority(EcPP pp, int attr) {
        this.attr = attr;

        // Sample ai, bi in Zr
        this.aiZr = pp.randZr();
        this.biZr = pp.randZr();
        this.aiBI = aiZr.toBigInteger();
        this.biBI = biZr.toBigInteger();

        // Public components: g1^ai, g1^bi
        this.APKA_i = pp.g1PowZn(aiZr);
        this.APKB_i = pp.g1PowZn(biZr);
    }

    public EcUser.KeyPair keyGen(EcPP pp, String RID) {
        Element Hr = pp.H2_ofRID(RID);
        Element baseA = Hr.mul(pp.h); // H_r * h
        Element SKR_A_i = pp.powZn(baseA, aiZr);
        Element SKR_B_i = pp.powZn(Hr,    biZr);
        return new EcUser.KeyPair(SKR_A_i, SKR_B_i);
    }
}
