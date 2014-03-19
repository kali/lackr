Handlebars
==========

Handlebars is itself very extensible. We have done our best to keep its extension point as easy to use as they were.

Registering Handlebars helpers
------------------------------

Let's assume we have a [Handlebars Java helper](https://github.com/jknack/handlebars.java#registering-helpers) ready to
be integrated. Our example is called humanize_integer. It formats big integers with a "M" or "k" suffix.

```java
public class MiscealaneousHelpers {

    public static CharSequence humanize_integer(Object numberAsObject, Options options) {
        if (numberAsObject == null)
            return "";
        if (numberAsObject instanceof Number) {
            long n = ((Number) numberAsObject).longValue();
            if (n >= 10_000_000) {
                return (n / 1_000_000) + "M";
            } else if (n >= 10_000) {
                return (n / 1_000) + "k";
            }
        }
        return numberAsObject.toString();
    }

}
```


By subclassing the HandlebarsPlugin, it is possible to access the Handlebars instance and register helpers
upon creation of the context. Registering the helper in Handlebars instance is as simple as calling
registerHelpers()...

```java
    Interpolr inter = new Interpolr();

    HandlebarsPlugin customizedPlugin = new HandlebarsPlugin() {

        public HandlebarsContext createContext(InterpolrContext interpolrContext) {
            HandlebarsContext ctx = super.createContext(interpolrContext);
            ctx.getHandlebars().registerHelpers(MiscealaneousHelpers.class);
            return ctx;
        }

    };

    inter.setPlugins(new Plugin[] { customizedPlugin });
```

This fragment of code defines a Interpolr with a customized HandlebarsPlugin only (no ESI). We override the
createContext() method, which is called one by every page that contains some Handlebars markup.

If the backend returns 

```
!-- lackr:handlebars:template name="template_name" -->
    some text from the template name:{{name}} value:{{value}} number: {{humanize_integer number}}
<!-- /lackr:handlebars:template -->
<!-- lackr:handlebars:eval name="template_name" -->
    { "name": "the name", "value": "the value", "number": 42012 }
<!-- /lackr:handlebars:eval -->
```

the proxy will substitute with: 

```
    some text from the template name:the name value:the value number: 42k
```
