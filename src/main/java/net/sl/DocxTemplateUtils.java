package net.sl;

import net.sl.exception.DocxTemplateFillerException;
import net.sl.exception.DocxTemplateFillerTechnicalException;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.xwpf.usermodel.BodyElementType;
import org.apache.poi.xwpf.usermodel.IBody;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFHyperlinkRun;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.XWPFPictureData;
import org.apache.poi.xwpf.usermodel.XWPFRelation;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTHyperlink;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * All the collateral methods used to fill templates logic and processors.
 * <p/>
 * Created on 10/2/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateUtils
{
    public static final String PAGE_BREAK = "\n";

    public static final String COMMA_SEPARATOR = ", ";

    public static final String DEFAULT_TAG_START = "${{";

    public static final String DEFAULT_TAG_END = "}}";

    public static final String HYPERLINK_DEFAULT_COLOR = "0000FF";

    private static final String DOC_ELEMENT_TYPE_PARAGRAPH = "PARAGRAPH";

    private static final String DOC_ELEMENT_TYPE_TABLE = "TABLE";

    private static DocxTemplateUtils instance = new DocxTemplateUtils();

    public static DocxTemplateUtils getInstance()
    {
        return instance;
    }

    public XWPFDocument deepCloneElements(List<IBodyElement> source) throws IOException
    {
        try (XWPFDocument targetDoc = new XWPFDocument())
        {
            for (IBodyElement bodyElement : source)
            {
                BodyElementType elementType = bodyElement.getElementType();

                if (elementType.name().equals(DOC_ELEMENT_TYPE_PARAGRAPH))
                {
                    XWPFParagraph pr = (XWPFParagraph) bodyElement;
                    pr.getRuns().forEach(run -> {
                        if (PAGE_BREAK.equals(run.text()))
                        {
                            pr.setPageBreak(true);
                        }
                    });
                    XWPFParagraph newPr = targetDoc.createParagraph();
                    newPr.getCTP().setPPr(pr.getCTP().getPPr());
                    int pos = targetDoc.getParagraphs().size() - 1;
                    targetDoc.setParagraph(pr, pos);
                }
                else if (elementType.name().equals(DOC_ELEMENT_TYPE_TABLE))
                {
                    XWPFTable table = (XWPFTable) bodyElement;
                    XWPFTable newTbl = targetDoc.createTable();
                    newTbl.getCTTbl().setTblPr(table.getCTTbl().getTblPr());
                    int pos = targetDoc.getTables().size() - 1;
                    targetDoc.setTable(pos, table);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            targetDoc.write(out);
            XWPFDocument resDoc = new XWPFDocument(new ByteArrayInputStream(out.toByteArray()));
            return resDoc.getXWPFDocument();
        }
    }

    public void copyParagraph(XWPFParagraph source, XWPFParagraph target)
    {
        target.getCTP().setPPr(source.getCTP().getPPr());
        // copy hyperlinks
        Arrays.stream(source.getCTP().getHyperlinkArray()).forEach(sourceHyperlink ->
                addHyperlink(target, sourceHyperlink.getAnchor(), sourceHyperlink.getId())
        );
        for (int i = 0; i < source.getRuns().size(); i++)
        {
            XWPFRun run = source.getRuns().get(i);
            XWPFRun targetRun = target.createRun();
            //copy formatting
            targetRun.getCTR().setRPr(run.getCTR().getRPr());
            if (run.getEmbeddedPictures().isEmpty())
            {
                //no images just copy text
                targetRun.setText(getRunText(run));
            }
            else
            {
                //need to copy image's content
                for (XWPFPicture picture : run.getEmbeddedPictures())
                {
                    //get source image width and height
                    long w = picture.getCTPicture().getSpPr().getXfrm().getExt().getCx();
                    long h = picture.getCTPicture().getSpPr().getXfrm().getExt().getCy();
                    XWPFPictureData pictureData = picture.getPictureData();
                    byte[] img = pictureData.getData();
                    String fileName = pictureData.getFileName();
                    int imageFormat = pictureData.getPictureType();
                    try
                    {
                        targetRun.addPicture(new ByteArrayInputStream(img),
                                imageFormat,
                                fileName,
                                (int) w, (int) h);
                    }
                    catch (InvalidFormatException | IOException e)
                    {
                        throw new DocxTemplateFillerTechnicalException("Unexpected image inserting error ", e);
                    }
                }
            }
        }
    }

    public String getRunText(XWPFRun run)
    {
        StringBuilder sb = new StringBuilder();
        int partsCount = run.getCTR().getTArray().length;
        for (int i = 0; i < partsCount; i++)
        {
            sb.append(run.getText(i));
        }
        return sb.toString();
    }

    /**
     * Copies all content of table (rows/cells and inner content) from source to target.
     *
     * @param source
     * @param target
     */
    public void copyTable(XWPFTable source1, XWPFTable target) throws IOException
    {
        boolean isParentTableCell = target.getBody() instanceof XWPFTableCell;
        XWPFTable source = (XWPFTable) deepCloneElements(Collections.singletonList(source1)).getBodyElements().get(0);
        target.getCTTbl().setTblPr(source.getCTTbl().getTblPr());
        target.getCTTbl().setTblGrid(source.getCTTbl().getTblGrid());
        for (int r = 0; r < source.getRows().size(); r++)
        {
            //we may need to add row and then delete
            XWPFTableRow targetRow;
            if (!isParentTableCell)
            {
                targetRow = r == 0 && !target.getRows().isEmpty() ? target.getRows().get(0) : target.createRow();
            }
            else
            {
                targetRow = target.createRow();
            }
            XWPFTableRow row = source.getRows().get(r);
            targetRow.getCtRow().setTrPr(row.getCtRow().getTrPr());
            for (int c = 0; c < row.getTableCells().size(); c++)
            {
                //newly created row has 1 cell
                XWPFTableCell targetCell = c == 0 && !targetRow.getTableCells().isEmpty() ? targetRow.getTableCells().get(0)
                        : targetRow.createCell();
                XWPFTableCell cell = row.getTableCells().get(c);
                copyTableCell(targetCell, cell);
                //newly created cell has one default paragraph we need to remove
                targetCell.removeParagraph(targetCell.getParagraphs().size() - 1);
            }
        }

    }

    private void copyTableCell(XWPFTableCell targetCell, XWPFTableCell cell) throws IOException
    {
        targetCell.getCTTc().setTcPr(cell.getCTTc().getTcPr());
        XmlCursor cursor = targetCell.getParagraphArray(0).getCTP().newCursor();
        for (int p = 0; p < cell.getBodyElements().size(); p++)
        {
            IBodyElement elem = cell.getBodyElements().get(p);
            if (elem instanceof XWPFParagraph)
            {
                XWPFParagraph targetPar = targetCell.insertNewParagraph(cursor);
                cursor.toNextToken();
                XWPFParagraph par = (XWPFParagraph) elem;
                copyParagraph(par, targetPar);
                targetPar.getRuns();
            }
            else if (elem instanceof XWPFTable)
            {
                XWPFTable targetTable = targetCell.insertNewTbl(cursor);
                XWPFTable table = (XWPFTable) elem;
                copyTable(deepCloneTable(table), targetTable);
                cursor.toNextToken();
            }
        }
    }

    private XWPFTable deepCloneTable(XWPFTable source) throws IOException
    {
        XWPFDocument copyDoc = deepCloneElements(Collections.singletonList(source));

        return copyDoc.getTables().get(0);
    }

    public void addHyperlink(XWPFParagraph par, String text, String url)
    {
        String rId = par.getDocument().getPackagePart().addExternalRelationship(url, XWPFRelation.HYPERLINK.getRelation()).getId();
        CTHyperlink hyperlink = par.getCTP().addNewHyperlink();
        hyperlink.setId(rId);
        hyperlink.addNewR();
        XWPFHyperlinkRun hyperlinkRun = new XWPFHyperlinkRun(hyperlink, hyperlink.getRArray(0), par);
        hyperlinkRun.setText(text);
        hyperlinkRun.setColor(HYPERLINK_DEFAULT_COLOR);
        hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);
    }

    public IBodyElement getNextSibling(IBodyElement element)
    {
        IBody body = element.getBody();
        for (int i = 0; i < body.getBodyElements().size(); i++)
        {
            if (body.getBodyElements().get(i) == element)
            {
                if (i + 1 < body.getBodyElements().size())
                {
                    return body.getBodyElements().get(i + 1);
                }
            }
        }

        return null;
    }

    public int getElementIndex(IBodyElement element)
    {
        IBody body = element.getBody();
        for (int i = 0; i < body.getBodyElements().size(); i++)
        {
            if (body.getBodyElements().get(i) == element)
            {
                return i;
            }
        }

        return -1;
    }

    public TagInfo getTag(IBodyElement elem, DocxTemplateFillerContext context) throws DocxTemplateFillerException
    {
        return getTag(elem, 0, context);
    }

    public TagInfo getTag(IBodyElement elem, int offset, DocxTemplateFillerContext context) throws DocxTemplateFillerException
    {
        if (elem instanceof XWPFParagraph)
        {
            String text = ((XWPFParagraph) elem).getText();
            int tagStartOffset = text.indexOf(context.getTagStart(), offset);
            if (tagStartOffset >= 0)
            {
                int tagEndOffset = text.indexOf(context.getTagEnd(), tagStartOffset);
                if (tagEndOffset < 0)
                {
                    throw new DocxTemplateFillerException("No closing tag found for line " + text);
                }

                String tagText = text.substring(tagStartOffset + context.getTagStart().length(), tagEndOffset);
                boolean isTagWithBody = !tagText.endsWith("/");
                if (!isTagWithBody)
                {
                    tagText = tagText.substring(0, tagText.length() - 1);
                }
                return new TagInfo(tagText, tagStartOffset, isTagWithBody);
            }
        }
        return null;
    }

    public void fillTags(IBody body, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException
    {
        fillTags(body.getBodyElements(), context);
    }

    public void fillTags(XWPFTable table, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException
    {

        for (int r = 0; r < table.getRows().size(); r++)
        {
            XWPFTableRow row = table.getRows().get(r);
            for (int c = 0; c < row.getTableCells().size(); c++)
            {
                fillTags(row.getTableCells().get(c), context);
            }
        }
    }

    public void fillTags(List<IBodyElement> bodyElements, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException
    {
        if (!bodyElements.isEmpty())
        {
            IBodyElement bodyElem = bodyElements.get(0);
            while (bodyElem != null)
            {
                TagInfo tag = DocxTemplateUtils.getInstance().getTag(bodyElem, context);
                if (tag != null)
                {
                    bodyElem = context.process(tag, bodyElem);
                    continue;
                }
                else if (bodyElem instanceof XWPFTable)
                {
                    fillTags((XWPFTable) bodyElem, context);
                }
                bodyElem = context.getNextSibling(bodyElem);
            }
        }
    }

    public void insertBodyElementsAfterParagraph(IBody sourceBody, XWPFParagraph tagStartPar) throws IOException
    {
        XWPFDocument doc = tagStartPar.getDocument();
        for (IBodyElement bodyElement : sourceBody.getBodyElements())
        {
            BodyElementType elementType = bodyElement.getElementType();

            XmlCursor cursor = tagStartPar.getCTP().newCursor();
            if (elementType.name().equals(DOC_ELEMENT_TYPE_PARAGRAPH))
            {
                XWPFParagraph pr = (XWPFParagraph) bodyElement;
                XWPFParagraph newPar;
                //we insert a new paragraph in the document and then apply body element to the paragraph (in fact copy)
                if (tagStartPar.getBody() instanceof XWPFTableCell)
                {
                    XWPFTableCell parentCell = (XWPFTableCell) tagStartPar.getBody();
                    newPar = parentCell.insertNewParagraph(cursor);
                }
                else
                {
                    newPar = doc.insertNewParagraph(cursor);
                }
                copyParagraph(pr, newPar);
            }
            else if (elementType.name().equals(DOC_ELEMENT_TYPE_TABLE))
            {
                XWPFTable table = (XWPFTable) bodyElement;
                XWPFTable newTbl;
                if (tagStartPar.getBody() instanceof XWPFTableCell)
                {
                    XWPFTableCell parentCell = (XWPFTableCell) tagStartPar.getBody();
                    newTbl = parentCell.insertNewTbl(cursor);
                }
                else
                {
                    newTbl = doc.insertNewTbl(cursor);
                }
                copyTable(table, newTbl);
            }
        }
    }

    public void storeDocToFile(XWPFDocument doc, String fileName)
    {
        try (FileOutputStream fos = new FileOutputStream(fileName))
        {
            doc.write(fos);
        }
        catch (IOException ex)
        {
            throw new DocxTemplateFillerTechnicalException(ex);
        }
    }

    public void removeElements(IBody parent, int startIndex, int count)
    {
        if (parent instanceof XWPFTableCell)
        {
            XWPFTableCell cell = (XWPFTableCell) parent;
            List<IBodyElement> bodyElementsRef = getBodyElementsRef(cell);
            removeElementFromTableCell(startIndex, count, cell, bodyElementsRef);
        }
        else
        {
            for (int i = 0; i < count; i++)
            {
                parent.getXWPFDocument().removeBodyElement(startIndex);
            }
        }
    }

    private void removeElementFromTableCell(int startIndex, int count, XWPFTableCell cell, List<IBodyElement> bodyElementsRef)
    {
        boolean isLastParagraph = false;
        for (int i = 0; i < count; i++)
        {
            IBodyElement element = bodyElementsRef.get(startIndex);
            if (element instanceof XWPFParagraph)
            {
                int realParIndex = getParagraphIndex(bodyElementsRef, (XWPFParagraph) element);
                isLastParagraph = realParIndex == cell.getParagraphs().size() - 1;
                if (isLastParagraph)
                {
                    clearRuns((XWPFParagraph) element);
                    break;
                }
                else
                {
                    cell.getParagraphs().remove(realParIndex);
                    cell.getCTTc().removeP(realParIndex);
                }
            }
            else if (element instanceof XWPFTable)
            {
                int realTableIndex = getTableIndex(bodyElementsRef, (XWPFTable) element);
                getTablesRef(cell).remove(realTableIndex);
                cell.getCTTc().removeTbl(realTableIndex);
            }
            if (!isLastParagraph)
            {
                bodyElementsRef.remove(startIndex);
            }
        }
    }

    public void clearRuns(XWPFParagraph par)
    {
        while (par.getRuns().size() > 1)
        {
            par.removeRun(0);
        }
        XWPFRun run = par.getRuns().get(0);
        run.setText("", 0);
    }

    private int getParagraphIndex(List<IBodyElement> bodyElementsRef, XWPFParagraph paragraph)
    {
        int index = -1;
        for (IBodyElement elem : bodyElementsRef)
        {
            if (elem instanceof XWPFParagraph)
            {
                index++;
            }
            if (elem == paragraph)
            {
                return index;
            }
        }
        return -1;
    }

    private int getTableIndex(List<IBodyElement> bodyElementsRef, XWPFTable table)
    {
        int index = -1;
        for (IBodyElement elem : bodyElementsRef)
        {
            if (elem instanceof XWPFTable)
            {
                index++;
            }
            if (elem == table)
            {
                return index;
            }
        }
        return -1;
    }

    /**
     * It's a dirty hack but I cannot find proper way to remove elements from cell's body elements
     *
     * @param cell
     * @return
     */
    private List<IBodyElement> getBodyElementsRef(XWPFTableCell cell)
    {
        try
        {
            Field beField = cell.getClass().getDeclaredField("bodyElements");
            beField.setAccessible(true);
            return (List<IBodyElement>) beField.get(cell);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new DocxTemplateFillerTechnicalException(e);
        }
    }

    /**
     * It's a dirty hack but I cannot find proper way to remove elements from cell's tables list
     *
     * @param cell
     * @return
     */
    private List<XWPFTable> getTablesRef(XWPFTableCell cell)
    {
        try
        {
            Field beField = cell.getClass().getDeclaredField("tables");
            beField.setAccessible(true);
            return (List<XWPFTable>) beField.get(cell);
        }
        catch (NoSuchFieldException | IllegalAccessException e)
        {
            throw new DocxTemplateFillerTechnicalException(e);
        }
    }
}
