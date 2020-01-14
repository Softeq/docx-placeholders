package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * The processor is based on DTO fields
 * <p/>
 * The processor can fill POJO DTO field (by name) and can process a collection of nested DTOs
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class PojoCollectionTagProcessor extends BodyTagProcessor implements TagProcessor {

    public static final String TAG_PREFIX_COLLECTION_START = "collection:";

    public static final String TAG_PREFIX_COLLECTION_END = "/collection";

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tag.getTagText().startsWith(TAG_PREFIX_COLLECTION_START);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        return replaceBodyTag(tag, elem, TAG_PREFIX_COLLECTION_START, TAG_PREFIX_COLLECTION_END, context);
    }

    /**
     * Subtracts the tag prefiz and returns the rest
     *
     * @param tag tag info with full tag text
     * @return tag text without collection prefix
     */
    private String getRealTagName(TagInfo tag) {
        return tag.getTagText().substring(TAG_PREFIX_COLLECTION_START.length());
    }

    @Override
    protected int fillTagBody(TagInfo tag, XWPFParagraph tagStartPar, List<IBodyElement> tagBodyList, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        int insertedCount = 0;
        try {
            //get collection by the name defined in the tag.
            Collection items = (Collection) PropertyUtils.getSimpleProperty(context.getRootValue(), getRealTagName(tag));
            if (items == null) {
                return 0;
            }
            int itemsCount = items.size();
            Iterator it = items.iterator();
            XWPFDocument tagBodyClone;
            //detect index of the tag start in the document to insert cloned tag bodies
            for (int i = 0; i < itemsCount; i++) {
                //clone tag body elems
                tagBodyClone = DocxTemplateUtils.getInstance().deepCloneElements(tagBodyList);
                //recursively apply the same logic to body clone to replace tags
                //i is used to specify element (anomaly) number in the tag elements list
                context.push(tag, it.next());
                DocxTemplateUtils.getInstance().fillTags(tagBodyClone, context);
                insertedCount += tagBodyClone.getBodyElements().size();
                DocxTemplateUtils.getInstance().insertBodyElementsAfterParagraph(tagBodyClone, tagStartPar);
                context.pop();
            }
        } catch (IOException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new DocxTemplateFillerException("Cannot clone tag body", e);
        }
        return insertedCount;
    }

}
