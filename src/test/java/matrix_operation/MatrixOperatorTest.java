package matrix_operation;

import dev.edinho.pdi.entities.Matrix;
import dev.edinho.pdi.entities.MatrixOperator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.naming.OperationNotSupportedException;

class MatrixOperatorTest {
    @Test
    void testMatrixProduct() throws OperationNotSupportedException {
        Matrix initialPosition = new Matrix(new double[][]{
                {1, 0, 4},
                {0, 1, 3},
                {0, 0, 1}
        });

        Matrix translationMatrix = new Matrix(new double[][]{
                {1}, {2}, {1}
        });

        Matrix finalPosition = MatrixOperator.product(initialPosition, translationMatrix);

        Matrix expected = new Matrix(new double[][]{
                {5}, {5}, {1}
        });

        Assertions.assertEquals(expected, finalPosition);
    }
}
