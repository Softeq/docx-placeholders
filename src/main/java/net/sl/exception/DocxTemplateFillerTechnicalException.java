package net.sl.exception;

/**
 * Template filling related technical exception. To be thrown when exception is not expected instead of plain RuntimeException.
 * <p/>
 * Created on 10/4/2019.
 * <p/>
 *
 * @author slapitsky
 */
public class DocxTemplateFillerTechnicalException extends RuntimeException
{

    public DocxTemplateFillerTechnicalException(String message)
    {
        this(message, null);
    }

    public DocxTemplateFillerTechnicalException(Exception e)
    {
        this(null, e);
    }

    public DocxTemplateFillerTechnicalException(String message, Exception e)
    {
        super(message, e);
    }
}
