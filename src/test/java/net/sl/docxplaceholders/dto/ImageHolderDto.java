package net.sl.docxplaceholders.dto;

import net.sl.docxplaceholders.tag.TagImageData;

/**
 * <p/>
 * Created on 1/9/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class ImageHolderDto {
    private TagImageData imageField;

    public ImageHolderDto(TagImageData imageField) {
        this.imageField = imageField;
    }

    public TagImageData getImageField() {
        return imageField;
    }
}
