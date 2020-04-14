# Akka Stream from script

This is an example of a script language for generating Akka stream source.
It is intended to stream big files from databases or to perform any ETL task, that ends in creation of a character stream.

## tl;dr
com.mdanetzky.streamscript.integration.XmlFileGeneratorTest\
In this class you will find tests showing how the script can be used to export and validate XML stream.

## How to use it
Write a script, that will create the output stream.
```xml
<?xml version="1.0"?>
<Data>
  {query: select id, text from table;id|int:text|string}
  <Dataset>
      <Id>{var:id/}</Id>
      <Text>{var:text/}</Text>
  </Dataset>
  {/query}
</Data>
```
Create a Slick database session and put into Map<String, Object> as "slick.session".\
Then pass them both to:
```java
Source<ByteString, NotUsed> ScriptParser.run(String script, Map<String, Object> context);
```
It wil create reactive source, that will only read database when the stream is advancing.\
You can also execute dependent sub queries like so:
```xml
<?xml version="1.0"?>
<Data>
  {query: select id, text from table;id|int:text|string}
  <Dataset>
      <Id>{var:id/}</Id>
      <Text>{var:text/}</Text>
      {query: subtext from subtable where text = (:text);subtext|string}
        <Subtext>{var:subtext/}</Subtext>
      {/query}
  </Dataset>
  {/query}
</Data>
```
Or you can execute simple javascript:
```xml
<?xml version="1.0"?>
<Data>
  {js: variable = 123}
  <Dataset>
      <Value>{var:variable/}</Value>
  </Dataset>
  {/js}
</Data>
```
Or do some conditionals:
```xml
<?xml version="1.0"?>
<Data>
  {query: select id, text from table;id|int:text|string}
  {if: text != 'do not include into xml'}
  <Dataset>
      <Id>{var:id/}</Id>
      <Text>{var:text/}</Text>
  </Dataset>
  {/if}
  {/query}
</Data>
```
Or CSV for that matter:
```csv
ID,Text
{query: select id, text from table;id|int:text|string}
{var:id/},{var:text/}
{/query}
```

## Extending the language
Scripts can also be extended by adding new Commands. A Command is a class, that realizes functionality of the script like "query", "if" or "js".
The Command must have following annotations:
- @Command(name) which describes current class as Command
- @CommandParameters pointing to the method which will receive unparsed command parameters like: {command:this_is_command_code}
- @StreamSource pointing to the method which will return the Akka Source
- @StreamSourceFromChildren pointing to the method which will receive sources from commands nested inside this one.
This can be used to manipulate the context of sub commands.

## Examples / Tests
The tests are divided in three groups:
- com.mdanetzky.streamscript.integration contains full ETL from database with XSD checking, Exception routing and complex stream handling
- com.mdanetzky.streamscript.knowledge containing tests written while I was learning the tools
- com.mdanetzky.streamscript.parser with unit tests of the parser itself

If you want to jump right in, take a look at com.mdanetzky.streamscript.integration.XmlFileGeneratorTest.
There you will find here a few examples of using the XmlFileGenerator which takes a script, a context map and xsd and runs following scenario:
- read database
- convert to xml row by row
- push the chunks of xml into xsd validator
- output chunks via OutputStream
- throw all exceptions through OutputStream
