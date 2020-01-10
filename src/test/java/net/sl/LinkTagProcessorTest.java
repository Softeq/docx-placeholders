package net.sl;

import net.sl.dto.LinkDto;
import net.sl.dto.LinkHolderDto;
import net.sl.exception.DocxTemplateFillerException;
import net.sl.processor.LinkTagProcessor;
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
public class LinkTagProcessorTest {

    private DocxTemplateFiller filler = new DocxTemplateFiller();

    @Test
    public void testFillingRoot() {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/LinkTagProcessorTest-template.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {

            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Collections.singletonList(new LinkTagProcessor()));
            LinkDto linkData = new LinkDto("Github", "https://github.com", "0000FF");
            context.push(null, linkData);

            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());

            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                Assert.assertTrue(doc.getBodyElements().get(0) instanceof XWPFParagraph);
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(0);
                Assert.assertTrue(par.getText().contains("Github"));
            }
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testFillingField() {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/LinkTagProcessorTest-fieldRef-template.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {

            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Collections.singletonList(new LinkTagProcessor()));
            final LinkDto linkData = new LinkDto("Stackoverflow", "https://stackoverflow.com", "FF0000");
            context.push(null, new LinkHolderDto(linkData));

            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());


            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                Assert.assertTrue(doc.getBodyElements().get(0) instanceof XWPFParagraph);
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(0);
                Assert.assertTrue(par.getText().contains("Stackoverflow"));
            }
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

}
