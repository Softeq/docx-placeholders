package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * An bstract class for processors which work with tags with body.
 * <p/>
 * Created on 1/13/2020.
 * <p/>
 *
 * @author slapitsky
 */
public abstract class BodyTagProcessor {

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
    protected List<IBodyElement> getTagBodyElements(IBodyElement tagStartElement, TagInfo tag,
                                                    String tagStartToken, String tagEndToken,
                                                    DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        List<IBodyElement> tagBodyList = new ArrayList<>();
        IBodyElement nextElem = DocxTemplateUtils.getInstance().getNextSibling(tagStartElement);
        int nestedCount = 0;
        while (nextElem != null) {
            TagInfo nextElemTag = DocxTemplateUtils.getInstance().getTag(nextElem, context);
            //if we get one more the same tag (nested collection) increase nested tag count to include all nested onses
            if (nextElemTag != null && nextElemTag.getTagText().startsWith(tagStartToken)) {
                //opening nested tag
                nestedCount++;
            } else if (nextElemTag != null && nextElemTag.getTagText().startsWith(tagEndToken)) {
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
     * Replaces tag with body with evaluated value.
     * The method gets tag body - all the elements between tag start and tag end, clones the body, and evaluates the
     * tag body items (nested tags).
     * Then evaluated result (body copy) is inserted and original tag body is removed (including tag start and end)
     *
     * @param tag             block tag to be replaced
     * @param tagStartElement body element where tag start was detected
     * @param context         filler context
     * @return
     * @throws DocxTemplateFillerException
     */
    protected IBodyElement replaceBodyTag(TagInfo tag, IBodyElement tagStartElement,
                                          String tagStartToken, String tagEndToken,
                                          DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        IBody body = tagStartElement.getBody();
        //get tag body elements (all paragraphs and tags between tag start and tag end paragraphs)
        List<IBodyElement> tagBodyList = getTagBodyElements(tagStartElement, tag, tagStartToken, tagEndToken, context);
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
     * Fills tag body.
     * Tag ref block is extracted.
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
    protected abstract int fillTagBody(TagInfo tag, XWPFParagraph tagStartPar, List<IBodyElement> tagBodyList, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException;
}
