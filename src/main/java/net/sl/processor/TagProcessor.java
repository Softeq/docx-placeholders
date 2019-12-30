package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.poi.xwpf.usermodel.IBodyElement;

/**
 * Tag Processor contract.
 * <p/>
 * To be used in the filler.
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public interface TagProcessor {

    /**
     * Checks whether the parocessor can process the tag.
     *
     * @param tag tag candidate
     * @return true if the processor can process passted tag
     */
    boolean canProcessTag(TagInfo tag);

    /**
     * Process the passed tag containing in the elem. The tag placeholder (or tag body) is replaced with evaluated value(s).
     *
     * @param tag     tag to process
     * @param elem    body element where the tag start was detected
     * @param context filler context
     * @return next body element to continue processing
     * @throws DocxTemplateFillerException
     */
    IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException;
}
