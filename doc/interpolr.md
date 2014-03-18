Interpolr
=========

Internal Architecture
---------------------

Interpolr is where the response bodies are scanned and processed. It is quite modular in its design, allowing
generic features (like ESIs) or very application oriented features.

Interpolr has been designed to be testable without HTTP, so we have introduced a few classes and interfaces
abstracting away the HTTP and Servlet stuff.

InterpolrProxy and a few others classes from its package are the link between the Interpolr world and the
"Proxy" world:

- Interpolr is meant to be a singleton in your application, holding, as a list of Plugins the various processor
  that will work over the documents
- InterpolrContext is an interface covering the context of processing of a page. It is implemented by
  InterpolrFrontendRequest in the context of proxy: it contains all the information associated with the execution
  of one incoming request on lackr. One will be created for every incoming request.
  For tests, we use a InterpolrContextStub implementation.
- In a similar fashion, InterpolrScope abstracts the Backend requests. It is implemented by InterpolrBackendRequest
  for use in real life, or InterpolrScopeStub in tests.
- Special attention has been given to sensible optimization of the text and string processing: we have chose to keep
  response bodies in byte array form as much as possible, and make efforts to avoid copying the same text over and
  over. The Chunk interface generalize what we need to be able to do with these byte[]-like. In its simple form, it is 
  implemented as a ConstantChunk (an actual byte[]), but it can go as far as representing a Future-like response to
  an ESI request, (see RequestChunk) or the result of Handlebars evaluation. Document is a list of Chunk to be 
  concatenated.
- Lackr generates much of its entropy scanning byte[]s to find its exotic — but constant — markup. In order to save
  a few white bears, and some milliseconds of performance, we use an efficient Boyer-Moore scanner to find these
  markup occurencies. As we are using byte[] and not String anyway, Regexp would not have cut it.
- various features can be plugged in and plugged out of Interpolr by the use of Plugin and Rules abstraction.

Plugins and Rules
-----------------

A Plugin is analogous to a feature. For instance ESI support is a plugin, and so is Handlebars.

One of the most important aspect of a Plugin is a listt of Rules. Interpolr will run all rules from all
its registered plugins against a DataChunk (which is basically a slice of a byte[]). Each rule will returns
a list of Chunk:
- a rule that does not find anything to do will simply return a list with its input as single item
- if a rule find something to act on, it must return a list composed with DataChunks for each "untouched" part
  of the document and Chunks of any kind for the altered parts
- in the process, the rule can optionally communicate with its Plugin, and a "PluginContext" a plugin-specific
  instance that holds at the context level (i.e. incoming request level) variables for the Plugin.
  For instance, this will allow a Rule to register a Handlebars template at the page level (or to query this registry).

Implementing a Plugin
---------------------

A good starting point is the SingleRulePlugin with a SimpleSubstitutionRule. See for instance 
[TestSubstitution](/src/test/java/com/fotonauts/lackr/TestSimpleSubstitution.java).

```java
    Interpolr inter = new Interpolr();
    inter.setPlugins(new Plugin[] { new SingleRulePlugin(new SimpleSubstitutionRule("titi", "toto")) });
    Document r = InterpolrTestUtils.parse(inter, "tititata");
    assertEquals("tototata", InterpolrTestUtils.expand(r));
```

It creates a simple interpolr which will replace every occurence of "titi" by "toto". InterpolrTestUtils make
it easier to tranform chunk test data to and from String.

### [SingleRulePlugin](/src/main/java/com/fotonauts/lackr/interpolr/plugins/SingleRulePlugin.java)

It demounstrates the basic responsibilities of a trivial Plugin:
- getRules() returns the rules
- createContext() creates the Plugin-specific context instance (when needed)
- preflightCheck() is called just before page rendering. It's a good place to check that everything that should
  be there is there indeed (for instance the Handlebars plugin will compile all templates and check that all "eval"
  tags have their template defined.

[SimpleSubstitutionRule](/src/main/java/com/fotonauts/lackr/interpolr/plugins/SimpleSubstitutionRule.java) builds
upon an abstract class:

### [SimpleTriggerRule](/src/main/java/com/fotonauts/lackr/interpolr/plugins/SimpleTriggerRule.java) 

It's an abstract class that is meant to be subclassed by all rules that will be triggered by a "constant" markup
(aka Trigger). All of the rules we have implemented so far are actually subclasses of SimpleTriggerRule.

The onFound method() will be called with the position where the trigger was found, and the current chunk list that
is being built by the rule. The onFound implementation must push the "result" chunk or chunks in the result list,
then returns the number of bytes to be skipped by SimpleTriggerRule before starting to scan again.

With these simple convention, SimpleTriggerRule will take care of dealing with the untouched chunks to create
before and after the matches.

### [SimpleSubstitutionRule](/src/main/java/com/fotonauts/lackr/interpolr/plugins/SimpleSubstitutionRule.java)

The actual implementation of onFound() adds the replacement ConstantChunk to the result buffer, then returns
the length of the trigger.

### [MarkupDetectingRule](/src/main/java/com/fotonauts/lackr/interpolr/plugins/MarkupDetectingRule.java)

But most of the exotic markup Lackr takes a form that is rarely a pure constant. We often need to capture something
that looks like a xml attribute. For instance the "main" HTML ESI markup has one attribute to capture:

```
<!--# include virtual="/path-to-esi.html" -->
```

And Handlebars plugin markup is even more complex with one attribute and the body to capture.

```
<!-- lackr:handlebars:template name="template_name" -->
    some text from the template name:{{name}} value:{{value}}
<!-- /lackr:handlebars:template -->
```

To help implementing these complex markups rules, we can subclass MarkupDetectingRule. The following is inspired from
the real TemplateRule:

```
public class TemplateRule extends MarkupDetectingRule {
    private Plugin plugin;

    protected static ConstantChunk EMPTY_CHUNK = new ConstantChunk("".getBytes());

    public TemplateRule(HandlebarsPlugin plugin) {
        this.plugin = plugin;
        super("<!-- lackr:handlebars:template name=\"*\" -->*<!-- /lackr:handlebars:template -->");
    }

    public Chunk substitute(byte[] buffer, int start, int[] boundPairs, int stop, InterpolrScope scope) {
        String name = new String(buffer, boundPairs[0], boundPairs[1] - boundPairs[0], "UTF-8");
        Document template = scope.getInterpolr().parse(buffer, boundPairs[2], boundPairs[3], scope);
        ((HandlebarsContext) scope.getInterpolrContext().getPluginData(plugin)).registerTemplate(name, template);
        return EMPTY_CHUNK;
    }
```

- TemplateRule extends MarkupDetectingRule
- MarkupDetectingRule constructor is called with the text of the markup we will match, with capture groups marked by a
  stars.
- When a markup zone is detected, substitute is called with "boundPairs" which allows to extract from the buffer the
  capture groups.

This code also shows 
- how to keep a reference on the actual Plugin, that is used in substitute() to get a reference of
the page handlebars context.
- a way to "eat" the content of the markuped element (by substituting an empty chunk)
