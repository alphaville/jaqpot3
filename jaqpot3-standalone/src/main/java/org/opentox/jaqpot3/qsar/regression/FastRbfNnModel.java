package org.opentox.jaqpot3.qsar.regression;

import java.io.Serializable;
import weka.core.Instances;

/**
 * 
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class FastRbfNnModel implements Serializable {

    private static final long serialVersionUID = 9211084530133014L;
    private double alpha;
    private double beta;
    private double epsilon;
    private double[] sigma;
    private double[] lrCoefficients;
    private Instances nodes;

    public FastRbfNnModel() {
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void setEpsilon(double epsilon) {
        this.epsilon = epsilon;
    }

    public Instances getNodes() {
        return nodes;
    }

    public void setNodes(Instances nodes) {
        this.nodes = nodes;
    }

    public double[] getSigma() {
        return sigma;
    }

    public void setSigma(double[] sigma) {
        this.sigma = sigma;
    }

    public double[] getLrCoefficients() {
        return lrCoefficients;
    }

    public void setLrCoefficients(double[] lrCoefficients) {
        this.lrCoefficients = lrCoefficients;
    }

}
