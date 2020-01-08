package net.sl.dto;

/**
 * <p/>
 * Created on 1/4/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class LinkDto {
    private String linkText;
    private String linkUrl;
    private String linkColor;

    public LinkDto(String linkText, String linkUrl, String linkColor) {
        this.linkText = linkText;
        this.linkUrl = linkUrl;
        this.linkColor = linkColor;
    }

    public String getLinkText() {
        return linkText;
    }

    public void setLinkText(String linkText) {
        this.linkText = linkText;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public String getLinkColor() {
        return linkColor;
    }

    public void setLinkColor(String linkColor) {
        this.linkColor = linkColor;
    }
}
