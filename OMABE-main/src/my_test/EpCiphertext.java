package my_test;

import it.unisa.dia.gas.jpbc.Element;


public class EpCiphertext {
    public final EpPolicy policy;   // (Γ,ρ)
    public final byte[] C0;         // μ ⊕ Ext(u)

    public final Element[][] C1A, C1B; // in G1
    public final Element[][] C2A, C2B; // in G1

    public EpCiphertext(EpPolicy policy, byte[] c0,
                        Element[][] c1a, Element[][] c1b,
                        Element[][] c2a, Element[][] c2b) {
        this.policy = policy; this.C0 = c0;
        this.C1A = c1a; this.C1B = c1b; this.C2A = c2a; this.C2B = c2b;
    }
}
