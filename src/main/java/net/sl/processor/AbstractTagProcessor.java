package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.util.ArrayList;
import java.util.List;

/**
 * The abstract class for processors.
 * <p/>
 * Fills a simple tag value to specified paragraph containing the tag.
 * <p/>
 * Created on 10/21/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class AbstractTagProcessor {

    /**
     * A paragraph where the tag was found is filled with the provided value. The tag's placeholders is replaced with tag value
     *
     * @param par     the paragraph containing tag
     * @param tag     tag to be replaced
     * @param value   value to use instead of the placeholder
     * @param context filler context
     * @throws DocxTemplateFillerException
     */
    protected void fillTagPlaceholderWithValue(XWPFParagraph par, TagInfo tag, String value, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        //position in the paragraph where placeholder ends
        int endIndex = tag.getTagStartOffset() + context.getTagStart().length() + tag.getTagText().length() + ("/" + context.getTagEnd())
                .length();
        //we add each run text's length to keep current run start index (in the par)
        int accumulatedTextLength = 0;
        //run where placeholder starts (to replace it)
        XWPFRun replaceRun = null;
        //new run text - old text with replaced placeholder
        String newRunText = null;
        List<XWPFRun> parRuns = par.getRuns();
        //if placeholder's mid is a separate ran the run must be removed totally
        List<Integer> runsToBeRemoved = new ArrayList<>();
        for (int i = 0; i < parRuns.size(); i++) {
            XWPFRun run = parRuns.get(i);
            String runText = run.text();

            switch (runText) {
                case DocxTemplateUtils.PAGE_BREAK:
                    par.setPageBreak(true);
                    break;
                case DocxTemplateUtils.COMMA_SEPARATOR:
                    if (StringUtils.EMPTY.equals(parRuns.get(i - 1).text())) {
                        runsToBeRemoved.add(0, i);
                    }
                    break;
                default:
                    break;
            }
            int runEndPosition = accumulatedTextLength + runText.length();
            if (tag.getTagStartOffset() >= accumulatedTextLength && tag.getTagStartOffset() < runEndPosition) {
                //insert in this run (placeholder placeHolderStartIndex is in this run)
                replaceRun = run;
                newRunText = runText.substring(0, tag.getTagStartOffset() - accumulatedTextLength);
            } else if (accumulatedTextLength > tag.getTagStartOffset()) {
                if (endIndex >= runEndPosition) {
                    //the run is located within placeholder's bounds
                    runsToBeRemoved.add(0, i);
                } else {
                    //the run has end of placeholder
                    if (endIndex - accumulatedTextLength > 0) {
                        String newText = runText.substring(endIndex - accumulatedTextLength);
                        run.setText(newText, 0);
                    }
                    break;
                }
            }
            accumulatedTextLength += runText.length();
        }

        //remove runs inside placeholder
        for (int removeRunIndex : runsToBeRemoved) {
            par.removeRun(removeRunIndex);
        }
        if (replaceRun == null) {
            throw new DocxTemplateFillerException("Cannot replace tag. Paragraph text='" + par.getText() +
                    "' offset=" + tag.getTagStartOffset() + " tag=" + tag.getTagText());
        }
        //remove part of placeholder from run
        replaceRun.setText(newRunText, 0);
        XWPFRun tagValueRun = replaceRun;
        if (newRunText.length() != 0) {
            tagValueRun = par.createRun();
        }
        //and fill value to the run
        tagValueRun.setText(value);
    }

}
