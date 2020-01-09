package net.sl.dto;

import net.sl.tag.TagLinkData;

/**
 * <p/>
 * Created on 1/9/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class LinkHolderDto {
    private TagLinkData linkField;

    public LinkHolderDto(TagLinkData linkField) {
        this.linkField = linkField;
    }

    public TagLinkData getLinkField() {
        return linkField;
    }
}
