package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.util.HashMap;
import java.util.Map;

public class EcUser {

    /** 用户在属性 i 下的密钥对 / Key pair for attribute i */
    public static class KeyPair {
        public final Element SKR_A_i; // (H_r * h)^{a_i}
        public final Element SKR_B_i; // (H_r)^{b_i}
        public KeyPair(Element a, Element b) { this.SKR_A_i = a; this.SKR_B_i = b; }
    }

    /** 传给云的变换用密钥 / Transformed user key for cloud */
    public static class TukPair {
        public final Element SKp_R_A_i; // SK_{R,A,i}^{1/z_i}
        public final Element SKp_R_B_i; // SK_{R,B,i}^{1/z_i}
        public TukPair(Element a, Element b) { this.SKp_R_A_i = a; this.SKp_R_B_i = b; }
    }

    public final String RID;

    public final Map<Integer, KeyPair> userKeys = new HashMap<>();

    public final Map<Integer, Element> zMap = new HashMap<>();

    public EcUser(String RID) { this.RID = RID; }

    public void receiveKey(int attr, KeyPair kp) { userKeys.put(attr, kp); }

    public Map<Integer, TukPair> tkGen(EcPP pp, Iterable<Integer> I) {
        Map<Integer, TukPair> out = new HashMap<>();
        for (int i : I) {
            KeyPair kp = userKeys.get(i);
            if (kp == null) continue;

            Element z = pp.randZr();
            Element zInv = z.invert().getImmutable();
            zMap.put(i, z);

            Element SKpA = pp.powZn(kp.SKR_A_i, zInv);
            Element SKpB = pp.powZn(kp.SKR_B_i, zInv);
            out.put(i, new TukPair(SKpA, SKpB));
        }
        return out;
    }
}
