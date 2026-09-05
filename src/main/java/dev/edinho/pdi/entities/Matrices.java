package dev.edinho.pdi.entities;

public class Matrices {
    private Matrices() {
        /* This utility class should not be instantiated */
    }

    public static Matrix columnsFromArray(double[] array) {
        double[][] column = new double[array.length][1];
        for (int i = 0; i < array.length; i++) {
            column[i][0] = array[i];
        }
        return new Matrix(column);
    }
}
