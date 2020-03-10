package net.sl.docxplaceholders;

import net.sl.docxplaceholders.dto.AvatarDto;
import net.sl.docxplaceholders.dto.CompanyDto;
import net.sl.docxplaceholders.dto.DeveloperDto;
import net.sl.docxplaceholders.dto.LanguageDto;
import net.sl.docxplaceholders.dto.ProjectDto;
import net.sl.docxplaceholders.dto.SiteDto;
import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.processor.ImageTagProcessor;
import net.sl.docxplaceholders.processor.LinkTagProcessor;
import net.sl.docxplaceholders.processor.MapTagProcessor;
import net.sl.docxplaceholders.processor.PojoCollectionTagProcessor;
import net.sl.docxplaceholders.processor.PojoFieldTagProcessor;
import net.sl.docxplaceholders.processor.PojoNestedBlockTagProcessor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AllTagProcessors {

    public static void main(String[] args) {
        //read template stream from resource
        //the template contains ${{firstName}} and ${{lastName}} placeholders
        try (InputStream templateIs = AllTagProcessors.class.getResourceAsStream("/net/sl/docxplaceholders/All-tag-processors-template.docx");
             //create target stream to store filled template
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {
            //create placeholders map placing values for the ${{firstName}} and ${{lastName}} placeholders
            Map<String, String> placeholdersValuesMap = new HashMap<>();
            placeholdersValuesMap.put("key", "value");

            //create context
            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Arrays.asList(
                    new PojoCollectionTagProcessor(), //to process collections
                    new PojoNestedBlockTagProcessor(), //to process nested POJO
                    new PojoFieldTagProcessor(), //to process POJO fields
                    new LinkTagProcessor(), //to process links
                    new ImageTagProcessor(), //to process images
                    new MapTagProcessor(placeholdersValuesMap) //to process any tag by key value
            ));
            context.push(null, getAllTagsPojo());

            //create filler instance
            DocxTemplateFiller filler = new DocxTemplateFiller();
            //fill the template from the source stream storing values to the output stream
            //filling logic is based on the context processors
            filler.fillTemplate(templateIs, filledTemplateOs, context);
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            ex.printStackTrace();
        }
    }

    private static Object getAllTagsPojo() {
        DeveloperDto dev = new DeveloperDto();
        dev.setFirstName("Stanislav");
        dev.setLastName("Lapitsky");
        dev.setCompany(new CompanyDto("Softeq", new SiteDto("Softeq", "https://softeq.by", "ff0000")));
        dev.setAvatar(new AvatarDto("Avatar", "image/jpeg", "/net/sl/docxplaceholders/avatar.jpg", 100, 100));
        dev.setSite(new SiteDto("Github", "https://github.com", "0000ff"));
        dev.setLanguages(Stream.of("Java", "SQL")
                .map(s -> new LanguageDto(s))
                .collect(Collectors.toList()));
        ProjectDto docxPlaceholders = new ProjectDto();
        docxPlaceholders.setName("Docx-placeholders");
        ProjectDto test = new ProjectDto();
        test.setName("Test");
        dev.setProjects(Arrays.asList(docxPlaceholders, test));
        return dev;
    }
}