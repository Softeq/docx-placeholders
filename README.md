# docx-placeholders

## Introducation
The **Docx-placeholders** is a library which allows defining and filling placeholders in .docx template files. 
There are many cases when business requires a template to be filled with values from java.
The approach allows usual .docx files using as templates where placeholders can be added and programmatically evaluated/filled.

A template .docx file containing "**Hello {firstName} {lastName}**" after evaluation converted to "**Hello John Smith**".

## Documentation
Content:
1. [Quick Start](#quick-start)
2. [Supported tags](#Supported-tags)
    - [Map](#Map-Tag-Processor)
    - [POJO fields and collections](#POJO-fields-and-collections)
    - [POJO nested block](#POJO-nested-block)
    - [Link and Image](#Link-and-Image-tags)
3. [Customize tag start and end tokens](#Customize-tag-start-end)
4. [Samples](#Samples)

### Quick Start

The simplest start is to define a template .docx document with placeholders like **${{firstName}}** and
 **${{lastName}}** and then evaluate (fill) the template with the following code. 
```java
//read template .docx
InputStream templateStream = getClass().getResourceAsStream("/some/resource/MyTemplate.docx");
//create a map with placeholders - key is placeholder name and value is appropriate value
Map<String, String> placeholdersValuesMap = new HashMap<String, String>() {{
    put("firstName", "John");
    put("lastName", "Smith");
}};
//Create context and set just one map tag processor
DocxTemplateFillerContext context = new DocxTemplateFillerContext();
context.setProcessors(Collections.singletonList(new MapTagProcessor(placeholdersValuesMap)));
//create target stream to store the filled template
ByteArrayOutputStream filledTemplateStream = new ByteArrayOutputStream();
//fill the template with the defined placeholders
filler.fillTemplate(templateStream, filledTemplateStream, context); 
```

## Supported tags

### Map Tag Processor
The tag processor is based on a Map<String, String>. Key of the Map is a tag name and value contains text to be used 
when the placeholder is filled. The code example is described in the [Quick Start](#quick-start) section. 
Developer defines a Map with tags to be filled and the values are placed in the template.

The simplest case described above is not enough in many cases. Let's consider a few alternative cases.

### POJO fields and collections
When there is a POJO java object and we need the fields of the POJO to be used in a template another tag processors 
should be used - PojoFieldTagProcessor and PojoCollectionTagProcessor.

A template below shows tags "field" and "collection".

![Alt text](img/pojo-simple-template-example.png?raw=true "POJO based tag processors template")

The POJO could be represented as the following JSON.
```json
{
  "companyName": "TestCompany",
  "projects": [
    {
      "projectName": "Project One"
    },
    {
      "projectName": "Project Two"
    }
  ]
}
```
```java
//read template .docx
InputStream templateStream = getClass().getResourceAsStream("/some/resource/MyTemplate.docx");
//create context and add two tag processors to handle the tags
DocxTemplateFillerContext context = new DocxTemplateFillerContext();
context.setProcessors(Arrays.asList(new PojoCollectionTagProcessor(), new PojoFieldTagProcessor()));

//place the POJO as a value root in the context
context.push(null, createPOJOexampleFromJSON());

//create target stream to store the filled template
ByteArrayOutputStream filledTemplateStream = new ByteArrayOutputStream();
//fill the template with the defined placeholders
filler.fillTemplate(templateStream, filledTemplateStream, context); 
```
After evaluation the filled .docx template is following:
![Alt text](img/pojo-simple-template-evaluated-example.png?raw=true "POJO based tag processors template filled")

How it works?

Each tag is evaluated by own Tag Processor. List of tag processors are defined in the DocxTemplateFillerContext. 
For each detected tag the list is iterated to find the processor which can process the tag. In the described case
one processor can process field tags and another one processes collection. 
The field processing tag gets tag value from the POJO object placed as the DocxTemplateFillerContext root value.

When the collection tag is met the collection processor does the following:
1. Detects tag body - all the body elements - paragraphs and tables between open and close tag.
2. Found the tag referenced collection in the DocxTemplateFillerContext value root.
3. Starts iterating the collection
4. Each collection item is placed to be the new value root.
5. The tag body (filled in the step 1) is cloned, evaluated (tags filled) with the new local value root. 
(So the ${{field:projectName}} finds value in the local root - collection member).
6. The evaluated tag body for the single collection item is inserted.
7. After all the collection items are processed and all the evaluated copies of tag body are inserted the original 
elements are removed (including the tag start and end).

The same approach is applied to the nested tags (e.g. collection in a collection). Company has a projects collection and
each project has a list of developers. Value root is pushed in a stack defined in the DocxTemplateFillerContext and 
restored after tag body evaluation.

## POJO nested block
Suppose there is a POJO User with nested POJO Address
```java
public class UserDto {
    private String firstName;
    private String lastName;
    private AddressDto address;
    //getters and setters
}
public class AddressDto {
    private String country;
    private String city;
    private String street;
    //getters and setters
}
```
To show values of the nested Address POJO the **${{block:address}}** tag. When the "block" tag is met the referenced field
becomes the value root and all the inner "field" tags e.g. **${{field:country}}** use the fields of new value root - Address.
So for the template
![Alt text](img/pojo-nested-block-example.png?raw=true "Nested block template")
And after evaluation the filled template 
![Alt text](img/pojo-nested-block-evaluated-example.png?raw=true "Nested block template")  

## Link and Image tags
There are some cases when just plain text is not enough. The cases when we need a link or image require more than just text.
For such cases separate interfaces were defined:
For links
```java
public interface TagLinkData {
    String getText();
    String getUrl();
    String getColor();
}
```
For images
```java
public interface TagImageData {
    String getTitle();
    String getContentType();
    InputStream getSourceStream();
    Integer getWidth();
    Integer getHeight();
}
``` 
If a link must be inserted in a template a tag "link" should be defined ${{link:/}} if the link is POJO or
${{link:linkField/}} if the link is a field of the value root POJO. For Images ${{image:/}} and ${{image:imageField/}}
In the first case POJO placed in the context root must implement the TagLinkData interface. The methods are used to 
return link attributes - text or the link, reference URL, and the link color.

Image interface includes more methods to define image size, content etc.

## Customize tag start/end

There are different tag open and close tokens. E.g. HTML and XML are supposed to use '<' and '>' chars. 
The library by default use '${{' and '}}' tokens but there is a way to customize them. The following code allows 
to set XML like tokens for tag start/end:

```java
DocxTemplateFillerContext context = new DocxTemplateFillerContext();
context.setTagStart("<");
context.setTagEnd(">");
``` 
After that tags like "&lt;field:firstName&gt;" can be added to the templates to be evaluated.

## Samples
To be defined.