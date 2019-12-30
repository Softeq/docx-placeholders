package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
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
public class DtoTagProcessor extends AbstractTagProcessor implements TagProcessor
{

    public static final String DTO_PATH_TAG_PREFIX = "dtoField:";

    public static final String DTO_COLLECTION_START_TAG_PREFIX = "dtoCollection:";

    public static final String DTO_COLLECTION_END_TAG_PREFIX = "/dtoCollection";

    private Object valuesRoot;

    public DtoTagProcessor(Object valuesDto)
    {
        this.valuesRoot = valuesDto;
    }

    @Override
    public boolean canProcessTag(TagInfo tag)
    {
        return tag.getTagText().startsWith(DTO_PATH_TAG_PREFIX) || tag.getTagText().startsWith(DTO_COLLECTION_START_TAG_PREFIX);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException
    {
        if (tag.getTagText().startsWith(DTO_COLLECTION_START_TAG_PREFIX))
        {
            return replaceCollectionTag(tag, elem, context);
        }
        else if (tag.getTagText().startsWith(DTO_PATH_TAG_PREFIX))
        {
            try
            {
                String tagValue = getStringTagValue(tag);
                fillTagPlaceholderWithValue((XWPFParagraph) elem, tag, tagValue, context);

                return DocxTemplateUtils.getInstance().getNextSibling(elem);
            }
            catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
            {
                DocxTemplateUtils.getInstance().storeDocToFile(((XWPFParagraph) elem).getDocument(),
                        "d:/temp/_beforeException-" + System.currentTimeMillis() + ".docx");
                throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
            }
        }
        return elem;
    }

    private String getStringTagValue(TagInfo tag) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException
    {
        Object value = null;
        try
        {
            value = PropertyUtils.getSimpleProperty(valuesRoot, getRealTagName(tag));
        }
        catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            return "no";
        }
        String tagValue = value == null ? null : value.toString();
        if (tagValue == null)
        {
            tagValue = StringUtils.EMPTY;
        }
        return tagValue;
    }

    private String getRealTagName(TagInfo tag)
    {
        if (tag.getTagText().startsWith(DTO_PATH_TAG_PREFIX))
        {
            return tag.getTagText().substring(DTO_PATH_TAG_PREFIX.length());
        }
        else if (tag.getTagText().startsWith(DTO_COLLECTION_START_TAG_PREFIX))
        {
            return tag.getTagText().substring(DTO_COLLECTION_START_TAG_PREFIX.length());
        }

        return tag.getTagText();
    }

    private IBodyElement replaceCollectionTag(TagInfo tag, IBodyElement tagStartElement, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException
    {
        IBody body = tagStartElement.getBody();
        List<IBodyElement> tagBodyList = getTagBodyElements(tagStartElement, tag, context);
        XWPFDocument tagBodyClone;
        try
        {
            //to avoid check POI check orphans after removing elements
            tagBodyClone = DocxTemplateUtils.getInstance().deepCloneElements(tagBodyList);
        }
        catch (IOException e)
        {
            throw new DocxTemplateFillerException("Cannot clone body elements list", e);
        }

        int i = DocxTemplateUtils.getInstance().getElementIndex(tagStartElement);
        //fill tag body
        int insertedCount = fillTagBody(tag, (XWPFParagraph) tagStartElement, tagBodyClone.getBodyElements(), context);
        i += insertedCount;

        //then we have to remove all the original tag body
        DocxTemplateUtils.getInstance().removeElements(body, i, tagBodyList.size() + 2);
        //next sibling now is the element after removed original body (referenced by the same i index now)
        IBodyElement nextSibling = body.getBodyElements().size() > i ? body.getBodyElements().get(i) : null;
        return nextSibling;
    }

    private List<IBodyElement> getTagBodyElements(IBodyElement tagStartElement, TagInfo tag, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException
    {
        List<IBodyElement> tagBodyList = new ArrayList<>();
        IBodyElement nextElem = DocxTemplateUtils.getInstance().getNextSibling(tagStartElement);
        int nestedCount = 0;
        while (nextElem != null)
        {
            TagInfo nextElemTag = DocxTemplateUtils.getInstance().getTag(nextElem, context);
            if (nextElemTag != null && nextElemTag.getTagText().startsWith(DTO_COLLECTION_START_TAG_PREFIX))
            {
                //opening nested tag
                nestedCount++;
            }
            else if (nextElemTag != null && nextElemTag.getTagText().startsWith(DTO_COLLECTION_END_TAG_PREFIX))
            {
                if (nestedCount == 0)
                {
                    break;
                }
                else
                {
                    nestedCount--;
                }
            }
            tagBodyList.add(nextElem);
            nextElem = DocxTemplateUtils.getInstance().getNextSibling(nextElem);
        }
        if (nextElem == null)
        {
            //tag start has no closing tag (we achieved end of the document)
            throw new DocxTemplateFillerException("The tag " + tag.getTagText() + " has no closing tag");
        }
        return tagBodyList;
    }

    private int fillTagBody(TagInfo tag, XWPFParagraph tagStartPar, List<IBodyElement> tagBodyList, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException
    {
        int insertedCount = 0;
        Object oldValuesRoot = valuesRoot;
        try
        {
            Collection items = (Collection) PropertyUtils.getSimpleProperty(valuesRoot, getRealTagName(tag));
            if (items == null)
            {
                return 0;
            }
            int itemsCount = items.size();
            Iterator it = items.iterator();
            XWPFDocument tagBodyClone;
            //detect index of the tag start in the document to insert cloned tag bodies
            for (int i = 0; i < itemsCount; i++)
            {
                //clone tag body elems
                tagBodyClone = DocxTemplateUtils.getInstance().deepCloneElements(tagBodyList);
                //recursively apply the same logic to body clone to replace tags
                //i is used to specify element (anomaly) number in the tag elements list
                valuesRoot = it.next();
                DocxTemplateUtils.getInstance().fillTags(tagBodyClone, context);
                insertedCount += tagBodyClone.getBodyElements().size();
                DocxTemplateUtils.getInstance().insertBodyElementsAfterParagraph(tagBodyClone, tagStartPar);
            }
        }
        catch (IOException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e)
        {
            throw new DocxTemplateFillerException("Cannot clone tag body", e);
        }
        valuesRoot = oldValuesRoot;
        return insertedCount;
    }

}
