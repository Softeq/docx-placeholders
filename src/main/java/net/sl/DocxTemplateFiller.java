package net.sl;

import net.sl.exception.DocxTemplateFillerException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFFooter;
import org.apache.poi.xwpf.usermodel.XWPFHeader;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Main class which obtains template stream and context, fills the result document and stores the result into target result stream.
 * <p/>
 * The filler reads template stream document, detects tags, defines which {@link TagProcessor} can calculate the tag value and fill the tag
 * value to result document.
 * <p/>
 * Created on 10/2/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateFiller
{

    public static final String TAG_START = "${{";

    public static final String TAG_END = "}}";

    /**
     * Reads templalte docx from the source stream, detects and fills placeholders, and writes results to the target output stream
     *
     * @param templateSourceStream template source stream containing docx
     * @param filledTemplateStream filled template results stream
     * @param context              filling contexts contgaining tags processors etc.
     * @throws IOException
     * @throws InvalidFormatException
     * @throws DocxTemplateFillerException
     */
    public void fillTemplate(InputStream templateSourceStream,
            OutputStream filledTemplateStream,
            DocxTemplateFillerContext context)
            throws IOException, InvalidFormatException, DocxTemplateFillerException
    {

        XWPFDocument doc = new XWPFDocument(OPCPackage.open(templateSourceStream));

        //replace tags in the document body
        DocxTemplateUtils.getInstance().fillTags(doc, context);

        //replace tags in the document headers
        for (XWPFHeader header : doc.getHeaderList())
        {
            DocxTemplateUtils.getInstance().fillTags(header, context);
        }
        //replace tags in the document footers
        for (XWPFFooter footer : doc.getFooterList())
        {
            DocxTemplateUtils.getInstance().fillTags(footer, context);
        }
        doc.write(filledTemplateStream);
    }

}
