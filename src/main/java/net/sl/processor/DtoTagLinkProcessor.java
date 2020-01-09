package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

/**
 * The processor is based on DTO fields
 * <p/>
 * The processor can fill link template placeholder using fields of the DTO for link, text, color etc
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DtoTagLinkProcessor extends AbstractTagProcessor implements TagProcessor {

    public static final String TAG_PREFIX_LINK = "link:";
    public static final String PROPERTY_TEXT_REF_NAME = "text";
    public static final String PROPERTY_URL_REF_NAME = "url";
    public static final String PROPERTY_COLOR_REF_NAME = "color";

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tag.getTagText().startsWith(TAG_PREFIX_LINK);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {
            fillTag(tag, (XWPFParagraph) elem, null, context);
        } catch (IOException e) {
            throw new DocxTemplateFillerException("Cannot fill tag " + tag, e);
        }
        return DocxTemplateUtils.getInstance().getNextSibling(elem);
    }

    private String getRealTagText(TagInfo tag) {
        return tag.getTagText().substring(TAG_PREFIX_LINK.length());
    }

    @Override
    protected void insertRun(XWPFParagraph par, TagInfo tag, Object tagData, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {

            Map<String, String> tagProprtiesMap = getTagPropertiesAsMap(getRealTagText(tag));
            String linkValueRefField = tagProprtiesMap.get(PROPERTY_TEXT_REF_NAME);
            String linkUrlRefField = tagProprtiesMap.get(PROPERTY_URL_REF_NAME);
            String linkColorRefField = tagProprtiesMap.get(PROPERTY_COLOR_REF_NAME);

            String linkText = (String) PropertyUtils.getSimpleProperty(context.getRootValue(), linkValueRefField);
            String linkUrl = (String) PropertyUtils.getSimpleProperty(context.getRootValue(), linkUrlRefField);
            String linkColor = (String) PropertyUtils.getSimpleProperty(context.getRootValue(), linkColorRefField);

            DocxTemplateUtils.getInstance().addHyperlink(par, linkText, linkUrl, linkColor);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
        }
    }
}
