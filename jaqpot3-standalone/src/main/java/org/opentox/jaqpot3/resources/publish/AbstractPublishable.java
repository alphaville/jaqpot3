package org.opentox.jaqpot3.resources.publish;

import org.restlet.data.MediaType;
/**
 *
 * @author Pantelis Sopasakis
 * @author Charalampos Chomenides
 */
public abstract class AbstractPublishable implements Publishable{

    private MediaType media;

    public AbstractPublishable() {
    }
    

    @Override
    public MediaType getMediaType() {
        return media;
    }

    @Override
    public void setMediaType(MediaType media) {
        this.media = media;
    }



}