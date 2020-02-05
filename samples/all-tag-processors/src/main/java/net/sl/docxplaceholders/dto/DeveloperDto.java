package net.sl.docxplaceholders.dto;

import java.util.List;

/**
 * <p/>
 * Created on 1/16/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class DeveloperDto {
    private String firstName;
    private String lastName;
    private SiteDto site;
    private AvatarDto avatar;
    private List<LanguageDto> languages;
    private CompanyDto company;
    private List<ProjectDto> projects;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public List<LanguageDto> getLanguages() {
        return languages;
    }

    public void setLanguages(List<LanguageDto> languages) {
        this.languages = languages;
    }

    public CompanyDto getCompany() {
        return company;
    }

    public void setCompany(CompanyDto company) {
        this.company = company;
    }

    public List<ProjectDto> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectDto> projects) {
        this.projects = projects;
    }

    public SiteDto getSite() {
        return site;
    }

    public void setSite(SiteDto site) {
        this.site = site;
    }

    public AvatarDto getAvatar() {
        return avatar;
    }

    public void setAvatar(AvatarDto avatar) {
        this.avatar = avatar;
    }
}
