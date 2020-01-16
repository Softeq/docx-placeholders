package net.sl.docxplaceholders;

import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.processor.MapTagProcessor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Simple {
    public static void main(String[] args) {
        //read template stream from resource
        //the template contains ${{firstName}} and ${{lastName}} placeholders
        try (InputStream templateIs = Simple.class.getResourceAsStream("/net/sl/docxplaceholders/Map-simple-template.docx");
             //create target stream to store filled template
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {
            //create placeholders map placing values for the ${{firstName}} and ${{lastName}} placeholders
            Map<String, String> placeholdersValuesMap = new HashMap<>();
            placeholdersValuesMap.put("firstName", "John");
            placeholdersValuesMap.put("lastName", "Smith");

            //create context
            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            //define just single simple Map Tag processor which finds tags by names and replaces them with values from
            //the map we pass
            context.setProcessors(Collections.singletonList(new MapTagProcessor(placeholdersValuesMap)));

            //create filler instance
            DocxTemplateFiller filler = new DocxTemplateFiller();
            //fill the template from the source stream storing values to the output stream
            //filling logic is based on the context processors
            filler.fillTemplate(templateIs, filledTemplateOs, context);

            System.out.println("Filled template size = " + filledTemplateOs.size());
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            ex.printStackTrace();
        }
    }
}