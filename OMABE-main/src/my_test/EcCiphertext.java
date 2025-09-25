package my_test;

import it.unisa.dia.gas.jpbc.Element;


public class EcCiphertext {

    public final EcPolicy Arho;   //  access structure
    public final byte[] C0;   // µ ⊕ Ext(...)
    public final Element[] C1A, C2A, C1B, C2B; // per-row components

    public EcCiphertext(EcPolicy Arho, byte[] C0, Element[] C1A, Element[] C2A, Element[] C1B, Element[] C2B) {
        this.Arho = Arho;
        this.C0 = C0;
        this.C1A = C1A;
        this.C2A = C2A;
        this.C1B = C1B;
        this.C2B = C2B;
    }
}
