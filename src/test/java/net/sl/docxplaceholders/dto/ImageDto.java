package net.sl.docxplaceholders.dto;

import net.sl.docxplaceholders.tag.TagImageData;

import java.io.InputStream;

/**
 * <p/>
 * Created on 1/4/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class ImageDto implements TagImageData {
    private String title;
    private String contentType;
    private InputStream sourceStream;
    private Integer width;
    private Integer height;

    public ImageDto(String title, InputStream sourceStream, String contentType, int width, int height) {
        this.title = title;
        this.contentType = contentType;
        this.sourceStream = sourceStream;
        this.width = width;
        this.height = height;
    }

    public String getTitle() {
        return title;
    }

    public String getContentType() {
        return contentType;
    }

    public InputStream getSourceStream() {
        return sourceStream;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

}
