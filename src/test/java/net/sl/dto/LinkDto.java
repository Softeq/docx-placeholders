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

    public LinkDto(String linkText, String linkUrl) {
        this.linkText = linkText;
        this.linkUrl = linkUrl;
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
}
