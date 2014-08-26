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
package org.opentox.jaqpot3.resources.collections;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import org.opentox.jaqpot3.util.Configuration;
import org.opentox.toxotis.core.component.Algorithm;
import org.opentox.toxotis.core.component.Parameter;
import org.opentox.toxotis.exceptions.impl.ToxOtisException;
import org.opentox.toxotis.ontology.LiteralValue;
import org.opentox.toxotis.ontology.MetaInfo;
import org.opentox.toxotis.ontology.OntologicalClass;
import org.opentox.toxotis.ontology.ResourceValue;
import org.opentox.toxotis.ontology.collection.OTAlgorithmTypes;
import org.opentox.toxotis.ontology.collection.OTClasses;
import org.opentox.toxotis.ontology.impl.MetaInfoImpl;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class Algorithms {

    private static Algorithm mlr;
    private static Algorithm svm;
    //private static Algorithm svc;
    private static Algorithm leverages;
    private static Algorithm mvh;
    private static Algorithm plsFilter;
    //private static Algorithm svmFilter;
    private static Algorithm fastRbfNn;
    private static Algorithm scaling;
    private static Algorithm modelBundler;
//    private static Algorithm fcbf;
//    private static Algorithm multifilter;
    private static Set<Algorithm> repository;
    private static org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Algorithms.class);
    private static final String _LICENSE = "JAQPOT - Just Another QSAR Project under OpenTox\n"
            + "Machine Learning algorithms designed for the prediction of toxicological "
            + "features of chemical compounds become available on the Web.\n\nJaqpot is developed "
            + "under OpenTox (see http://opentox.org ) which is an FP7-funded EU research project. "
            + "This project was developed at the Automatic Control Lab in the Chemical Engineering "
            + "School of National Technical University of Athens. Please read README for more "
            + "information.\n"
            + "\n"
            + "Copyright (C) 2009-2010 Pantelis Sopasakis & Charalampos Chomenides\n"
            + "\n"
            + "This program is free software: you can redistribute it and/or modify "
            + "it under the terms of the GNU General Public License as published by "
            + "the Free Software Foundation, either version 3 of the License, or "
            + "(at your option) any later version.\n"
            + "\n"
            + "This program is distributed in the hope that it will be useful, "
            + "but WITHOUT ANY WARRANTY; without even the implied warranty of "
            + "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the "
            + "GNU General Public License for more details."
            + "\n\n"
            + "You should have received a copy of the GNU General Public License "
            + "along with this program.  If not, see http://www.gnu.org/licenses/ .\n\n"
            + "The Jaqpot source code is found online at https://github.com/alphaville/jaqpot3";

    static {
        repository = new HashSet<Algorithm>();
        for (Method m : Algorithms.class.getDeclaredMethods()) {
            try {
                if (m.getReturnType().equals(Algorithm.class) && m.getParameterTypes().length == 0) {
                    repository.add((Algorithm) m.invoke(null));
                }
            } catch (IllegalAccessException ex) {
                logger.error("Cannot access method in Algorithms", ex);
                throw new RuntimeException(ex);
            } catch (IllegalArgumentException ex) {
                logger.error("Unexpected Illegal Argument Exception while accessing no-argument methods in Algorithms", ex);
                throw new RuntimeException(ex);
            } catch (InvocationTargetException ex) {
                logger.error("Some algorithm definition throws an exception", ex);
                throw new RuntimeException(ex);
            }
        }
    }

    public static Set<Algorithm> getAll() {
        return repository;
    }

    public static Algorithm forName(String name) {
        for (Algorithm a : getAll()) {
            if (a.getUri().getId().equals(name)) {
                return a;
            }
        }
        return null;
        
    }

    public static Algorithm fastRbfNn() {
        if (fastRbfNn == null) {
            try {
                fastRbfNn = new Algorithm(Configuration.getBaseUri().augment("algorithm", "fastRbfNn"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("fastRbfNn", "Fast Training Algorithm for RBF networks based on subtractive clustering").
                        addSubject("Regression", "RBF", "Training", "ANN", "Artificial Neural Network", "Machine Learning", "Single Target", "Eager Learning").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Fast-RBF-NN is a training algorithm for Radial Basis Function Neural Networks. The algorithm is based on the "
                        + "subtractive clustering technique and has a number of advantages compared to the traditional learning algorithms including faster "
                        + "training times and more accurate predictions. Due to these advantages the method proves suitable for developing models for complex "
                        + "nonlinear systems. The algorithm is presented in detail in the publication: H. Sarimveis, A. Alexandridis and G. Bafas, 'A fast algorithm "
                        + "for RBF networks based on subtractive clustering', Neurocomuting 51 (2003), 501-505").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(fastRbfNn.getUri().toString());
                fastRbfNn.setMeta(algorithmMeta);
                fastRbfNn.setOntologies(new HashSet<OntologicalClass>());
                fastRbfNn.getOntologies().add(OTAlgorithmTypes.regression());
                fastRbfNn.getOntologies().add(OTAlgorithmTypes.singleTarget());
                fastRbfNn.getOntologies().add(OTAlgorithmTypes.eagerLearning());

                fastRbfNn.setParameters(new HashSet<Parameter>());

                Parameter a =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_a"), "a", new LiteralValue(1.0d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                a.getMeta().addDescription("Design parameter involved in the caclulation of the initial potential of all vectors of the training set according to the "
                        + "formula P(i)=sum_{j=1}^{K}exp(-a*||x(i)-x(j)||^2) for i=1,2,...,K.");
                fastRbfNn.getParameters().add(a);

                Parameter b =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_b"), "b", new LiteralValue(0.9d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                b.getMeta().addDescription("A design parameter that is suggested to be chosen smaller than a to avoid the selection of closely located hidden nodes. This parameter is "
                        + "involved in the formula that defines the potential update in every step of the algorithm, that is P(i) = P(i) - P(L)exp(||x(i)-x*(L)||^2).");
                fastRbfNn.getParameters().add(b);

                Parameter e =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_e"), "e", new LiteralValue(0.6d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                e.getMeta().addDescription("Parameter used to implicitly determine the number of iterations and therefore the number hidden nodes the "
                        + "algorithm will find. The algorithm terminates when max_{i}P(i) is less than or equal to e*P*(L)");
                fastRbfNn.getParameters().add(e);
                /*
                Parameter scalingParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_scaling"), "scaling", new LiteralValue(1, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingParam.getMeta().addDescription("Set scaling enabled");
                fastRbfNn.getParameters().add(scalingParam);
                
                Parameter scalingMinParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_scaling_min"), "scaling_min", new LiteralValue(3, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingMinParam.getMeta().addDescription("Set scaling min");
                fastRbfNn.getParameters().add(scalingMinParam);
                
                Parameter scalingMaxParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_scaling_max"), "scaling_max", new LiteralValue(3, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingMaxParam.getMeta().addDescription("Set scaling max");
                fastRbfNn.getParameters().add(scalingMaxParam);
                
                Parameter normalizationParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_normalization"), "normalization", new LiteralValue(1, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                normalizationParam.getMeta().addDescription("Set normalization enabled");
                fastRbfNn.getParameters().add(normalizationParam);
                 */               
                Parameter missingValHandlingParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_mvh"), "mvh", new LiteralValue(0, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                missingValHandlingParam.getMeta().addDescription("Set missing value handling enabled");
                fastRbfNn.getParameters().add(missingValHandlingParam);
                
                
                Parameter missingValHandlingIgnoreUriParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "fast_rbf_nn_mvh_ignore_uri"), "mvh_ignore_uri", new LiteralValue<URI>(null, XSDDatatype.XSDanyURI)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                missingValHandlingIgnoreUriParam.getMeta().addComment("You can specify multiple URIs to be ignored");
                fastRbfNn.getParameters().add(missingValHandlingIgnoreUriParam);
                
                
                fastRbfNn.getMeta().addRights(_LICENSE);
                fastRbfNn.setEnabled(true);

            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return fastRbfNn;
    }

    public static Algorithm scaling() {
        if (scaling == null) {
            try {
                scaling = new Algorithm(Configuration.getBaseUri().augment("algorithm", "scaling"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("scaling").
                        addSubject("Filter", "Data Preprocessing", "Scaling", "Data Preparation").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("This web service is intended to scale the numeric values of an OpenTox dataset within a specified range."
                        + "If not otherwise specified by the client, this range is assumed to be [-1,1]. Scaling is necessary for algorithms "
                        + "like SVM and Neural Networks as it substantially improves the accuracy of the trained models. In other cases such as "
                        + "MLR it can numerically stabilize the training procedure and is one of the mode fundamental preprocessing steps").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(scaling.getUri().toString());
                scaling.setMeta(algorithmMeta);
                scaling.setOntologies(new HashSet<OntologicalClass>());
                scaling.getOntologies().add(OTAlgorithmTypes.preprocessing());

                scaling.setParameters(new HashSet<Parameter>());

                Parameter min =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "scaling_min"), "min", new LiteralValue(0d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                min.getMeta().addDescription("Minimum value for the scaled data");
                scaling.getParameters().add(min);

                Parameter max =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "scaling_max"), "max", new LiteralValue(1d, XSDDatatype.XSDdouble)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                max.getMeta().addDescription("Maximum value for the scaled data");
                scaling.getParameters().add(max);

                Parameter ignore_uri =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "ignore_uri"), "ignore_uri", new LiteralValue(null, XSDDatatype.XSDanyURI)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                ignore_uri.getMeta().addDescription("If a dataset URI is provided, then the scaling is carried out with respect to the minimum and maximum "
                        + "values of the features in that dataset. Used for applying a dataset on a model that requires scaled data.");
                scaling.getParameters().add(ignore_uri);
                
                Parameter scalingParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "scaling_scaling"), "scaling", new LiteralValue(0d, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingParam.getMeta().addDescription("Set scaling enabled");
                scaling.getParameters().add(scalingParam);
                
                scaling.getMeta().addRights(_LICENSE);
                scaling.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return scaling;
    }
    
    public static Algorithm mlr() {
        if (mlr == null) {
            try {
                mlr = new Algorithm(Configuration.getBaseUri().augment("algorithm", "mlr"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("mlr", "Multiple Linear Regression Training Algorithm").
                        addComment("Multiple Linear Regression Algorithm").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Regression", "Linear", "Training", "Multiple Linear Regression", "Machine Learning", "Single Target", "Eager Learning", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Training algorithm for multiple linear regression models. "
                        + "Applies on datasets which contain exclusively numeric data entries. The algorithm is an implementation "
                        + "of LinearRegression of Weka. More information about Linear Regression you will find at "
                        + "http://en.wikipedia.org/wiki/Linear_regression. The weka API for Linear Regression Training is located at "
                        + "http://weka.sourceforge.net/doc/weka/classifiers/functions/LinearRegression.html").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(mlr.getUri().toString());
                mlr.setMeta(algorithmMeta);
                mlr.setOntologies(new HashSet<OntologicalClass>());
                mlr.getOntologies().add(OTAlgorithmTypes.regression());
                mlr.getOntologies().add(OTAlgorithmTypes.singleTarget());
                mlr.getOntologies().add(OTAlgorithmTypes.eagerLearning());
                mlr.setParameters(new HashSet<Parameter>());
                
                /*
                Parameter scalingParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "mlr_scaling"), "scaling", new LiteralValue(1, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingParam.getMeta().addDescription("Set scaling enabled");
                mlr.getParameters().add(scalingParam);
                
                Parameter scalingMinParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "mlr_scaling_min"), "scaling_min", new LiteralValue(3, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingMinParam.getMeta().addDescription("Set scaling min");
                mlr.getParameters().add(scalingMinParam);
                
                Parameter scalingMaxParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "mlr_scaling_max"), "scaling_max", new LiteralValue(3, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingMaxParam.getMeta().addDescription("Set scaling max");
                mlr.getParameters().add(scalingMaxParam);
                
                Parameter normalizationParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "mlr_normalization"), "normalization", new LiteralValue(1, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                normalizationParam.getMeta().addDescription("Set normalization enabled");
                mlr.getParameters().add(normalizationParam);
                */
                Parameter missingValHandlingParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "mlr_mvh"), "mvh", new LiteralValue(0, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                missingValHandlingParam.getMeta().addDescription("Set missing value handling enabled");
                mlr.getParameters().add(missingValHandlingParam);
                
                
                Parameter missingValHandlingIgnoreUriParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "mlr_mvh_ignore_uri"), "mvh_ignore_uri", new LiteralValue<URI>(null, XSDDatatype.XSDanyURI)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                missingValHandlingIgnoreUriParam.getMeta().addComment("You can specify multiple URIs to be ignored");
                mlr.getParameters().add(missingValHandlingIgnoreUriParam);
                
                mlr.getMeta().addRights(_LICENSE);
                mlr.setEnabled(true);
                
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return mlr;
    }

    public static Algorithm mvh() {
        if (mvh == null) {
            try {
                mvh = new Algorithm(Configuration.getBaseUri().augment("algorithm", "mvh"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("mvh", "Missing Value Handling Algorithm", "Simple MVH Filter").
                        addComment("You can also use this algorithm from withing the proxy service at " + Configuration.BASE_URI + "/algorithm/multifilter").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Filter", "Data Preprocessing", "Missing Values", "MVH", "Data Preparation", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Replaces missing values in the dataset with new ones, leading into a dense dataset using the means-and-modes approach. This "
                        + "action will definitely have effect on the reliability of any model created with the dataset as these values are actually 'guessed' and might "
                        + "strongly divert from the actual ones.").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(mvh.getUri().toString());
                Parameter ignoreUriParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "ignore_uri"), "ignoreUri", new LiteralValue<URI>(null, XSDDatatype.XSDanyURI)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                ignoreUriParam.getMeta().addComment("You can specify multiple URIs to be ignored");
                mvh.setParameters(new HashSet<Parameter>());
                mvh.getParameters().add(ignoreUriParam);

                mvh.setMeta(algorithmMeta);
                mvh.setOntologies(new HashSet<OntologicalClass>());
                mvh.getOntologies().add(OTAlgorithmTypes.preprocessing());
                mvh.getMeta().addRights(_LICENSE);
                mvh.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return mvh;
    }

    public static Algorithm plsFilter() {
        if (plsFilter == null) {
            try {
                plsFilter = new Algorithm(Configuration.getBaseUri().augment("algorithm", "pls"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("pls", "Partial Least Squares Filter", "PLS Dataset Preprocessing").
                        addComment("You can also use this algorithm from withing the proxy service at " + Configuration.BASE_URI + "/algorithm/multifilter").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Filter", "Data Preprocessing", "PLS", "Data Preparation", "Weka").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Applies the PLS algorithm on the data and removes some features from the dataset. PLS is a standard, widely used "
                        + "supervised algorithm for dimension reduction on datasets.").
                        addPublisher(Configuration.BASE_URI).
                        setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(plsFilter.getUri().toString());
                plsFilter.setParameters(new HashSet<Parameter>());

                Parameter numComponents =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "numComponents"), "numComponents", new LiteralValue(1, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.MANDATORY);
                numComponents.getMeta().addDescription("The maximum number of attributes(features) to use").
                        addComment("The number of components must be less than the number of independent features in the dataset");
                plsFilter.getParameters().add(numComponents);

                Parameter plsAlgorithm =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "plsAlgorithm"), "algorithm", new LiteralValue<String>("PLS1")).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                plsAlgorithm.getMeta().addDescription("The type of algorithm to use").addComment("Admissible values are PLS1 and SIMPLS");
                plsFilter.getParameters().add(plsAlgorithm);

                Parameter plsPreprocessing =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "plsPreprocessing"), "preprocessing", new LiteralValue<String>("center")).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                plsPreprocessing.getMeta().addDescription("Preprocessing on the provided data prior to the application of the PLS algorithm").
                        addComment("Admissible values are 'none', 'center' and 'standardize'. Default is 'none'");
                plsFilter.getParameters().add(plsPreprocessing);


                Parameter doUpdateClass =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "doUpdateClass"), "doUpdateClass", new LiteralValue<String>("off")).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                doUpdateClass.getMeta().addDescription("Whether the target feature should be updated. The target feature is specified using the "
                        + "mandatory parameter 'target'.").
                        addComment(" Admissible values are 'on' and 'off'. Default is 'off'.");
                plsFilter.getParameters().add(doUpdateClass);



                Parameter target =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "plsTarget"), "target", new LiteralValue<String>("")).setScope(
                        Parameter.ParameterScope.MANDATORY);
                target.getMeta().addDescription("URI of the target/class feature of the dataset with the respect to which PLS runs").
                        addComment("This is different from the parameter prediction_feature. This is not stort of a dependent feature "
                        + "since this is a filtering algorithm and it does not generate a predictive model but transforms the submitted "
                        + "dataset").addComment("The value should be a URI among the features of the sumbitted dataset");
                plsFilter.getParameters().add(target);

                plsFilter.setMeta(algorithmMeta);
                plsFilter.setOntologies(new HashSet<OntologicalClass>());
                plsFilter.getOntologies().add(OTAlgorithmTypes.preprocessing());
                plsFilter.getOntologies().add(OTAlgorithmTypes.supervised());
                plsFilter.getMeta().addRights(_LICENSE);
                plsFilter.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return plsFilter;
    }

    public static Algorithm svm() {
        if (svm == null) {
            try {
                svm = new Algorithm(Configuration.getBaseUri().augment("algorithm", "svm"));
                MetaInfo algorithmMeta = new MetaInfoImpl().addTitle("svm", "Support Vector Machine Training Algorithm").
                        addComment("Support Vector Machine Algorithm").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addSubject("Regression", "Training", "Machine Learning", "Single Target", "Eager Learning", "Weka", "SVM").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("Algorithm for training regression models using the Support Vector Machine Learning Algorithm. "
                        + "The training is based on the Weka implementation of SVM and specifically the class weka.classifiers.functions.SVMreg. "
                        + "A comprehensive introductory text is provided by John Shawe-Taylor and Nello Cristianin in the book 'Support Vector Machines' "
                        + "Cambridge University Press, 2000").
                        addPublisher(Configuration.BASE_URI).setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis()))).
                        addIdentifier(svm.getUri().toString());
                svm.setParameters(new HashSet<Parameter>());

                Parameter kernel =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "svm_kernel"), "kernel", new LiteralValue<String>("RBF")).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                kernel.getMeta().addDescription("Kernel of the Support Vector Machine. Available kernels include 'rbf', 'linear' and 'polynomial'.").
                        addIdentifier(kernel.getUri().toString());
                svm.getParameters().add(kernel);

                Parameter gamma = new Parameter(Configuration.getBaseUri().augment("prm", "svm_gamma"));
                gamma.setName("gamma").setScope(Parameter.ParameterScope.OPTIONAL);
                gamma.setTypedValue(new LiteralValue<Double>(1.5d));
                gamma.getMeta().
                        addDescription("Gamma Parameter for the SVM kernel").
                        addComment("Only strictly positive values are acceptable").
                        addIdentifier(gamma.getUri().toString());
                svm.getParameters().add(gamma);

                Parameter cost = new Parameter(Configuration.getBaseUri().augment("prm", "svm_cost"));
                cost.setName("cost").setScope(Parameter.ParameterScope.OPTIONAL);
                cost.setTypedValue(new LiteralValue<Double>(100.0d));
                cost.getMeta().addComment("Only strictly positive values are acceptable").
                        addIdentifier(cost.getUri().toString());
                svm.getParameters().add(cost);

                Parameter epsilon = new Parameter(Configuration.getBaseUri().augment("prm", "svm_epsilon"));
                epsilon.setName("epsilon").setScope(Parameter.ParameterScope.OPTIONAL);
                epsilon.setTypedValue(new LiteralValue<Double>(0.1d));
                epsilon.getMeta().addComment("Only strictly positive values are acceptable").
                        addIdentifier(epsilon.getUri().toString());
                svm.getParameters().add(epsilon);

                Parameter tolerance = new Parameter(Configuration.getBaseUri().augment("prm", "svm_tolerance"));
                tolerance.setName("tolerance").setScope(Parameter.ParameterScope.OPTIONAL);
                tolerance.setTypedValue(new LiteralValue<Double>(0.0001d));
                tolerance.getMeta().addComment("Only strictly positive values are acceptable and we advise users to use values that "
                        + "do not exceed 0.10").
                        addIdentifier(tolerance.getUri().toString());
                svm.getParameters().add(tolerance);

                Parameter degree = new Parameter(Configuration.getBaseUri().augment("prm", "svm_degree"));
                degree.setName("degree").setScope(Parameter.ParameterScope.OPTIONAL);
                degree.setTypedValue(new LiteralValue<Integer>(3));
                degree.getMeta().addDescription("Degree of polynomial kernel").
                        addComment("To be used in combination with the polynomial kernel").
                        addIdentifier(degree.getUri().toString());
                svm.getParameters().add(degree);
                /*
                Parameter scalingParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "svm_scaling"), "scaling", new LiteralValue(1, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingParam.getMeta().addDescription("Set scaling enabled");
                svm.getParameters().add(scalingParam);
                
                Parameter scalingMinParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "svm_scaling_min"), "scaling_min", new LiteralValue(3, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingMinParam.getMeta().addDescription("Set scaling min");
                svm.getParameters().add(scalingMinParam);
                
                Parameter scalingMaxParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "svm_scaling_max"), "scaling_max", new LiteralValue(3, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                scalingMaxParam.getMeta().addDescription("Set scaling max");
                svm.getParameters().add(scalingMaxParam);
                
                Parameter normalizationParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "svm_normalization"), "normalization", new LiteralValue(1, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                normalizationParam.getMeta().addDescription("Set normalization enabled");
                svm.getParameters().add(normalizationParam);
                */
                
                Parameter missingValHandlingParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "svm_mvh"), "mvh", new LiteralValue(0, XSDDatatype.XSDint)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                missingValHandlingParam.getMeta().addDescription("Set missing value handling enabled");
                svm.getParameters().add(missingValHandlingParam);
                
                Parameter missingValHandlingIgnoreUriParam =
                        new Parameter(
                        Configuration.getBaseUri().augment("prm", "svm_mvh_ignore_uri"), "mvh_ignore_uri", new LiteralValue<URI>(null, XSDDatatype.XSDanyURI)).setScope(
                        Parameter.ParameterScope.OPTIONAL);
                missingValHandlingIgnoreUriParam.getMeta().addComment("You can specify multiple URIs to be ignored");
                svm.getParameters().add(missingValHandlingIgnoreUriParam);
                
                
                svm.setMeta(algorithmMeta);
                svm.setOntologies(new HashSet<OntologicalClass>());
                svm.getOntologies().add(OTAlgorithmTypes.regression());
                svm.getOntologies().add(OTAlgorithmTypes.singleTarget());
                svm.getOntologies().add(OTAlgorithmTypes.eagerLearning());
                svm.getMeta().addRights(_LICENSE);
                svm.setEnabled(true);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return svm;
    }

    public static Algorithm leverages() {
        if (leverages == null) {
            try {
                leverages = new Algorithm(Configuration.getBaseUri().augment("algorithm", "leverages"));
                MetaInfo algorithmMeta = new MetaInfoImpl();
                algorithmMeta.addTitle("leverages");
                algorithmMeta.addTitle("Leverages DoA Algorithm");
                algorithmMeta.addSubject("Domain of Applicability", "Model Applicability Domain", "DoA", "MAD").
                        addContributor("Pantelis Sopasakis", "Charalampos Chomenides").
                        addDescription("The well known leverages algorithm for the estimation of a model's applicability domain").
                        addComment("For example cURL commands for this algorithm check out http://cut.gd/P6fa").
                        addPublisher(Configuration.BASE_URI).setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis())));
                leverages.setMeta(algorithmMeta);
                leverages.setOntologies(new HashSet<OntologicalClass>());
                leverages.getOntologies().add(OTAlgorithmTypes.applicabilityDomain());
                leverages.getOntologies().add(OTAlgorithmTypes.singleTarget());
                leverages.getOntologies().add(OTAlgorithmTypes.eagerLearning());
                leverages.getMeta().addRights(_LICENSE);
            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return leverages;
    }

    public static Algorithm modelBundler() {
        if (modelBundler == null) {
            try {
                modelBundler = new Algorithm(Configuration.getBaseUri().augment("algorithm", "modelBundler"));
                MetaInfo algorithmMeta = new MetaInfoImpl();
                algorithmMeta.addTitle("Model Bundler");
                algorithmMeta.addSubject("Model Super-service").
                        addContributor("Pantelis Sopasakis").
                        addDescription("...").
                        addPublisher(Configuration.BASE_URI).setDate(new LiteralValue<Date>(new Date(System.currentTimeMillis())));
                modelBundler.setMeta(algorithmMeta);
                modelBundler.setOntologies(new HashSet<OntologicalClass>());
                modelBundler.getOntologies().add(OTAlgorithmTypes.algorithmType());
                modelBundler.getOntologies().add(OTAlgorithmTypes.singleTarget());
                modelBundler.getMeta().addRights(_LICENSE);

                modelBundler.setParameters(new HashSet<Parameter>());
                Parameter model = new Parameter(Configuration.getBaseUri().augment("prm", "model"));
                model.setName("model").setScope(Parameter.ParameterScope.MANDATORY);
                model.getMeta().addDescription("Model").
                        addIdentifier(model.getUri().toString());
                modelBundler.getParameters().add(model);

            } catch (ToxOtisException ex) {
                throw new RuntimeException(ex);
            }
        }
        return modelBundler;
    }
}
