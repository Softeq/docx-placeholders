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
 * All the collateral methods used to fill templates' placeholders in tag processors.
 * <p/>
 * Created on 10/2/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateUtils {

    public static final String PAGE_BREAK = "\n";

    public static final String COMMA_SEPARATOR = ", ";

    public static final String DEFAULT_TAG_START = "${{";

    public static final String DEFAULT_TAG_END = "}}";

    public static final String HYPERLINK_DEFAULT_COLOR = "0000FF";

    private static final String DOC_ELEMENT_TYPE_PARAGRAPH = "PARAGRAPH";

    private static final String DOC_ELEMENT_TYPE_TABLE = "TABLE";

    private static DocxTemplateUtils instance = new DocxTemplateUtils();

    public static DocxTemplateUtils getInstance() {
        return instance;
    }

    /**
     * Creates deep cloned document (root to the copies of the specified list of body elements)
     *
     * @param source list of body elements to be cloned
     * @return a document - new root for cloned elements
     * @throws IOException
     */
    public XWPFDocument deepCloneElements(List<IBodyElement> source) throws IOException {
        //create a new document to add copies to
        try (XWPFDocument targetDoc = new XWPFDocument()) {
            for (IBodyElement bodyElement : source) {
                BodyElementType elementType = bodyElement.getElementType();

                //for each element (table or paragraph) create a copy and add to the document root
                if (elementType.name().equals(DOC_ELEMENT_TYPE_PARAGRAPH)) {
                    XWPFParagraph pr = (XWPFParagraph) bodyElement;
                    pr.getRuns().forEach(run -> {
                        if (PAGE_BREAK.equals(run.text())) {
                            pr.setPageBreak(true);
                        }
                    });
                    XWPFParagraph newPr = targetDoc.createParagraph();
                    newPr.getCTP().setPPr(pr.getCTP().getPPr());
                    int pos = targetDoc.getParagraphs().size() - 1;
                    targetDoc.setParagraph(pr, pos);
                } else if (elementType.name().equals(DOC_ELEMENT_TYPE_TABLE)) {
                    XWPFTable table = (XWPFTable) bodyElement;
                    XWPFTable newTbl = targetDoc.createTable();
                    newTbl.getCTTbl().setTblPr(table.getCTTbl().getTblPr());
                    int pos = targetDoc.getTables().size() - 1;
                    targetDoc.setTable(pos, table);
                }
            }

            //create real copy by saving the doc and read it back
            //the logic is used to prevent locks and reusing using subelements' properties - deep clone
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            targetDoc.write(out);
            XWPFDocument resDoc = new XWPFDocument(new ByteArrayInputStream(out.toByteArray()));
            return resDoc.getXWPFDocument();
        }
    }

    /**
     * Copies content (runs) and all the properties (PRs) of source paraghraph to the target paragraph.
     *
     * @param source
     * @param target
     */
    public void copyParagraph(XWPFParagraph source, XWPFParagraph target) {
        target.getCTP().setPPr(source.getCTP().getPPr());
        // copy hyperlinks
        Arrays.stream(source.getCTP().getHyperlinkArray()).forEach(sourceHyperlink ->
                addHyperlink(target, sourceHyperlink.getAnchor(), sourceHyperlink.getId(), HYPERLINK_DEFAULT_COLOR)
        );
        for (int i = 0; i < source.getRuns().size(); i++) {
            XWPFRun run = source.getRuns().get(i);
            XWPFRun targetRun = target.createRun();
            //copy formatting
            targetRun.getCTR().setRPr(run.getCTR().getRPr());
            if (run.getEmbeddedPictures().isEmpty()) {
                //no images just copy text
                targetRun.setText(getRunText(run));
            } else {
                //need to copy image's content
                for (XWPFPicture picture : run.getEmbeddedPictures()) {
                    //get source image width and height
                    long w = picture.getCTPicture().getSpPr().getXfrm().getExt().getCx();
                    long h = picture.getCTPicture().getSpPr().getXfrm().getExt().getCy();
                    XWPFPictureData pictureData = picture.getPictureData();
                    byte[] img = pictureData.getData();
                    String fileName = pictureData.getFileName();
                    int imageFormat = pictureData.getPictureType();
                    try {
                        targetRun.addPicture(new ByteArrayInputStream(img),
                                imageFormat,
                                fileName,
                                (int) w, (int) h);
                    } catch (InvalidFormatException | IOException e) {
                        throw new DocxTemplateFillerTechnicalException("Unexpected image inserting error ", e);
                    }
                }
            }
        }
    }

    /**
     * Returns run's text
     *
     * @param run
     * @return text of the run
     */
    public String getRunText(XWPFRun run) {
        StringBuilder sb = new StringBuilder();
        int partsCount = run.getCTR().getTArray().length;
        for (int i = 0; i < partsCount; i++) {
            sb.append(run.getText(i));
        }
        return sb.toString();
    }

    /**
     * Copies all content of table (rows/cells' properties and inner content) from source to target.
     *
     * @param source
     * @param target
     */
    public void copyTable(XWPFTable source1, XWPFTable target) throws IOException {
        boolean isParentTableCell = target.getBody() instanceof XWPFTableCell;
        XWPFTable source = (XWPFTable) deepCloneElements(Collections.singletonList(source1)).getBodyElements().get(0);
        target.getCTTbl().setTblPr(source.getCTTbl().getTblPr());
        target.getCTTbl().setTblGrid(source.getCTTbl().getTblGrid());
        for (int r = 0; r < source.getRows().size(); r++) {
            //we may need to add row and then delete
            XWPFTableRow targetRow;
            if (!isParentTableCell) {
                targetRow = r == 0 && !target.getRows().isEmpty() ? target.getRows().get(0) : target.createRow();
            } else {
                targetRow = target.createRow();
            }
            XWPFTableRow row = source.getRows().get(r);
            targetRow.getCtRow().setTrPr(row.getCtRow().getTrPr());
            for (int c = 0; c < row.getTableCells().size(); c++) {
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

    /**
     * Copies all content of cell from source to target.
     *
     * @param targetCell target
     * @param cell       source
     * @throws IOException
     */
    private void copyTableCell(XWPFTableCell targetCell, XWPFTableCell cell) throws IOException {
        targetCell.getCTTc().setTcPr(cell.getCTTc().getTcPr());
        XmlCursor cursor = targetCell.getParagraphArray(0).getCTP().newCursor();
        for (int p = 0; p < cell.getBodyElements().size(); p++) {
            IBodyElement elem = cell.getBodyElements().get(p);
            if (elem instanceof XWPFParagraph) {
                XWPFParagraph targetPar = targetCell.insertNewParagraph(cursor);
                cursor.toNextToken();
                XWPFParagraph par = (XWPFParagraph) elem;
                copyParagraph(par, targetPar);
                targetPar.getRuns();
            } else if (elem instanceof XWPFTable) {
                XWPFTable targetTable = targetCell.insertNewTbl(cursor);
                XWPFTable table = (XWPFTable) elem;
                copyTable(deepCloneTable(table), targetTable);
                cursor.toNextToken();
            }
        }
    }

    /**
     * Creates deep cloned document with the table copy and returns cloned table
     *
     * @param source source table
     * @return
     * @throws IOException
     */
    private XWPFTable deepCloneTable(XWPFTable source) throws IOException {
        XWPFDocument copyDoc = deepCloneElements(Collections.singletonList(source));

        return copyDoc.getTables().get(0);
    }

    /**
     * Adds hiperlink to the specified paragraph
     *
     * @param par  target paragraph to add hiperlink
     * @param text hiperlink text
     * @param url  hiperlink reference URL
     */
    public void addHyperlink(XWPFParagraph par, String text, String url, String color) {
        String rId = par.getDocument().getPackagePart().addExternalRelationship(url, XWPFRelation.HYPERLINK.getRelation()).getId();
        CTHyperlink hyperlink = par.getCTP().addNewHyperlink();
        hyperlink.setId(rId);
        hyperlink.addNewR();
        XWPFHyperlinkRun hyperlinkRun = new XWPFHyperlinkRun(hyperlink, hyperlink.getRArray(0), par);
        hyperlinkRun.setText(text);
        hyperlinkRun.setColor(color);
        hyperlinkRun.setUnderline(UnderlinePatterns.SINGLE);
    }

    /**
     * Returns next sibling body element
     *
     * @param elem source element
     * @return next body element on the same level (the same parent).
     */
    public IBodyElement getNextSibling(IBodyElement elem) {
        for (int i = 0; i < elem.getBody().getBodyElements().size() - 1; i++) {
            if (elem.getBody().getBodyElements().get(i) == elem) {
                return elem.getBody().getBodyElements().get(i + 1);
            }
        }

        return null;
    }

    /**
     * @param element
     * @return index of the element (number of child of the element's parent)
     */
    public int getElementIndex(IBodyElement element) {
        IBody body = element.getBody();
        for (int i = 0; i < body.getBodyElements().size(); i++) {
            if (body.getBodyElements().get(i) == element) {
                return i;
            }
        }

        return -1;
    }

    /**
     * Gets tag (if exists) in the specified body element
     *
     * @param elem    body element
     * @param context filler context to define tag start and end tokens
     * @return
     * @throws DocxTemplateFillerException
     */
    public TagInfo getTag(IBodyElement elem, DocxTemplateFillerContext context) throws DocxTemplateFillerException {
        return getTag(elem, 0, context);
    }

    /**
     * Gets tag (if exists) in the specified body element after offset (e.g. tag in the mid of paragraph)
     *
     * @param elem    body element
     * @param offset  start offset (e.g. mid of the paragraph)
     * @param context filler context to define tag start and end tokens
     * @return
     * @throws DocxTemplateFillerException
     */
    public TagInfo getTag(IBodyElement elem, int offset, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        if (elem instanceof XWPFParagraph) {
            String text = ((XWPFParagraph) elem).getText();
            int tagStartOffset = text.indexOf(context.getTagStart(), offset);
            if (tagStartOffset >= 0) {
                int tagEndOffset = text.indexOf(context.getTagEnd(), tagStartOffset);
                if (tagEndOffset < 0) {
                    throw new DocxTemplateFillerException("No closing tag found for line " + text);
                }

                String tagText = text.substring(tagStartOffset + context.getTagStart().length(), tagEndOffset);
                boolean isTagWithBody = !tagText.endsWith("/");
                if (!isTagWithBody) {
                    tagText = tagText.substring(0, tagText.length() - 1);
                }
                return new TagInfo(tagText, tagStartOffset, isTagWithBody);
            }
        }
        return null;
    }

    /**
     * Fills tags placeholders in the specified body element
     *
     * @param body
     * @param context
     * @throws DocxTemplateFillerException
     */
    public void fillTags(IBody body, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        fillTags(body.getBodyElements(), context);
    }

    /**
     * Fills tags placeholders in the specified table
     *
     * @param table
     * @param context
     * @throws DocxTemplateFillerException
     */
    public void fillTags(XWPFTable table, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {

        for (int r = 0; r < table.getRows().size(); r++) {
            XWPFTableRow row = table.getRows().get(r);
            for (int c = 0; c < row.getTableCells().size(); c++) {
                fillTags(row.getTableCells().get(c), context);
            }
        }
    }

    /**
     * Fills tags placeholders in the specified body elements list
     *
     * @param bodyElements
     * @param context
     * @throws DocxTemplateFillerException
     */
    public void fillTags(List<IBodyElement> bodyElements, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        if (!bodyElements.isEmpty()) {
            IBodyElement bodyElem = bodyElements.get(0);
            while (bodyElem != null) {
                TagInfo tag = DocxTemplateUtils.getInstance().getTag(bodyElem, context);
                if (tag != null) {
                    bodyElem = context.process(tag, bodyElem);
                    continue;
                } else if (bodyElem instanceof XWPFTable) {
                    fillTags((XWPFTable) bodyElem, context);
                }
                bodyElem = DocxTemplateUtils.getInstance().getNextSibling(bodyElem);
            }
        }
    }

    /**
     * Inserts elements (children of specified source body) after the target paragraph
     *
     * @param sourceBody  root for the elements to be inserted
     * @param tagStartPar target paragraph to insert elements after
     * @throws IOException
     */
    public void insertBodyElementsAfterParagraph(IBody sourceBody, XWPFParagraph tagStartPar) throws IOException {
        XWPFDocument doc = tagStartPar.getDocument();
        for (IBodyElement bodyElement : sourceBody.getBodyElements()) {
            BodyElementType elementType = bodyElement.getElementType();

            XmlCursor cursor = tagStartPar.getCTP().newCursor();
            if (elementType.name().equals(DOC_ELEMENT_TYPE_PARAGRAPH)) {
                XWPFParagraph pr = (XWPFParagraph) bodyElement;
                XWPFParagraph newPar;
                //we insert a new paragraph in the document and then apply body element to the paragraph (in fact copy)
                if (tagStartPar.getBody() instanceof XWPFTableCell) {
                    XWPFTableCell parentCell = (XWPFTableCell) tagStartPar.getBody();
                    newPar = parentCell.insertNewParagraph(cursor);
                } else {
                    newPar = doc.insertNewParagraph(cursor);
                }
                copyParagraph(pr, newPar);
            } else if (elementType.name().equals(DOC_ELEMENT_TYPE_TABLE)) {
                XWPFTable table = (XWPFTable) bodyElement;
                XWPFTable newTbl;
                if (tagStartPar.getBody() instanceof XWPFTableCell) {
                    XWPFTableCell parentCell = (XWPFTableCell) tagStartPar.getBody();
                    newTbl = parentCell.insertNewTbl(cursor);
                } else {
                    newTbl = doc.insertNewTbl(cursor);
                }
                copyTable(table, newTbl);
            }
        }
    }

    /**
     * Stores the specified docuemnt to file. Used for tests mostly
     *
     * @param doc
     * @param fileName
     */
    public void storeDocToFile(XWPFDocument doc, String fileName) {
        try (FileOutputStream fos = new FileOutputStream(fileName)) {
            doc.write(fos);
        } catch (IOException ex) {
            throw new DocxTemplateFillerTechnicalException(ex);
        }
    }

    /**
     * Removes 'count' child elements starting from the 'startIndex' of the 'parent'
     *
     * @param parent
     * @param startIndex
     * @param count
     */
    public void removeElements(IBody parent, int startIndex, int count) {
        if (parent instanceof XWPFTableCell) {
            XWPFTableCell cell = (XWPFTableCell) parent;
            removeElementFromTableCell(startIndex, count, cell);
        } else {
            for (int i = 0; i < count; i++) {
                parent.getXWPFDocument().removeBodyElement(startIndex);
            }
        }
    }

    /**
     * Removes 'count' child elements starting from the 'startIndex' from the cell.
     *
     * @param startIndex
     * @param count
     * @param cell
     */
    private void removeElementFromTableCell(int startIndex, int count, XWPFTableCell cell) {
        //cell has no simple access to the children elements
        //getter returns just a copy so we use a separate method to extract proper collection
        List<IBodyElement> bodyElementsRef = getBodyElementsRef(cell);
        boolean isLastParagraph = false;
        for (int i = 0; i < count; i++) {
            IBodyElement element = bodyElementsRef.get(startIndex);
            if (element instanceof XWPFParagraph) {
                int realParIndex = getParagraphIndex(bodyElementsRef, (XWPFParagraph) element);
                isLastParagraph = realParIndex == cell.getParagraphs().size() - 1;
                if (isLastParagraph) {
                    clearRuns((XWPFParagraph) element);
                    break;
                } else {
                    cell.getParagraphs().remove(realParIndex);
                    cell.getCTTc().removeP(realParIndex);
                }
            } else if (element instanceof XWPFTable) {
                int realTableIndex = getTableIndex(bodyElementsRef, (XWPFTable) element);
                getTablesRef(cell).remove(realTableIndex);
                cell.getCTTc().removeTbl(realTableIndex);
            }
            if (!isLastParagraph) {
                bodyElementsRef.remove(startIndex);
            }
        }
    }

    /**
     * Removes the paragrraph runs but the last one must remain with empty string content
     *
     * @param par
     */
    public void clearRuns(XWPFParagraph par) {
        while (par.getRuns().size() > 1) {
            par.removeRun(0);
        }
        XWPFRun run = par.getRuns().get(0);
        run.setText("", 0);
    }

    /**
     * Gets index of the paragraph in the specified list (or -1) if the par is not found
     *
     * @param bodyElementsRef
     * @param paragraph
     * @return
     */
    private int getParagraphIndex(List<IBodyElement> bodyElementsRef, XWPFParagraph paragraph) {
        int index = -1;
        for (IBodyElement elem : bodyElementsRef) {
            if (elem instanceof XWPFParagraph) {
                index++;
            }
            if (elem == paragraph) {
                return index;
            }
        }
        return -1;
    }

    /**
     * Gets index of the table in the specified list (or -1) if the table is not found
     *
     * @param bodyElementsRef
     * @param table
     * @return
     */
    private int getTableIndex(List<IBodyElement> bodyElementsRef, XWPFTable table) {
        int index = -1;
        for (IBodyElement elem : bodyElementsRef) {
            if (elem instanceof XWPFTable) {
                index++;
            }
            if (elem == table) {
                return index;
            }
        }
        return -1;
    }

    /**
     * It's a dirty hack but I cannot find proper way to remove elements from cell's body elements.
     * Reflection is used to extract direct reference to the cell children list
     *
     * @param cell
     * @return
     */
    private List<IBodyElement> getBodyElementsRef(XWPFTableCell cell) {
        try {
            Field beField = cell.getClass().getDeclaredField("bodyElements");
            beField.setAccessible(true);
            return (List<IBodyElement>) beField.get(cell);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new DocxTemplateFillerTechnicalException(e);
        }
    }

    /**
     * It's a dirty hack but I cannot find proper way to remove elements from cell's tables list
     * Reflection is used to extract direct reference to the cell children list
     *
     * @param cell
     * @return
     */
    private List<XWPFTable> getTablesRef(XWPFTableCell cell) {
        try {
            Field beField = cell.getClass().getDeclaredField("tables");
            beField.setAccessible(true);
            return (List<XWPFTable>) beField.get(cell);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new DocxTemplateFillerTechnicalException(e);
        }
    }
}
