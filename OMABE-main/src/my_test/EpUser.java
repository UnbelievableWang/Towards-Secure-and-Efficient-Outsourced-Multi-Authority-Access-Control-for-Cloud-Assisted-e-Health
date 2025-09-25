package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

public class EpUser {

    public final String RID;

    public final Map<Integer, Element[]> SKA = new HashMap<>();
    public final Map<Integer, Element[]> SKB = new HashMap<>();

    public final Map<Integer, BigInteger[]> SKAExp = new HashMap<>();
    public final Map<Integer, BigInteger[]> SKBExp = new HashMap<>();

    public BigInteger zR;
    public final Map<Integer, Element[]> SKpA = new HashMap<>();
    public final Map<Integer, Element[]> SKpB = new HashMap<>();

    public EpUser(String RID) { this.RID = RID; }

    public void receiveFromAuthority(EpPP pp, int attr, EpAuthority aa) {
        BigInteger[] e  = pp.H2_exp3(RID);
        BigInteger[] eH = EpMatrix.addVecMod(pp, e, pp.HcalExp);

        BigInteger[] skaExp = EpMatrix.mulMatVecMod(pp, aa.W_A, eH);
        BigInteger[] skbExp = EpMatrix.mulMatVecMod(pp, aa.W_B, e);

        Element[] ska = pp.liftVecToG2(skaExp);
        Element[] skb = pp.liftVecToG2(skbExp);

        SKA.put(attr, ska);  SKB.put(attr, skb);
        SKAExp.put(attr, skaExp);  SKBExp.put(attr, skbExp);
    }

    public void tkGen(EpPP pp, Iterable<Integer> attrs) {
        this.zR = pp.randZqBI();
        BigInteger inv = pp.mod(this.zR).modInverse(pp.q);
        for (int i : attrs) {
            BigInteger[] skaExp = SKAExp.get(i);
            BigInteger[] skbExp = SKBExp.get(i);
            if (skaExp != null) SKpA.put(i, pp.liftVecToG2(EpMatrix.scalarVecMod(pp, skaExp, inv)));
            if (skbExp != null) SKpB.put(i, pp.liftVecToG2(EpMatrix.scalarVecMod(pp, skbExp, inv)));
        }
    }
}
