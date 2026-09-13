package dev.edinho.pdi.entities;

public class MatrixOperator {
    private MatrixOperator() {
        /* This utility class should not be instantiated */
    }

    public static Matrix product(Matrix m, Matrix n) {
        if (m.getColumns() != n.getRows()) {
            throw new IllegalArgumentException("Cannot compute matrix product: incompatible dimensions.");
        }
        double[][] c = new double[m.getRows()][n.getColumns()];
        for (int i = 0; i < m.getRows(); i++) {
            for (int j = 0; j < n.getColumns(); j++) {
                double sum = 0;
                for (int k = 0; k < m.getColumns(); k++) {
                    sum += m.get(i, k) * n.get(k, j);
                }
                c[i][j] = sum;
            }
        }
        return new Matrix(c);
    }
}
