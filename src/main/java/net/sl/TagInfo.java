package net.sl;

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

    private boolean isTagWithBody = false;

    public TagInfo(String tagText, int tagStartOffset, boolean isTagWithBody)
    {
        this.tagText = tagText;
        this.tagStartOffset = tagStartOffset;
        this.isTagWithBody = isTagWithBody;
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

    public boolean isTagWithBody()
    {
        return isTagWithBody;
    }

    public void setTagWithBody(boolean tagWithBody)
    {
        isTagWithBody = tagWithBody;
    }
}
