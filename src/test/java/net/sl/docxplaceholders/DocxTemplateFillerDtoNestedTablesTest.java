package net.sl.docxplaceholders;

import net.sl.docxplaceholders.dto.CompanyExampleDto;
import net.sl.docxplaceholders.dto.CompanyProjectDto;
import net.sl.docxplaceholders.dto.DeveloperDto;
import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.processor.PojoCollectionTagProcessor;
import net.sl.docxplaceholders.processor.PojoFieldTagProcessor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Test filling from DTO (template with nested tables).
 * <p/>
 * Created on 10/3/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateFillerDtoNestedTablesTest
{

    private DocxTemplateFiller filler = new DocxTemplateFiller();

    @Test
    public void testFillingFromDto()
    {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/docxplaceholders/Placeholders-dto-value-template-nested-collections.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {
            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Arrays.asList(new PojoCollectionTagProcessor(), new PojoFieldTagProcessor()));
            context.push(null, fillExample());
            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());

            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(0);
                Assert.assertEquals("Company: TestCompany", par.getText());
                Assert.assertEquals(4, doc.getTables().size());
            }
        }
        catch (IOException | InvalidFormatException | DocxTemplateFillerException ex)
        {
            Assert.fail();
        }
    }

    private CompanyExampleDto fillExample()
    {
        CompanyExampleDto res = new CompanyExampleDto();
        res.setCompanyName("TestCompany");
        res.setProjects(Arrays.asList(
                fillCompanyProjectExample("Project One", 2),
                fillCompanyProjectExample("Project Two", 3),
                fillCompanyProjectExample("Project Three ", 0)
        ));
        return res;
    }

    private CompanyProjectDto fillCompanyProjectExample(String projectName, int devCount)
    {
        CompanyProjectDto res = new CompanyProjectDto();
        res.setProjectName(projectName);
        res.setDevelopers(new ArrayList<>());
        for (int i = 0; i < devCount; i++)
        {
            DeveloperDto devDto = new DeveloperDto();
            devDto.setFirstName("FirstName" + i);
            devDto.setLastName("LastName" + i);
            devDto.setLanguage(i % 2 == 0 ? "Java" : "C++");
            res.getDevelopers().add(devDto);
        }
        return res;
    }
}
