package dev.edinho.pdi.entities;

import java.util.Arrays;

public class Matrix {
    public final double[][] m;
    private final int rows;
    private final int columns;

    public Matrix(double[][] matrix) {
        this.m = matrix;
        this.rows = matrix.length;
        this.columns = matrix[0].length;
    }

    public int getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Matrix other)) return false;
        return Arrays.deepEquals(this.m, other.m);
    }

    @Override
    public int hashCode() {
        return Arrays.deepHashCode(m);
    }
}
