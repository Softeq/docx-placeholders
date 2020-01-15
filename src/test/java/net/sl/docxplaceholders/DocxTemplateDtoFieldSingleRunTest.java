package net.sl.docxplaceholders;

import net.sl.docxplaceholders.dto.CompanyExampleDto;
import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.processor.MapTagProcessor;
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
import java.util.Collections;

/**
 * <p/>
 * Created on 10/3/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateDtoFieldSingleRunTest {

    private DocxTemplateFiller filler = new DocxTemplateFiller();

    @Test
    public void testFillingFromDto() {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/docxplaceholders/Placeholders-dto-value-single-run-template.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {
            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setTagStart("B");
            context.setTagEnd("E");
            context.setProcessors(Collections.singletonList(new MapTagProcessor(Collections.singletonMap("key", "Value"))));
            context.push(null, fillExample());
            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());

            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                Assert.assertTrue(doc.getBodyElements().get(0) instanceof XWPFParagraph);
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(0);
                Assert.assertEquals("bValuea", par.getText());
            }
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            Assert.fail();
        }
    }

    private CompanyExampleDto fillExample() {
        CompanyExampleDto res = new CompanyExampleDto();
        res.setCompanyName("Company");
        return res;
    }

}
