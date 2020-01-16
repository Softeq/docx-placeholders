package net.sl.docxplaceholders;

import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.processor.MapTagProcessor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p/>
 * Created on 01/16/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class TagInHeaderFooterTest {

    private DocxTemplateFiller filler = new DocxTemplateFiller();

    @Test
    public void testMapValuesInHeaderFooter() {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/docxplaceholders/TagInHeaderFooter-template.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {
            Map<String, String> placeholdersValuesMap = new HashMap<String, String>() {{
                put("firstName", "John");
                put("lastName", "Smith");
            }};
            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Collections.singletonList(new MapTagProcessor(placeholdersValuesMap)));
            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());

            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                Assert.assertTrue(doc.getBodyElements().get(0) instanceof XWPFParagraph);
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(0);
                Assert.assertTrue(par.getText().contains("John"));
                Assert.assertTrue(par.getText().contains("Smith"));

                XWPFHeader header = doc.getHeaderList().get(0);
                Assert.assertTrue(header.getBodyElements().get(0) instanceof XWPFParagraph);
                par = (XWPFParagraph) header.getBodyElements().get(0);
                Assert.assertTrue(par.getText().contains("John"));
                Assert.assertTrue(par.getText().contains("Smith"));

                XWPFFooter footer = doc.getFooterList().get(0);
                Assert.assertTrue(footer.getBodyElements().get(0) instanceof XWPFParagraph);
                par = (XWPFParagraph) footer.getBodyElements().get(0);
                Assert.assertTrue(par.getText().contains("John"));
                Assert.assertTrue(par.getText().contains("Smith"));
            }
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            Assert.fail();
        }
    }
}
