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
package org.opentox.jaqpot3.qsar.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import org.opentox.jaqpot3.qsar.exceptions.QSARException;
import weka.core.Attribute;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.Remove;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class AttributeCleanup {

    private boolean keepCompoundURI = false;

    /**
     * An enumeration for the main data-types recognized by weka which
     * are also useful for YAQP. These are <code>string, nominal</code> and
     * <code>numeric</code>.
     */
    public enum AttributeType {

        string,
        nominal,
        numeric;
    }
    private java.util.EnumSet<AttributeType> toBeRemoved;

    /**
     * Constructs a new clean-up filter which removes string attributes
     * (default behaviour).
     */
    public AttributeCleanup() {
        super();
        this.toBeRemoved = java.util.EnumSet.noneOf(AttributeType.class);
    }

    public AttributeCleanup(boolean keepCompoundURI, AttributeType... toBeRemoved) {
        this.keepCompoundURI = keepCompoundURI;
        this.toBeRemoved = java.util.EnumSet.noneOf(AttributeType.class);
        this.toBeRemoved.addAll(Arrays.asList(toBeRemoved));
    }

    public AttributeCleanup(boolean keepCompoundURI, AttributeType toBeRemoved) {
        this.keepCompoundURI = keepCompoundURI;
        this.toBeRemoved = java.util.EnumSet.noneOf(AttributeType.class);
        this.toBeRemoved.add(toBeRemoved);
    }

    public AttributeCleanup(boolean keepCompoundURI, Set<AttributeType> toBeRemoved) {
        this.keepCompoundURI = keepCompoundURI;
        this.toBeRemoved = java.util.EnumSet.noneOf(AttributeType.class);
    }

    public boolean isKeepCompoundURI() {
        return keepCompoundURI;
    }

    public void setKeepCompoundURI(boolean keepCompoundURI) {
        this.keepCompoundURI = keepCompoundURI;
    }

    public Set<AttributeType> getToBeRemoved() {
        return toBeRemoved;
    }

    public void setToBeRemoved(java.util.EnumSet<AttributeType> toBeRemoved) {
        this.toBeRemoved = java.util.EnumSet.allOf(AttributeType.class);
    }

    public void addToBeRemoved(AttributeType type) {
        toBeRemoved.add(type);
    }

    public Instances filter(Instances data) throws QSARException {
        if (toBeRemoved == null || (toBeRemoved != null && toBeRemoved.size() == 0)) {
            this.toBeRemoved = java.util.EnumSet.allOf(AttributeType.class);
            toBeRemoved.add(AttributeType.string);
        }

        return remove(data);
    }

    private Instances remove(Instances input) throws QSARException {
        Remove remove = new Remove();
        ArrayList<Integer> attributeList = new ArrayList<Integer>();

        for (int i = 0; i < input.numAttributes(); i++) {
            Attribute attribute = input.attribute(i);
            System.out.println(attribute.name());
            if ((attribute.name().equals("compound_uri")
                    || attribute.name().equalsIgnoreCase("uri")) && isKeepCompoundURI()) {
                continue;
            } else if (!isKeepCompoundURI()
                    && (attribute.name().equals("compound_uri") || attribute.name().equalsIgnoreCase("uri"))) {
                attributeList.add(i);
            }
            if (attribute.isNominal() && toBeRemoved.contains(AttributeType.nominal)) {
                attributeList.add(i);
                continue;
            } else if (attribute.isString() && toBeRemoved.contains(AttributeType.string)) {
                attributeList.add(i);
                continue;
            } else if (attribute.isNumeric() && toBeRemoved.contains(AttributeType.numeric)) {
                attributeList.add(i);
                continue;
            }
        }
        int[] attributeIndices = new int[attributeList.size()];
        for (int i = 0; i < attributeList.size(); i++) {
            attributeIndices[i] = attributeList.get(i).intValue();
        }
        remove.setAttributeIndicesArray(attributeIndices);
        try {
            remove.setInputFormat(input);
        } catch (Exception ex) {
            throw new QSARException("FilteringError: Invalid input format "
                    + "for attribute-type removing filter", ex);
        }
        Instances output;
        try {
            output = Remove.useFilter(input, remove);
        } catch (Exception ex) {
            throw new QSARException("FilteringError: The filter is unable to "
                    + "remove the specified types :" + toBeRemoved.toString(), ex);
        }
        return output;


    }
}
