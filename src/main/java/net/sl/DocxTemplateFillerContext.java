package net.sl;

import net.sl.exception.DocxTemplateFillerException;
import net.sl.processor.TagProcessor;

import org.apache.poi.xwpf.usermodel.IBodyElement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Context shares common info required for template filling.
 * <p/>
 * Contains list of processors which can process tags as well as a queue (used as stack when we go to the nested tags). Tag start and tag
 * end tokens could be defined to customize tag detecting and usege. If they are empty default tag start dn end are used.
 * <p/>
 * Created on 10/2/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateFillerContext
{

    private String tagStart = null;

    private String tagEnd = null;

    private List<TagProcessor> processors = new ArrayList<>();

    private Deque<String> tagsQueue = new ArrayDeque<>();

    public Deque<String> getTagsQueue()
    {
        return tagsQueue;
    }

    public void setTagsQueue(Deque<String> tagsQueue)
    {
        this.tagsQueue = tagsQueue;
    }

    public IBodyElement process(TagInfo tag, IBodyElement elem) throws DocxTemplateFillerException
    {
        for (TagProcessor processor : processors)
        {
            if (processor.canProcessTag(tag))
            {
                IBodyElement nextElement = processor.process(tag, elem, this);
                if (nextElement != null)
                {
                    return nextElement;
                }
            }
        }
        return getNextSibling(elem);
    }

    public IBodyElement getNextSibling(IBodyElement elem)
    {
        for (int i = 0; i < elem.getBody().getBodyElements().size() - 1; i++)
        {
            if (elem.getBody().getBodyElements().get(i) == elem)
            {
                return elem.getBody().getBodyElements().get(i + 1);
            }
        }

        return null;
    }

    public List<TagProcessor> getProcessors()
    {
        return processors;
    }

    public void setProcessors(List<TagProcessor> processors)
    {
        this.processors = processors;
    }

    public String getTagStart()
    {
        return tagStart != null ? tagStart : DocxTemplateUtils.DEFAULT_TAG_START;
    }

    public void setTagStart(String tagStart)
    {
        this.tagStart = tagStart;
    }

    public String getTagEnd()
    {
        return tagEnd != null ? tagEnd : DocxTemplateUtils.DEFAULT_TAG_END;
    }

    public void setTagEnd(String tagEnd)
    {
        this.tagEnd = tagEnd;
    }
}
