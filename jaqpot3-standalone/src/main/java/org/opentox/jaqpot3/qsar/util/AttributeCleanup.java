package org.opentox.jaqpot3.qsar.util;

import java.util.ArrayList;
import java.util.HashSet;
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
     * An enumeration for the main datatypes recognized by weka which
     * are also useful for YAQP. These are <code>string, nominal</code> and
     * <code>numeric</code>.
     */
    public enum ATTRIBUTE_TYPE {

        string,
        nominal,
        numeric;
    }
    private Set<ATTRIBUTE_TYPE> toBeRemoved;

    /**
     * Constructs a new clean-up filter which removes string attributes
     * (default behaviour).
     */
    public AttributeCleanup() {
        super();
        this.toBeRemoved = new HashSet<ATTRIBUTE_TYPE>();
    }

    public AttributeCleanup(boolean keepCompoundURI, ATTRIBUTE_TYPE... toBeRemoved) {
        this.keepCompoundURI = keepCompoundURI;
        this.toBeRemoved = new HashSet<ATTRIBUTE_TYPE>();
        for (ATTRIBUTE_TYPE type : toBeRemoved) {
            this.toBeRemoved.add(type);
        }
    }

    public AttributeCleanup(boolean keepCompoundURI, ATTRIBUTE_TYPE toBeRemoved) {
        this.keepCompoundURI = keepCompoundURI;
        this.toBeRemoved = new HashSet<ATTRIBUTE_TYPE>();
        this.toBeRemoved.add(toBeRemoved);
    }

    public AttributeCleanup(boolean keepCompoundURI, Set<ATTRIBUTE_TYPE> toBeRemoved) {
        this.keepCompoundURI = keepCompoundURI;
        this.toBeRemoved = toBeRemoved;
    }

    public boolean isKeepCompoundURI() {
        return keepCompoundURI;
    }

    public void setKeepCompoundURI(boolean keepCompoundURI) {
        this.keepCompoundURI = keepCompoundURI;
    }

    public Set<ATTRIBUTE_TYPE> getToBeRemoved() {
        return toBeRemoved;
    }

    public void setToBeRemoved(Set<ATTRIBUTE_TYPE> toBeRemoved) {
        this.toBeRemoved = toBeRemoved;
    }

    public void addToBeRemoved(ATTRIBUTE_TYPE type) {
        toBeRemoved.add(type);
    }

    public Instances filter(Instances data) throws QSARException {
        if (toBeRemoved == null || (toBeRemoved != null && toBeRemoved.size() == 0)) {
            this.toBeRemoved = new HashSet<ATTRIBUTE_TYPE>();
            toBeRemoved.add(ATTRIBUTE_TYPE.string);
        }

        return remove(data);
    }

    private Instances remove(Instances input) throws QSARException {
        Remove remove = new Remove();
        ArrayList<Integer> attributeList = new ArrayList<Integer>();

        int j = 0;
        for (int i = 0; i < input.numAttributes(); i++) {
            Attribute attribute = input.attribute(i);
            if (attribute.isNominal() && toBeRemoved.contains(ATTRIBUTE_TYPE.nominal)) {
                attributeList.add(i);
                continue;
            } else if (attribute.isString() && toBeRemoved.contains(ATTRIBUTE_TYPE.string)) {               
                if (attribute.name().equals("compound_uri") && isKeepCompoundURI()) {
                    continue;
                } 
                    attributeList.add(i);
                    continue;               
            } else if (attribute.isNumeric() && toBeRemoved.contains(ATTRIBUTE_TYPE.numeric)) {
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
            throw new QSARException("FilteringError: Invalid input format for attribute-type removing filter", ex);
        }
        Instances output;
        try {
            output = Remove.useFilter(input, remove);
        } catch (Exception ex) {
            throw new QSARException("FilteringError: The filter is unable to remove the specified types :" + toBeRemoved.toString(), ex);
        }
        return output;


    }
}
