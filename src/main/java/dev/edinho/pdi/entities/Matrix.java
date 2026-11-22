package dev.edinho.pdi.entities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Matrix {
    private final double[][] m;
    private final int rows;
    private final int columns;

    public Matrix(double[][] matrix) {
        this.m = matrix;
        this.rows = matrix.length;
        this.columns = matrix[0].length;
    }

    public void set(double value, int x, int y) {
        m[x][y] = value;
    }

    public double get(int row, int column) {
        return m[row][column];
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

    public Matrix squareSlice(int centerX, int centerY, int dimension) {
        Matrix slice = Matrices.withDimensions(dimension,dimension);
        int range = dimension / 2;
        int startX = centerX - range;
        int endX = centerX + range;
        int startY = centerY - range;
        int endY = centerY + range;

        int sliceX = 0;
        int sliceY = 0;
        for (int x = startX; x <= endX + 1; x++) {
            sliceY = 0;
            for (int y = startY; y <= endY; y++) {

                if (!inBound(x,y)) continue;

                slice.set(get(x,y), sliceX,sliceY);
                sliceY = Math.clamp((long) sliceY + 1, 0, dimension);
            }
            sliceX = Math.clamp((long) sliceX + 1, 0, dimension);
        }
        return slice;
    }

    public boolean inBound(int x, int y) {
        return x >= 0 && x < getColumns() && y >= 0 && y < getRows();
    }
    @Override
    public String toString() {
        StringBuilder strBuilder = new StringBuilder();


        for (int i = 0; i < getRows(); i++) {
            strBuilder.append("[");
            for(int j = 0; j < getColumns(); j++) {
                strBuilder
                        .append(" ")
                        .append(get(i, j))
                        .append(" ");
            }
            strBuilder
                    .append("]")
                    .append("\n");
        }
        return strBuilder.toString();
    }

}
