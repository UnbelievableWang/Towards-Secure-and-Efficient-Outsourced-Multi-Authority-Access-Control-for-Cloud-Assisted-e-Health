package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.util.LinkedHashMap;
import java.util.Map;


public class EpPDCT {
    public final byte[] C0;
    public final Element T1;             // in GT
    public final Element T2;             // in GT
    public final Map<Integer, Element> cxByRow = new LinkedHashMap<>(); // 仅记录


    public EpPDCT(byte[] c0, Element t1, Element t2) {
        this.C0 = c0; this.T1 = t1; this.T2 = t2;
    }
}
