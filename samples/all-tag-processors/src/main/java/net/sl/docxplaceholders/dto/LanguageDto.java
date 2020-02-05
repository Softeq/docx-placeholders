package net.sl.docxplaceholders.dto;

/**
 * <p/>
 * Created on 1/13/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class LanguageDto {
    private String languageName;

    public LanguageDto(String languageName) {
        this.languageName = languageName;
    }

    public String getLanguageName() {
        return languageName;
    }

    public void setLanguageName(String languageName) {
        this.languageName = languageName;
    }
}
