package net.sl.tag;

import java.io.InputStream;

/**
 * The interface defines getters necessary to represent image. When image placeholder is filled the image attributes
 * returned from the methods declared are used.
 * <p/>
 * Created on 1/9/2020.
 * <p/>
 *
 * @author slapitsky
 */
public interface TagImageData {
    String getTitle();

    String getContentType();

    InputStream getSourceStream();

    Integer getWidth();

    Integer getHeight();
}
