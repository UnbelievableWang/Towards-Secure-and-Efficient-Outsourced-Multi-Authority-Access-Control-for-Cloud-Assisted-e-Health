package my_test;

import java.math.BigInteger;
import java.util.Arrays;

public class EcMatrix {
    private BigInteger[][] data;
    private int rows;
    private int cols;
    // Constructor
    public EcMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.data = new BigInteger[rows][cols];
        for (int i = 0; i < rows; i++) {
            Arrays.fill(this.data[i], BigInteger.ZERO);
        }
    }

    // Getters
    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public BigInteger get(int row, int col) {
        return data[row][col];
    }

    // Setters
    public void set(int row, int col, BigInteger value) {
        data[row][col] = value;
    }

    // Initialize matrix with a 2D array
    public EcMatrix(BigInteger[][] data) {
        this.rows = data.length;
        this.cols = data[0].length;
        this.data = new BigInteger[rows][cols];
        for (int i = 0; i < rows; i++) {
            this.data[i] = Arrays.copyOf(data[i], cols);
        }
    }

    // Addition
    public EcMatrix add(EcMatrix other) {
        if (this.rows != other.rows || this.cols != other.cols) {
            throw new IllegalArgumentException("Matrices dimensions do not match for addition.");
        }

        EcMatrix result = new EcMatrix(rows, cols);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result.set(i, j, this.get(i, j).add(other.get(i, j)));
            }
        }

        return result;
    }

    // Multiplication
    public EcMatrix multiply(EcMatrix other) {
        if (this.cols != other.rows) {
            throw new IllegalArgumentException("Matrices dimensions do not match for multiplication.");
        }

        EcMatrix result = new EcMatrix(this.rows, other.cols);
        for (int i = 0; i < this.rows; i++) {
            for (int j = 0; j < other.cols; j++) {
                BigInteger sum = BigInteger.ZERO;
                for (int k = 0; k < this.cols; k++) {
                    sum = sum.add(this.get(i, k).multiply(other.get(k, j)));
                }
                result.set(i, j, sum);
            }
        }

        return result;
    }

}

