package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
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
public class DtoTagCollectionProcessor extends AbstractTagProcessor implements TagProcessor {

    public static final String DTO_COLLECTION_START_TAG_PREFIX = "dtoCollection:";

    public static final String DTO_COLLECTION_END_TAG_PREFIX = "/dtoCollection";

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tag.getTagText().startsWith(DTO_COLLECTION_START_TAG_PREFIX);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        return replaceCollectionTag(tag, elem, context);
    }

    /**
     * Subtracts the tag prefiz and returns the rest
     *
     * @param tag tag info with full tag text
     * @return tag text without collection prefix
     */
    private String getRealTagName(TagInfo tag) {
        return tag.getTagText().substring(DTO_COLLECTION_START_TAG_PREFIX.length());
    }

    /**
     * Replaces tag with body with evaluated value.
     * The method gets tag body - all the elements between tag start and tag end, clones the body as many times as tag
     * collection elements count, fills the tag body items (evaluate nested tags).
     * Then evaluated results (body copies) are inserted and original tag body is removed (including tag start and end)
     *
     * @param tag             collection tag to be replaced
     * @param tagStartElement body element where tag start was detected
     * @param context         filler context
     * @return
     * @throws DocxTemplateFillerException
     */
    private IBodyElement replaceCollectionTag(TagInfo tag, IBodyElement tagStartElement, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        IBody body = tagStartElement.getBody();
        //get tag body elements (all paragraphs and tags between tag start and tag end paragraphs)
        List<IBodyElement> tagBodyList = getTagBodyElements(tagStartElement, tag, context);
        XWPFDocument tagBodyClone;
        try {
            //to avoid check POI check orphans after removing elements
            tagBodyClone = DocxTemplateUtils.getInstance().deepCloneElements(tagBodyList);
        } catch (IOException e) {
            throw new DocxTemplateFillerException("Cannot clone body elements list", e);
        }

        int i = DocxTemplateUtils.getInstance().getElementIndex(tagStartElement);
        //fills tag body. insert as many copies as collection elements count
        //returns amount of inserted elements to continue processing after the tag end
        int insertedCount = fillTagBody(tag, (XWPFParagraph) tagStartElement, tagBodyClone.getBodyElements(), context);
        i += insertedCount;

        //then we have to remove all the original tag body
        DocxTemplateUtils.getInstance().removeElements(body, i, tagBodyList.size() + 2);
        //next sibling now is the element after removed original body (referenced by the same i index now)
        IBodyElement nextSibling = body.getBodyElements().size() > i ? body.getBodyElements().get(i) : null;
        return nextSibling;
    }

    /**
     * Gets tag body elements (all paragraphs and tags between tag start and tag end paragraphs).
     * The method goes through all the body alements after the tag start collecting them till the tag end paragraph is
     * achieved
     *
     * @param tagStartElement body element (normally a paragraph) where the tag start was detected
     * @param tag             tag to get body
     * @param context         filler context
     * @return
     * @throws DocxTemplateFillerException
     */
    private List<IBodyElement> getTagBodyElements(IBodyElement tagStartElement, TagInfo tag, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        List<IBodyElement> tagBodyList = new ArrayList<>();
        IBodyElement nextElem = DocxTemplateUtils.getInstance().getNextSibling(tagStartElement);
        int nestedCount = 0;
        while (nextElem != null) {
            TagInfo nextElemTag = DocxTemplateUtils.getInstance().getTag(nextElem, context);
            //if we get one more the same tag (nested collection) increase nested tag count to include all nested onses
            if (nextElemTag != null && nextElemTag.getTagText().startsWith(DTO_COLLECTION_START_TAG_PREFIX)) {
                //opening nested tag
                nestedCount++;
            } else if (nextElemTag != null && nextElemTag.getTagText().startsWith(DTO_COLLECTION_END_TAG_PREFIX)) {
                //found tag end. If no nested tags found the tag body end is achieved
                if (nestedCount == 0) {
                    break;
                } else {
                    nestedCount--;
                }
            }
            tagBodyList.add(nextElem);
            nextElem = DocxTemplateUtils.getInstance().getNextSibling(nextElem);
        }
        if (nextElem == null) {
            //tag start has no closing tag (we achieved end of the document)
            throw new DocxTemplateFillerException("The tag " + tag.getTagText() + " has no closing tag");
        }
        return tagBodyList;
    }

    /**
     * Fills tag body.
     * Tag ref collection is extracted. The tag body must be inserted as many times as many collection elements found.
     * Tag body cloned, tag value pushed to be a new values root for the body (necessary to recoursively evaluate
     * tags in the tag body copy).
     * fillTags evaluate tag values for the body item and all the filled body elements is inserted after paragraph.
     *
     * @param tag         tag to fill
     * @param tagStartPar tag start paragraph (par where collection start tag was found)
     * @param tagBodyList list of body elements to be inserted for each element of the collection
     * @param context     filler context
     * @return amount of inserted body element. Used to shift index and continue tags evaluation after the filled body.
     * @throws DocxTemplateFillerException
     */
    private int fillTagBody(TagInfo tag, XWPFParagraph tagStartPar, List<IBodyElement> tagBodyList, DocxTemplateFillerContext context)
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
