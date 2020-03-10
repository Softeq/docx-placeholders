package net.sl.docxplaceholders.dto;

import java.util.List;

/**
 * Created on 2/6/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class CompanyContainerDto {
    private List<CompanyExampleDto> companies;

    public List<CompanyExampleDto> getCompanies() {
        return companies;
    }

    public void setCompanies(List<CompanyExampleDto> companies) {
        this.companies = companies;
    }
}
