package net.sl.docxplaceholders;

import net.sl.docxplaceholders.exception.DocxTemplateFillerException;
import net.sl.docxplaceholders.processor.TagProcessor;
import org.apache.poi.xwpf.usermodel.IBodyElement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Context shares common info required for template filling.
 * <p/>
 * Contains list of processors which can process tags as well as a queue (used as stack when we go to the nested tags).
 * Tag start and tag end tokens could be defined to customize tag detecting and usege. If they are empty default tag
 * start adn end are used.
 * <p/>
 * Created on 10/2/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateFillerContext {

    private String tagStart = null;

    private String tagEnd = null;

    private List<TagProcessor> processors = new ArrayList<>();

    private Deque<StackContext> tagsQueue = new ArrayDeque<>();

    /**
     * Processes tag contained in the body element. The result is the next body element to continue placeholders
     * filling
     *
     * @param tag
     * @param elem
     * @return the next body element to move pointer after processing
     * @throws DocxTemplateFillerException
     */
    public IBodyElement process(TagInfo tag, IBodyElement elem) throws DocxTemplateFillerException {
        for (TagProcessor processor : processors) {
            if (processor.canProcessTag(tag)) {
                IBodyElement nextElement = processor.process(tag, elem, this);
                if (nextElement != null) {
                    return nextElement;
                }
            }
        }
        return DocxTemplateUtils.getInstance().getNextSibling(elem);
    }


    /**
     * @return list of registered tag processors
     */
    public List<TagProcessor> getProcessors() {
        return processors;
    }

    /**
     * Sets list of tag processors
     *
     * @param processors
     */
    public void setProcessors(List<TagProcessor> processors) {
        this.processors = processors;
    }

    /**
     * @return tag start prefix if it's set (or default tag start if not)
     */
    public String getTagStart() {
        return tagStart != null ? tagStart : DocxTemplateUtils.DEFAULT_TAG_START;
    }

    /**
     * Sets tag start prefix (used to detect tags)
     *
     * @param tagStart
     */
    public void setTagStart(String tagStart) {
        this.tagStart = tagStart;
    }

    /**
     * @return tag end prefix if it's set (or default tag end if not)
     */
    public String getTagEnd() {
        return tagEnd != null ? tagEnd : DocxTemplateUtils.DEFAULT_TAG_END;
    }

    /**
     * Sets tag end prefix (used to detect tags)
     *
     * @param tagEnd
     */
    public void setTagEnd(String tagEnd) {
        this.tagEnd = tagEnd;
    }

    /**
     * Stack root value. Nested tags could change the root pushing elements in the stack
     *
     * @return current top value of the stack. Used to evaluate tag value
     */
    public Object getRootValue() {
        return tagsQueue.getFirst().getValue();
    }

    /**
     * Places a new value root on top of the stack. Used by nested tags.
     *
     * @param tag       source tag
     * @param rootValue value object corresponding to the pushed tag. Nested tags use the new root to extract tag values
     */
    public void push(TagInfo tag, Object rootValue) {
        tagsQueue.push(new StackContext(tag, rootValue));
    }

    /**
     * Restores previous stack top when tag is closed
     */
    public void pop() {
        tagsQueue.pop();
    }

    /**
     * Keeps stack information - tag and root value
     */
    public static class StackContext {
        private final TagInfo tag;
        private final Object value;

        public StackContext(TagInfo tag, Object value) {
            this.tag = tag;
            this.value = value;
        }

        public TagInfo getTag() {
            return tag;
        }

        public Object getValue() {
            return value;
        }
    }
}
