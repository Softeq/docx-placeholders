package net.sl.docxplaceholders.dto;

import net.sl.docxplaceholders.tag.TagLinkData;

/**
 * Created on 2/3/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class SiteDto implements TagLinkData {
    private String text;
    private String url;
    private String color;

    public SiteDto(String text, String url, String color) {
        this.text = text;
        this.url = url;
        this.color = color;
    }

    @Override
    public String getText() {
        return text;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getColor() {
        return color;
    }

    public void setText(String text) {
        this.text = text;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
