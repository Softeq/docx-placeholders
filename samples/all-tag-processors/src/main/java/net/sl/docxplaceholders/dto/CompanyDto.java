package net.sl.docxplaceholders.dto;

/**
 * <p/>
 * Created on 2/3/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class CompanyDto {
    private String name;
    private SiteDto site;

    public CompanyDto(String name, SiteDto site) {
        this.name = name;
        this.site = site;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SiteDto getSite() {
        return site;
    }

    public void setSite(SiteDto site) {
        this.site = site;
    }
}
