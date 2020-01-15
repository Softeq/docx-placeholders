package net.sl.docxplaceholders.tag;

/**
 * The interface defines getters necessary to represent link. When link placeholder is filled th link attributes
 * by the values returned from the methods declared.
 * <p/>
 * Created on 1/9/2020.
 * <p/>
 *
 * @author slapitsky
 */
public interface TagLinkData {
    String getText();

    String getUrl();

    String getColor();
}
