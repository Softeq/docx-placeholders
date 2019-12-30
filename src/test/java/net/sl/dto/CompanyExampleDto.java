package net.sl.dto;

import java.util.List;

/**
 * Example DTO to be used in tests.
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class CompanyExampleDto {
    private String companyName;

    private List<CompanyProjectDto> projects;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public List<CompanyProjectDto> getProjects() {
        return projects;
    }

    public void setProjects(List<CompanyProjectDto> projects) {
        this.projects = projects;
    }
}
