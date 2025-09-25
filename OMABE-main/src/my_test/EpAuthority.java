package my_test;

import it.unisa.dia.gas.jpbc.Element;

import java.math.BigInteger;


public class EpAuthority {

    public final int attr;
    public final BigInteger[][] W_A, W_B;

    public final BigInteger[] APK_A_exp;     // m_A = (W_A^T * a1) mod q (3×1)
    public final BigInteger[] APK_B_exp;     // m_B = (W_B^T * a1) mod q (3×1)

    public final Element[] APK_A;            // [[m_A]]_1 ∈ G1^{3×1}
    public final Element[] APK_B;            // [[m_B]]_1 ∈ G1^{3×1}

    public EpAuthority(EpPP pp, int attr) {
        this.attr = attr;
        this.W_A = randMat3x3(pp);
        this.W_B = randMat3x3(pp);

        BigInteger[][] WAt = EpMatrix.transpose(W_A);
        BigInteger[][] WBt = EpMatrix.transpose(W_B);
        this.APK_A_exp = EpMatrix.mulMatVecMod(pp, WAt, pp.a1);
        this.APK_B_exp = EpMatrix.mulMatVecMod(pp, WBt, pp.a1);

        this.APK_A = pp.liftVecToG1(APK_A_exp);
        this.APK_B = pp.liftVecToG1(APK_B_exp);
    }

    private static BigInteger[][] randMat3x3(EpPP pp) {
        BigInteger[][] M = new BigInteger[3][3];
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 3; j++)
                M[i][j] = pp.randZqBI();
        return M;
    }
}
