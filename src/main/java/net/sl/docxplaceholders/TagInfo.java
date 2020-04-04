package net.sl.docxplaceholders;

/**
 * Single tag info.
 * <p/>
 * Created on 10/3/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class TagInfo
{
    private String tagText;

    private int tagStartOffset = 0;

    private boolean hasClosingSlash = false;

    public TagInfo(String tagText, int tagStartOffset, boolean hasClosingSlash) {
        this.tagText = tagText;
        this.tagStartOffset = tagStartOffset;
        this.hasClosingSlash = hasClosingSlash;
    }

    public String getTagText()
    {
        return tagText;
    }

    public void setTagText(String tagText)
    {
        this.tagText = tagText;
    }

    public int getTagStartOffset()
    {
        return tagStartOffset;
    }

    public void setTagStartOffset(int tagStartOffset)
    {
        this.tagStartOffset = tagStartOffset;
    }

    @Override
    public String toString() {
        return "TagInfo{" +
                "tagText='" + tagText + '\'' +
                ", tagStartOffset=" + tagStartOffset +
                ", hasClosingSlash=" + hasClosingSlash +
                '}';
    }

    public boolean hasClosingSlash() {
        return hasClosingSlash;
    }

    public void hasClosingSlash(boolean hasClosingSlash) {
        this.hasClosingSlash = hasClosingSlash;
    }
}
