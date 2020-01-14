package net.sl.processor;

import net.sl.DocxTemplateFillerContext;
import net.sl.TagInfo;
import net.sl.exception.DocxTemplateFillerException;
import net.sl.tag.TagImageData;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.Document;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * The processor is based on DTO fields
 * <p/>
 * The processor can fill image template placeholders from POJO DTO fields
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class ImageTagProcessor extends AbstractTagProcessor implements TagProcessor {

    public static final String TAG_PREFIX_IMAGE = "image:";
    public static final String PROPERTY_TITLE_REF_NAME = "title";
    public static final String PROPERTY_SOURCE_REF_NAME = "source";
    public static final String PROPERTY_FORMAT_REF_NAME = "imageFormat";
    public static final String PROPERTY_PIXELS_WIDTH_REF_NAME = "width";
    public static final String PROPERTY_PIXELS_HEIGHT_REF_NAME = "height";

    @Override
    public boolean canProcessTag(TagInfo tag) {
        return tag.getTagText().startsWith(TAG_PREFIX_IMAGE);
    }

    @Override
    public IBodyElement process(TagInfo tag, IBodyElement elem, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {
            fillTag(tag, (XWPFParagraph) elem, null, context);
            return elem;
        } catch (IOException e) {
            throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
        }
    }

    /**
     * Returns tag text without prefix
     *
     * @param tag
     * @return
     */
    private String getRealTagText(TagInfo tag) {
        return tag.getTagText().substring(TAG_PREFIX_IMAGE.length());
    }

    @Override
    protected void insertRun(XWPFParagraph par, TagInfo tag, Object tagData, DocxTemplateFillerContext context)
            throws DocxTemplateFillerException {
        try {
            //we read from the context Image contract - the TagImageData interface.
            //the interface has getters to return image attributes
            String tagText = getRealTagText(tag);

            TagImageData image;
            if (StringUtils.isBlank(tagText)) {
                //the image data is value root
                image = (TagImageData) context.getRootValue();
            } else {
                //the image data  referenced from root DTO
                image = (TagImageData) PropertyUtils.getSimpleProperty(context.getRootValue(), tagText);
            }
            int imageFormat = getImageFormat(image);
            XWPFRun targetRun = par.createRun();
            targetRun.addPicture(image.getSourceStream(),
                    imageFormat,
                    image.getTitle(), Units.pixelToEMU(image.getWidth()), Units.pixelToEMU(image.getHeight()));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | IOException | InvalidFormatException e) {
            throw new DocxTemplateFillerException("Cannot access value for tag " + tag.getTagText(), e);
        }

    }

    /**
     * Gets Format by specified content type.
     *
     * @param image image data
     * @return image format or PNG if the content type is empty or unknown.
     */
    private int getImageFormat(TagImageData image) {
        if (image.getContentType() == null) {
            return Document.PICTURE_TYPE_PNG;
        }
        if (image.getContentType().contains("jpeg") || image.getContentType().contains("jpg")) {
            return Document.PICTURE_TYPE_JPEG;
        } else if (image.getContentType().contains("png")) {
            return Document.PICTURE_TYPE_PNG;
        } else if (image.getContentType().contains("tiff")) {
            return Document.PICTURE_TYPE_TIFF;
        } else if (image.getContentType().contains("bmp")) {
            return Document.PICTURE_TYPE_BMP;
        }

        //for all the rest formats
        return Document.PICTURE_TYPE_PNG;
    }
}
