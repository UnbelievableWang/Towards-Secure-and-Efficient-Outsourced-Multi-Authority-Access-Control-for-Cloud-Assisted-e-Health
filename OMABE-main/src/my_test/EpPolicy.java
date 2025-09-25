package my_test;

import java.math.BigInteger;
import java.util.*;

public class EpPolicy {

    public final int l, n;                  // rows, cols
    public final BigInteger[][] Gamma;      // l×n over Z_q
    public final Map<Integer, Integer> rho; // row -> attr
    public EpPolicy(BigInteger[][] Gamma, Map<Integer,Integer> rho) {
        this.l = Gamma.length; this.n = Gamma[0].length;
        this.Gamma = Gamma; this.rho = new HashMap<>(rho);
    }

    public List<Integer> rowsForAttrs(Set<Integer> attrs) {
        List<Integer> xs = new ArrayList<>();
        for (int x = 0; x < l; x++) if (attrs.contains(rho.get(x))) xs.add(x);
        return xs;
    }

    public BigInteger[] gammaColFromRow(int x) {
        BigInteger[] v = new BigInteger[n];
        for (int j = 0; j < n; j++) v[j] = Gamma[x][j];
        return v;
    }

    public Map<Integer, BigInteger> solveRecon(EpPP pp, List<Integer> X) {
        int m = X.size();
        if (m == 0) throw new IllegalArgumentException("empty X");

        // A = Γ_X^T  (n×m)
        BigInteger[][] A = new BigInteger[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                A[i][j] = pp.mod(Gamma[X.get(j)][i]);

        // b = e1
        BigInteger[] b = new BigInteger[n];
        b[0] = BigInteger.ONE;
        for (int i = 1; i < n; i++) b[i] = BigInteger.ZERO;

        BigInteger[] c = solveLinearModP(pp, A, b); // length m
        Map<Integer, BigInteger> out = new LinkedHashMap<>();
        for (int j = 0; j < m; j++) out.put(X.get(j), pp.mod(c[j]));
        return out;
    }

    private static BigInteger[] solveLinearModP(EpPP pp, BigInteger[][] A, BigInteger[] b) {
        int r = A.length, c = A[0].length;
        BigInteger[][] M = new BigInteger[r][c + 1];
        for (int i = 0; i < r; i++) {
            System.arraycopy(A[i], 0, M[i], 0, c);
            M[i][c] = b[i];
        }

        int row = 0;
        int[] pivCol = new int[r];
        Arrays.fill(pivCol, -1);

        for (int col = 0; col < c && row < r; col++) {
            int piv = -1;
            for (int i = row; i < r; i++) if (!pp.mod(M[i][col]).equals(BigInteger.ZERO)) { piv = i; break; }
            if (piv == -1) continue;
            if (piv != row) { BigInteger[] tmp = M[piv]; M[piv] = M[row]; M[row] = tmp; }
            BigInteger inv = pp.mod(M[row][col]).modInverse(pp.q);
            for (int j = col; j <= c; j++) M[row][j] = pp.mod(M[row][j].multiply(inv));
            for (int i = 0; i < r; i++) if (i != row) {
                BigInteger f = M[i][col];
                if (pp.mod(f).equals(BigInteger.ZERO)) continue;
                for (int j = col; j <= c; j++) {
                    BigInteger t = pp.mod(M[row][j].multiply(f));
                    M[i][j] = pp.mod(M[i][j].subtract(t));
                }
            }
            pivCol[row] = col; row++;
        }

        BigInteger[] x = new BigInteger[c];
        Arrays.fill(x, BigInteger.ZERO);
        for (int i = 0; i < r; i++) if (pivCol[i] != -1) x[pivCol[i]] = pp.mod(M[i][c]);
        return x;
    }
}
