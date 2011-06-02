package org.opentox.jaqpot3.qsar.serializable;

import java.io.Serializable;
import weka.filters.supervised.attribute.PLSFilter;

/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public class PLSModel implements Serializable {

    private static final long serialVersionUID = 941376210178L;

    private final PLSFilter pls;

    public PLSModel(PLSFilter pls) {
        this.pls = pls;
    }

    public PLSFilter getPls() {
        return pls;
    }
}
