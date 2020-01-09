package net.sl.dto;

import net.sl.tag.TagLinkData;

/**
 * <p/>
 * Created on 1/4/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class LinkDto implements TagLinkData {
    private String text;
    private String url;
    private String color;

    public LinkDto(String text, String url, String color) {
        this.text = text;
        this.url = url;
        this.color = color;
    }

    public String getText() {
        return text;
    }

    public String getUrl() {
        return url;
    }

    public String getColor() {
        return color;
    }

}
