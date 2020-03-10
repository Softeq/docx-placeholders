package net.sl.docxplaceholders;

import net.sl.docxplaceholders.dto.CompanyContainerDto;
import net.sl.docxplaceholders.dto.CompanyExampleDto;
import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.processor.LinkTagProcessor;
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
import java.util.Arrays;
import java.util.Collections;

/**
 * <p/>
 * Created on 10/3/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class CopyLinkTest {

    private DocxTemplateFiller filler = new DocxTemplateFiller();

    @Test
    public void testCopyLink() {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/docxplaceholders/CopyLinkTest-template.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {

            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Arrays.asList(new PojoCollectionTagProcessor(), new PojoFieldTagProcessor(), new LinkTagProcessor()));
            CompanyExampleDto dto = new CompanyExampleDto();
            dto.setCompanyName("test");

            CompanyContainerDto root = new CompanyContainerDto();
            root.setCompanies(Collections.singletonList(dto));
            context.push(null, root);

            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());

            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                Assert.assertTrue(doc.getBodyElements().get(1) instanceof XWPFParagraph);
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(1);
                Assert.assertTrue(par.getText().contains("google"));
            }
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            ex.printStackTrace();
            Assert.fail();
        }
    }
}
