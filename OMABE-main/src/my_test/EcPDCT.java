
package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.util.LinkedHashMap;
import java.util.Map;


public class EcPDCT {

    public final byte[] C0;
    public final Element T1;
    public final Map<Integer, Element> T2_byRow = new LinkedHashMap<>();

    public EcPDCT(byte[] C0, Element T1) {
        this.C0 = C0;
        this.T1 = T1;
    }

    public void putT2(int rowX, Element T2x) {
        T2_byRow.put(rowX, T2x);
    }
}
