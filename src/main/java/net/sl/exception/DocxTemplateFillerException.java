package net.sl.exception;

/**
 * Template filling related exception.
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateFillerException extends Exception {

    public DocxTemplateFillerException(String message) {
        super(message);
    }

    public DocxTemplateFillerException(String message, Exception e) {
        super(message, e);
    }
}
