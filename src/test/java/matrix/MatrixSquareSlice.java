package matrix;

import dev.edinho.pdi.entities.Matrix;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MatrixSquareSliceTest {
    @Test
    void shouldReturnASquareSliceFromAnotherMatrix() {
        Matrix matrix = new Matrix(new double[][]{
                {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}
        });
        Matrix slice = matrix.squareSlice(3, 3, 3);
        Matrix expected = new Matrix(new double[][]{
                {3, 4, 5}, {3, 4, 5}, {3, 4, 5}
        });

        Assertions.assertEquals(slice, expected);
    }

    @Test
    void shouldReturnASquareSliceFromAnotherMatrixUsingHisBorder() {
        Matrix matrix = new Matrix(new double[][]{
                {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}
        });
        Matrix slice = matrix.squareSlice(3, 4, 3);
        Matrix expected = new Matrix(new double[][]{
                {4, 5, 0}, {4, 5, 0}, {4, 5, 0}
        });

        Assertions.assertEquals(slice, expected);
    }

    @Test
    void shouldCreateASliceWithAnotherDimension() {
        Matrix matrix = new Matrix(new double[][]{
                {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}, {1, 2, 3, 4, 5}
        });
        Matrix slice = matrix.squareSlice(2, 2, 5);

        Assertions.assertEquals(slice, matrix);
    }
}
