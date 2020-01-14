package net.sl;

import net.sl.dto.AddressDto;
import net.sl.dto.UserDto;
import net.sl.exception.DocxTemplateFillerException;
import net.sl.processor.PojoFieldTagProcessor;
import net.sl.processor.PojoNestedBlockTagProcessor;
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

/**
 * <p/>
 * Created on 01/13/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateFillerDtoInnerBlockTest {

    private DocxTemplateFiller filler = new DocxTemplateFiller();

    @Test
    public void testFillingBlockFromDto() {
        try (InputStream templateIs = getClass().getResourceAsStream("/net/sl/Placeholders-dto-block-value-template.docx");
             ByteArrayOutputStream filledTemplateOs = new ByteArrayOutputStream();) {
            DocxTemplateFillerContext context = new DocxTemplateFillerContext();
            context.setProcessors(Arrays.asList(new PojoNestedBlockTagProcessor(), new PojoFieldTagProcessor()));
            context.push(null, fillExample());
            filler.fillTemplate(templateIs, filledTemplateOs, context);
            Assert.assertNotEquals(0, filledTemplateOs.size());

            try (InputStream is = new ByteArrayInputStream(filledTemplateOs.toByteArray());
                 XWPFDocument doc = new XWPFDocument(OPCPackage.open(is));) {
                DocxTemplateUtils.getInstance().storeDocToFile(doc, "D:/TEMP/_filler_result.docx");
                Assert.assertEquals(1, doc.getTables().size());
                Assert.assertEquals("Belarus\n" +
                        "Minsk\n" +
                        "Skorina 1\n", doc.getTables().get(0).getText());
                XWPFParagraph par = (XWPFParagraph) doc.getBodyElements().get(0);
                Assert.assertEquals("User: Stanislav Lapitsky", par.getText());
            }
        } catch (IOException | InvalidFormatException | DocxTemplateFillerException ex) {
            Assert.fail();
        }
    }

    private UserDto fillExample() {
        UserDto res = new UserDto();
        res.setFirstName("Stanislav");
        res.setLastName("Lapitsky");

        AddressDto address = new AddressDto();
        address.setCountry("Belarus");
        address.setCity("Minsk");
        address.setStreet("Skorina 1");
        res.setAddress(address);
        return res;
    }

}
