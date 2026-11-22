package matrix;

import dev.edinho.pdi.entities.Matrix;
import dev.edinho.pdi.entities.Matrices;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class MatricesCreationTest {
    @Test
    void testMatrixCreation() {
        Matrix m = Matrices.columnsFromArray(new double[]{1, 2, 3, 4, 5});
        Matrix expected = new Matrix(new double[][]{
                {1}, {2}, {3}, {4}, {5}
        });

        Assertions.assertEquals(expected, m);
    }

    @Test
    void createMatrixWithNMDimensions() {
        int n = 3;
        int m = 3;

        Matrix matrix = Matrices.withDimensions(n,m);
        Matrix expected = new Matrix(new double[][]{{0, 0, 0}, {0,0,0}, {0,0,0}});

        Assertions.assertEquals(matrix,expected);
    }
}
