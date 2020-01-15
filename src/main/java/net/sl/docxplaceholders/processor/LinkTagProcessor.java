package net.sl.docxplaceholders.processor;

import net.sl.docxplaceholders.DocxTemplateFillerContext;
import net.sl.docxplaceholders.DocxTemplateUtils;
import net.sl.docxplaceholders.TagInfo;
import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.tag.TagLinkData;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

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
public class LinkTagProcessor extends AbstractTagProcessor implements TagProcessor {

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
        return elem;
    }

    /**
     * Returns tag text without prefix
     *
     * @param tag
     * @return
     */
    private String getRealTagText(TagInfo tag) {
        return tag.getTagText().substring(TAG_PREFIX_LINK.length());
    }

    @Override
    protected void insertRun(XWPFParagraph par, TagInfo tag, Object tagData, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {
            //we read from the context Link contract - the TagLinkData interface.
            //the interface has getters to return link attributes
            String tagText = getRealTagText(tag);

            TagLinkData link;
            if (StringUtils.isBlank(tagText)) {
                link = (TagLinkData) context.getRootValue();
            } else {
                link = (TagLinkData) PropertyUtils.getSimpleProperty(context.getRootValue(), tagText);
            }
            DocxTemplateUtils.getInstance().addHyperlink(par, link.getText(), link.getUrl(), link.getColor());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
        }
    }
}
