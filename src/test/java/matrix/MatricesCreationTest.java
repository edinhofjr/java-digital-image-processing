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
}
