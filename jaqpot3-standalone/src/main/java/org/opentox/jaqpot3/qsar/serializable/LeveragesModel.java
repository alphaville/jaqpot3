/*
 *
 * Jaqpot - version 3
 *
 * The JAQPOT-3 web services are OpenTox API-1.2 compliant web services. Jaqpot
 * is a web application that supports model training and data preprocessing algorithms
 * such as multiple linear regression, support vector machines, neural networks
 * (an in-house implementation based on an efficient algorithm), an implementation
 * of the leverage algorithm for domain of applicability estimation and various
 * data preprocessing algorithms like PLS and data cleanup.
 *
 * Copyright (C) 2009-2012 Pantelis Sopasakis & Charalampos Chomenides
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Contact:
 * Pantelis Sopasakis
 * chvng@mail.ntua.gr
 * Address: Iroon Politechniou St. 9, Zografou, Athens Greece
 * tel. +30 210 7723236
 *
 */

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
