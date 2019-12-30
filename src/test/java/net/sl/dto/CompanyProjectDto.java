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
public class CompanyProjectDto
{
    private String projectName;

    private List<DeveloperDto> developers;

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public List<DeveloperDto> getDevelopers()
    {
        return developers;
    }

    public void setDevelopers(List<DeveloperDto> developers)
    {
        this.developers = developers;
    }
}
