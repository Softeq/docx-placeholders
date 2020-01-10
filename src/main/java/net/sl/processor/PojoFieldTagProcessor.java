package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import net.sl.exception.DocxTemplateFillerTechnicalException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * The processor is based on DTO fields
 * <p/>
 * The processor can fill template placeholder with POJO DTO fields (by name)
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class PojoFieldTagProcessor extends AbstractTagProcessor implements TagProcessor {

    public static final String TAG_PREFIX_FIELD = "field:";

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tag.getTagText().startsWith(TAG_PREFIX_FIELD);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {
            String tagValue = getStringTagValue(tag, context);
            fillTag(tag, (XWPFParagraph) elem, tagValue, context);

            return DocxTemplateUtils.getInstance().getNextSibling(elem);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException e) {
            throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
        }
    }

    private String getStringTagValue(TagInfo tag, DocxTemplateFillerContext context)
            throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        Object value = null;
        try {
            value = PropertyUtils.getSimpleProperty(context.getRootValue(), getRealTagName(tag));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new DocxTemplateFillerTechnicalException("Cannot get tag " + tag + " value from the context");
        }
        String tagValue = value == null ? null : value.toString();
        if (tagValue == null) {
            tagValue = StringUtils.EMPTY;
        }
        return tagValue;
    }

    private String getRealTagName(TagInfo tag) {
        return tag.getTagText().substring(TAG_PREFIX_FIELD.length());
    }

    @Override
    protected void insertRun(XWPFParagraph par, TagInfo tag, Object tagData, DocxTemplateFillerContext context) throws DocxTemplateFillerException {
        XWPFRun run = par.createRun();
        run.setText((String) tagData);
    }
}
