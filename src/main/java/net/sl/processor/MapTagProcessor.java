package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import java.util.Map;

/**
 * Map tagValuesMap contains tags with values - key is the tag name, value is the tag value.
 * <p/>
 * The procesor detects procesable tags by keys exit in the map. Then just place appropriate values for the tags defined by keys
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class MapTagProcessor extends AbstractTagProcessor implements TagProcessor {

    private Map<String, String> tagValuesMap;

    public MapTagProcessor(Map<String, String> tagValuesMap) {
        this.tagValuesMap = tagValuesMap;
    }

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tagValuesMap.containsKey(tag.getTagText());
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        fillTagPlaceholderWithValue((XWPFParagraph) elem, tag, getTagValue(tag), context);
        return elem;
    }

    private String getTagValue(TagInfo tag) {
        //get tag text value and store to the run
        String tagValue = tagValuesMap.get(tag.getTagText());
        if (tagValue == null) {
            tagValue = StringUtils.EMPTY;
        }

        return tagValue;
    }

}
