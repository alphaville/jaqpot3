package org.opentox.jaqpot3.qsar.serializable;

import Jama.Matrix;
import java.io.Serializable;

/**
 * A class encapsulating 'actual' models as these are defined in ToxOtis models.
 * A LeveragesModel object includes all necessary information needed to generate
 * a Domain of Applicability estimation for a test dataset. In particular it contains
 * the matrix Ω = inv(X'X) and the number γ=3k/n.
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class LeveragesModel implements Serializable {

    private static final long serialVersionUID = 165610711007992L;
    private Matrix dataMatrix = null;
    private double gamma = 0;//3k/n

    public LeveragesModel() {
    }

    public LeveragesModel(Matrix dataMatrix, double gamma) {
        this.dataMatrix = dataMatrix;
        this.gamma = gamma;
    }

    /**
     * The characteristic matrix Ω=inv(X'*X)
     * @return
     *      The matrix needed to perform a DoA estimation using the
     *      leverages algorithm.
     */
    public Matrix getDataMatrix() {
        return dataMatrix;
    }

    public void setDataMatrix(Matrix dataMatrix) {
        this.dataMatrix = dataMatrix;
    }

    /**
     * The characteristic value γ=3k/n
     * @return
     */
    public double getGamma() {
        return gamma;
    }

    public void setGamma(double gamma) {
        this.gamma = gamma;
    }

    public void setGamma(int k, int n) {
        this.gamma = 3.0 * k / n;
    }
}
