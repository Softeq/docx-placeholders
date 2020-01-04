package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.DocxTemplateUtils;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRelation;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The processor is based on DTO fields
 * <p/>
 * The processor can fill template placeholder with POJO DTO fields (by name)
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DtoTagLinkProcessor extends AbstractTagProcessor implements TagProcessor {

    public static final String TAG_PREFIX_LINK = "link:";
    public static final String PROPERTY_TEXT_REF_NAME = "text";
    public static final String PROPERTY_URL_REF_NAME = "url";

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tag.getTagText().startsWith(TAG_PREFIX_LINK);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {
            Map<String, String> tagProprtiesMap = getTagLinkData(tag);
            String linkValueRefField = tagProprtiesMap.get(PROPERTY_TEXT_REF_NAME);
            String linkUrlRefField = tagProprtiesMap.get(PROPERTY_URL_REF_NAME);

            String linkText = (String) PropertyUtils.getSimpleProperty(context.getRootValue(), linkValueRefField);
            String linkUrl = (String) PropertyUtils.getSimpleProperty(context.getRootValue(), linkUrlRefField);
            fillLink(tag, (XWPFParagraph) elem, linkText, linkUrl, context);
            return DocxTemplateUtils.getInstance().getNextSibling(elem);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException e) {
            throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
        }
    }

    private Map<String, String> getTagLinkData(TagInfo tag) {
        Map<String, String> tagValuesMap = new HashMap<>();
        String tagText = getRealTagText(tag);
        Document doc = Jsoup.parse("<p " + tagText + "/>");
        Element link = doc.select("p").first();
        for (Attribute attr : link.attributes()) {
            tagValuesMap.put(attr.getKey(), attr.getValue());
        }

        return tagValuesMap;
    }

    protected void fillLink(TagInfo tag, XWPFParagraph par, String text, String url, DocxTemplateFillerContext context)
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
                //now we can replace text for the run where the placeholder starts
                run = parRuns.get(tagStartRunIndex);
                run.setText(newRunText, 0);
                break;
            }
            accumulatedTextLength += runText.length();
        }
        //now we have indexes of the placeholder start and end run
        //all the previous runs must remain
        //all the runs in between must be removed completely
        //all the runs after must be removed and reinserted after the link
        List<XWPFRun> runsAfterPlaceholderToBeReinserted = new ArrayList<>();
        for (int i = tagEndRunIndex; i < parRuns.size(); i++) {
            runsAfterPlaceholderToBeReinserted.add(parRuns.get(i));
        }
        //create a copy with all the runs
        XWPFDocument clonedParagraphDoc = DocxTemplateUtils.getInstance().deepCloneElements(Collections.singletonList(par));
        XWPFParagraph parClone = clonedParagraphDoc.getParagraphs().get(0);
        //and remain copies to be reinserted only
        for (int i = 0; i < tagEndRunIndex; i++) {
            parClone.removeRun(0);
        }

        for (int i = parRuns.size() - 1; i > tagStartRunIndex; i--) {
            par.removeRun(i);
        }
        //insert link
        String rId = par.getDocument().getPackagePart().addExternalRelationship(url, XWPFRelation.HYPERLINK.getRelation()).getId();
        CTHyperlink hyperlink = par.getCTP().addNewHyperlink();
        hyperlink.setId(rId);
        hyperlink.addNewR();
        XWPFHyperlinkRun hyperlinkRun = new XWPFHyperlinkRun(hyperlink, hyperlink.getRArray(0), par);
        hyperlinkRun.setText(text);
        hyperlinkRun.setColor("0000FF");
        hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);

        //reinsert runs back by coping runs from cloned paragraph
        DocxTemplateUtils.getInstance().copyParagraph(parClone, par);
    }

    private String getRealTagText(TagInfo tag) {
        return tag.getTagText().substring(TAG_PREFIX_LINK.length());
    }

}
