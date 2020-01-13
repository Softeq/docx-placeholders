package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
public abstract class AbstractTagProcessor {

    /**
     * We detect tag placeholder start and end runs
     * The start run text is corrected (placeholder text part is removed from the end), the same happens with endTag
     * placeholder part is removed from the run start.
     * All the runs between placeholder start and end are removed.
     *
     * @param tag
     * @param par
     * @param tagData
     * @param context
     * @throws DocxTemplateFillerException
     * @throws IOException
     */
    protected void fillTag(TagInfo tag, XWPFParagraph par, Object tagData, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException, IOException {
        //position in the paragraph where placeholder ends
        int tagEndOffset = tag.getTagStartOffset() + context.getTagStart().length() + tag.getTagText().length() + context.getTagEnd().length();
        //we add each run text's length to keep current run start index (in the par)
        int accumulatedTextLength = 0;
        //new run text - old text with replaced placeholder
        String newRunText = null;
        List<XWPFRun> parRuns = par.getRuns();
        int tagStartRunIndex = -1;
        int tagEndRunIndex = -1;
        for (int i = 0; i < parRuns.size(); i++) {
            XWPFRun run = parRuns.get(i);
            String runText = run.text();

            int runEndPosition = accumulatedTextLength + runText.length();

            if (tag.getTagStartOffset() >= accumulatedTextLength && tag.getTagStartOffset() < runEndPosition) {
                //found a run where the placeholder starts
                tagStartRunIndex = i;
                //the new text for the starrt run is the text without the placeholder
                //but the text for the run will be replaced later
                newRunText = runText.substring(0, tag.getTagStartOffset() - accumulatedTextLength);
            }
            if (tagEndOffset >= accumulatedTextLength && tagEndOffset < runEndPosition) {
                //found a run where the placeholder ends
                tagEndRunIndex = i;
                if (tagStartRunIndex == tagEndRunIndex) {
                    //the placeholder starts and ends in the same run
                } else {
                    String newEndRunText = runText.substring(runEndPosition - accumulatedTextLength);
                    run.setText(newEndRunText, 0);
                }
                //no need to iterate. we found where the tag placeholder ends
                break;
            }
            accumulatedTextLength += runText.length();
        }

        //now we can replace text for the run where the placeholder starts
        XWPFRun startRun = parRuns.get(tagStartRunIndex);
        startRun.setText(newRunText, 0);

        //now we have indexes of the placeholder start and end run
        //all the previous runs must remain
        //all the runs in between must be removed completely
        //all the runs after must be removed and reinserted after the run which represents placeholder's value

        //create a copy with all the runs
        XWPFDocument clonedParagraphDoc = DocxTemplateUtils.getInstance().deepCloneElements(Collections.singletonList(par));
        XWPFParagraph parClone = clonedParagraphDoc.getParagraphs().get(0);
        //and remain copies to be reinserted only
        for (int i = 0; i < tagEndRunIndex; i++) {
            parClone.removeRun(0);
        }

        //remove all runs after placeholder (to be reinserted after adding run with placeholder value)
        for (int i = parRuns.size() - 1; i > tagStartRunIndex; i--) {
            par.removeRun(i);
        }

        //insert run with the placeholder value
        insertRun(par, tag, tagData, context);

        //reinsert runs back by coping runs from cloned paragraph
        if (tagEndRunIndex >= 0) {
            DocxTemplateUtils.getInstance().copyParagraph(parClone, par);
        }
    }

    /**
     * An important method used by multiple TagProcessor. In fact it must insert a run with all defined content.
     * It could be simple text run for Map or POJO field processor or more complicated run for Link or Image
     *
     * @param par     target paragraph where the tag was found
     * @param tag     the tag to be replaced
     * @param tagData data to evaluate tag value and properly insert content. The data could be extracted from the tag
     *                and/or context
     * @param context filler context
     * @throws DocxTemplateFillerException
     */
    protected abstract void insertRun(XWPFParagraph par, TagInfo tag, Object tagData, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException;

    protected Map<String, String> getTagPropertiesAsMap(String tagText) {
        Map<String, String> tagValuesMap = new HashMap<>();
        Document doc = Jsoup.parse("<p " + tagText + "/>");
        Element link = doc.select("p").first();
        for (Attribute attr : link.attributes()) {
            tagValuesMap.put(attr.getKey(), attr.getValue());
        }

        return tagValuesMap;
    }
}
