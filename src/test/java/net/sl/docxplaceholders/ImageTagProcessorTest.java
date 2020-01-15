package net.sl.docxplaceholders;

import net.sl.docxplaceholders.dto.ImageDto;
import net.sl.docxplaceholders.dto.ImageHolderDto;
import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.processor.ImageTagProcessor;
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
 * Created on 08/01/2020.
 * <p/>
 *
 * @author slapitsky
 */
public class ImageTagProcessorTest {

    private DocxTemplateFiller filler = new DocxTemplateFiller();

    @Test
    public void testFillingRoot() {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/docxplaceholders/ImageTagProcessorTest-template.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {

            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Collections.singletonList(new ImageTagProcessor()));
            ImageDto imageData = new ImageDto("Test Image", getClass().getResourceAsStream("/net/sl/docxplaceholders/image/lightning.jpg"), "jpeg", 200, 100);
            context.push(null, imageData);

            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());


            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                Assert.assertTrue(doc.getBodyElements().get(0) instanceof XWPFParagraph);
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(0);
                Assert.assertTrue(par.getRuns().stream()
                        .anyMatch(r -> !r.getEmbeddedPictures().isEmpty()));
            }
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void testFillingField() {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/docxplaceholders/ImageTagProcessorTest-fieldRef-template.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {

            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Collections.singletonList(new ImageTagProcessor()));
            ImageDto imageData = new ImageDto("Heart Image", getClass().getResourceAsStream("/net/sl/docxplaceholders/image/heart.png"), "png", 200, 100);
            context.push(null, new ImageHolderDto(imageData));

            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());

            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                Assert.assertTrue(doc.getBodyElements().get(0) instanceof XWPFParagraph);
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(0);
                Assert.assertTrue(par.getRuns().stream()
                        .anyMatch(r -> !r.getEmbeddedPictures().isEmpty()));
            }
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }

}
