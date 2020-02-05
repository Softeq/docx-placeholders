package net.sl.docxplaceholders.dto;

import net.sl.docxplaceholders.tag.TagImageData;

import java.io.InputStream;

/**
 * Created on 2/3/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class AvatarDto implements TagImageData {
    private String title;
    private String contentType;
    private String avatarResourcePath;
    private Integer width;
    private Integer height;

    public AvatarDto(String title, String contentType, String avatarResourcePath, Integer width, Integer height) {
        this.title = title;
        this.contentType = contentType;
        this.avatarResourcePath = avatarResourcePath;
        this.width = width;
        this.height = height;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public InputStream getSourceStream() {
        return getClass().getResourceAsStream(avatarResourcePath);
    }

    @Override
    public Integer getWidth() {
        return width;
    }

    @Override
    public Integer getHeight() {
        return height;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getAvatarResourcePath() {
        return avatarResourcePath;
    }

    public void setAvatarResourcePath(String avatarResourcePath) {
        this.avatarResourcePath = avatarResourcePath;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }
}
