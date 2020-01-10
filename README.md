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
    - [Map](#Map)
    - [POJO field](#Field)
    - [POJO Collection](#Collection)    
    - [Link](#Link)
    - [Image](#Image)
3. [Customize tag start and end tokens](#Customize-tag-start-end)
4. [Samples](#Samples)

### Quick Start

The simplest start is to define a template .docx document whith placeholders like ${{firstName}} and $ {{lastName}} and 
then evaluate (fill) the template with the following code. 
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

### POJO field
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


After evaluation the filled .docx template is following:
![Alt text](img/pojo-simple-template-evaluated-example.png?raw=true "POJO based tag processors template filled")

## Customize tag start/end

There are different tag open and close tokens. E.g. HTML and XML are supposed to use '<' and '>' chars. 
The library by default use '${{' and '}}' tokens but there is a way to customize them. The following code allows 
to set XML like tokens for tag start/end:

```java
DocxTemplateFillerContext context = new DocxTemplateFillerContext();
context.setTagStart("<");
context.setTagEnd(">");
``` 