package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * The processor is based on DTO fields
 * <p/>
 * The processor can fill image template placeholders from POJO DTO fields
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DtoTagImageProcessor extends AbstractTagProcessor implements TagProcessor {

    public static final String TAG_PREFIX_IMAGE = "image:";
    public static final String PROPERTY_TITLE_REF_NAME = "title";
    public static final String PROPERTY_SOURCE_REF_NAME = "source";
    public static final String PROPERTY_FORMAT_REF_NAME = "imageFormat";
    public static final String PROPERTY_PIXELS_WIDTH_REF_NAME = "width";
    public static final String PROPERTY_PIXELS_HEIGHT_REF_NAME = "height";

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tag.getTagText().startsWith(TAG_PREFIX_IMAGE);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {
            fillTag(tag, (XWPFParagraph) elem, null, context);
            return DocxTemplateUtils.getInstance().getNextSibling(elem);
        } catch (IOException e) {
            throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
        }
    }

    private String getRealTagText(TagInfo tag) {
        return tag.getTagText().substring(TAG_PREFIX_IMAGE.length());
    }

    @Override
    protected void insertRun(XWPFParagraph par, TagInfo tag, Object tagData, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {
            Map<String, String> tagProprtiesMap = getTagPropertiesAsMap(getRealTagText(tag));
            String titleRefField = tagProprtiesMap.get(PROPERTY_TITLE_REF_NAME);
            String sourceRefField = tagProprtiesMap.get(PROPERTY_SOURCE_REF_NAME);
            String formatRefField = tagProprtiesMap.get(PROPERTY_FORMAT_REF_NAME);
            String widthRefField = tagProprtiesMap.get(PROPERTY_PIXELS_WIDTH_REF_NAME);
            String heightRefField = tagProprtiesMap.get(PROPERTY_PIXELS_HEIGHT_REF_NAME);

            String title = (String) PropertyUtils.getSimpleProperty(context.getRootValue(), titleRefField);
            String formatStr = (String) PropertyUtils.getSimpleProperty(context.getRootValue(), formatRefField);
            int imageFormat = formatStr != null && (formatStr.contains("jpeg") || formatStr.contains("jpg")) ?
                    org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_JPEG :
                    org.apache.poi.xwpf.usermodel.Document.PICTURE_TYPE_PNG;
            Integer w = (Integer) PropertyUtils.getSimpleProperty(context.getRootValue(), widthRefField);
            Integer h = (Integer) PropertyUtils.getSimpleProperty(context.getRootValue(), heightRefField);
            InputStream imageStream = (InputStream) PropertyUtils.getSimpleProperty(context.getRootValue(), sourceRefField);

            XWPFRun targetRun = par.createRun();
            targetRun.addPicture(imageStream, imageFormat, title, Units.pixelToEMU(w), Units.pixelToEMU(h));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException | InvalidFormatException e) {
            throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
        }

    }
}
