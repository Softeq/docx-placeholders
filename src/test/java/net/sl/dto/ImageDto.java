package net.sl.dto;

import java.io.InputStream;

/**
 * <p/>
 * Created on 1/4/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class ImageDto {
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

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public InputStream getSourceStream() {
        return sourceStream;
    }

    public void setSourceStream(InputStream sourceStream) {
        this.sourceStream = sourceStream;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
