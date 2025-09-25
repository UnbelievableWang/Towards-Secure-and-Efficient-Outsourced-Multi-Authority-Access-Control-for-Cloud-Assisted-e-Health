package my_test;

import java.math.BigInteger;


public final class EpMatrix {

    private EpMatrix() {}


    public static BigInteger[][] transpose(BigInteger[][] M) {
        int r = M.length, c = M[0].length;
        BigInteger[][] T = new BigInteger[c][r];
        for (int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
                T[j][i] = M[i][j];
        return T;
    }

    public static BigInteger[] col(BigInteger[][] M, int j) {
        BigInteger[] v = new BigInteger[M.length];
        for (int i = 0; i < M.length; i++) v[i] = M[i][j];
        return v;
    }


    public static BigInteger det3(EpPP pp, BigInteger[][] m) {
        BigInteger a = m[0][0], b = m[0][1], c = m[0][2];
        BigInteger d = m[1][0], e = m[1][1], f = m[1][2];
        BigInteger g = m[2][0], h = m[2][1], i = m[2][2];
        BigInteger det = a.multiply(e).multiply(i)
                .add(b.multiply(f).multiply(g))
                .add(c.multiply(d).multiply(h))
                .subtract(c.multiply(e).multiply(g))
                .subtract(b.multiply(d).multiply(i))
                .subtract(a.multiply(f).multiply(h));
        return pp.mod(det);
    }

    public static BigInteger[][] randInvertible3x3(EpPP pp) {
        while (true) {
            BigInteger[][] M = new BigInteger[3][3];
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    M[i][j] = pp.randZqBI();
            if (!pp.mod(det3(pp, M)).equals(BigInteger.ZERO)) return M;
        }
    }

    public static BigInteger[][] inverse3(EpPP pp, BigInteger[][] M) {
        int n = 3;
        BigInteger[][] A = new BigInteger[n][2*n];
        for (int i = 0; i < n; i++) {
            System.arraycopy(M[i], 0, A[i], 0, n);
            for (int j = 0; j < n; j++) A[i][n+j] = (i==j) ? BigInteger.ONE : BigInteger.ZERO;
        }
        int r = 0;
        for (int c = 0; c < n && r < n; c++) {
            int piv = -1;
            for (int i = r; i < n; i++) if (!pp.mod(A[i][c]).equals(BigInteger.ZERO)) { piv = i; break; }
            if (piv == -1) throw new IllegalArgumentException("singular");
            if (piv != r) { BigInteger[] tmp=A[piv]; A[piv]=A[r]; A[r]=tmp; }
            BigInteger inv = pp.mod(A[r][c]).modInverse(pp.q);
            for (int j = c; j < 2*n; j++) A[r][j] = pp.mod(A[r][j].multiply(inv));
            for (int i = 0; i < n; i++) if (i != r) {
                BigInteger f = A[i][c];
                if (pp.mod(f).equals(BigInteger.ZERO)) continue;
                for (int j = c; j < 2*n; j++) {
                    BigInteger t = pp.mod(A[r][j].multiply(f));
                    A[i][j] = pp.mod(A[i][j].subtract(t));
                }
            }
            r++;
        }
        BigInteger[][] INV = new BigInteger[n][n];
        for (int i = 0; i < n; i++) System.arraycopy(A[i], n, INV[i], 0, n);
        return INV;
    }

    public static BigInteger[] mulMatVecMod(EpPP pp, BigInteger[][] M, BigInteger[] v) {
        BigInteger[] out = new BigInteger[3];
        for (int i = 0; i < 3; i++) {
            BigInteger acc = BigInteger.ZERO;
            for (int j = 0; j < 3; j++) acc = acc.add(pp.mod(M[i][j]).multiply(pp.mod(v[j])));
            out[i] = pp.mod(acc);
        }
        return out;
    }

    public static BigInteger[] rightActExp(EpPP pp, BigInteger[][] E3xn, BigInteger[] gammaCol) {
        int n = E3xn[0].length;
        BigInteger[] out = new BigInteger[3];
        for (int i = 0; i < 3; i++) {
            BigInteger acc = BigInteger.ZERO;
            for (int k = 0; k < n; k++) acc = acc.add(pp.mod(E3xn[i][k]).multiply(pp.mod(gammaCol[k])));
            out[i] = pp.mod(acc);
        }
        return out;
    }

    public static BigInteger[] addVecMod(EpPP pp, BigInteger[] u, BigInteger[] v) {
        BigInteger[] w = new BigInteger[u.length];
        for (int i = 0; i < u.length; i++) w[i] = pp.mod(u[i].add(v[i]));
        return w;
    }

    public static BigInteger[] scalarVecMod(EpPP pp, BigInteger[] u, BigInteger k) {
        BigInteger[] w = new BigInteger[u.length];
        for (int i = 0; i < u.length; i++) w[i] = pp.mod(u[i].multiply(k));
        return w;
    }
}
