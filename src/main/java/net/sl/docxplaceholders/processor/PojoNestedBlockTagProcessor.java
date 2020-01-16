package net.sl.docxplaceholders.processor;

import net.sl.docxplaceholders.DocxTemplateFillerContext;
import net.sl.docxplaceholders.DocxTemplateUtils;
import net.sl.docxplaceholders.TagInfo;
import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * The processor is based on DTO field. The field is represented as inner block.
 * E.g. POJO User has inner POJO Address. Using the block refernecing the nested POJO address' fields
 * <p/>
 * The processor can fill nested POJO field (by name) and go deeper to the inner POJO
 * <p/>
 * Created on 01/13/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class PojoNestedBlockTagProcessor extends BodyTagProcessor implements TagProcessor {

    public static final String TAG_PREFIX_BLOCK_START = "block:";

    public static final String TAG_PREFIX_BLOCK_END = "/block";

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tag.getTagText().startsWith(TAG_PREFIX_BLOCK_START);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        return replaceBodyTag(tag, elem, TAG_PREFIX_BLOCK_START, TAG_PREFIX_BLOCK_END, context);
    }

    /**
     * Subtracts the tag prefix and returns the rest
     *
     * @param tag tag info with full tag text
     * @return tag text without collection prefix
     */
    private String getRealTagName(TagInfo tag) {
        return tag.getTagText().substring(TAG_PREFIX_BLOCK_START.length());
    }

    @Override
    protected int fillTagBody(TagInfo tag, XWPFParagraph tagStartPar, List<IBodyElement> tagBodyList, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        int insertedCount = 0;
        try {
            //get collection by the name defined in the tag.
            Object blockRoot = PropertyUtils.getSimpleProperty(context.getRootValue(), getRealTagName(tag));
            if (blockRoot == null) {
                //no block body
                return insertedCount;
            }
            //clone tag body elems
            XWPFDocument tagBodyClone = DocxTemplateUtils.getInstance().deepCloneElements(tagBodyList);
            //recursively apply the same logic to body clone to replace tags
            //i is used to specify element (anomaly) number in the tag elements list
            context.push(tag, blockRoot);
            DocxTemplateUtils.getInstance().fillTags(tagBodyClone, context);
            insertedCount += tagBodyClone.getBodyElements().size();
            DocxTemplateUtils.getInstance().insertBodyElementsAfterParagraph(tagBodyClone, tagStartPar);
            context.pop();
        } catch (IOException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new DocxTemplateFillerException("Cannot clone tag body", e);
        }
        return insertedCount;
    }

}
