Handlebars
==========

Handlebars is itself very extensible. We have done our best to keep its extension point as easy to use as they were.

Registering Handlebars helpers
------------------------------

By subclassing the HandlebarsPlugin, it is possible to access the Handlerbars instance and register helpers
upon creation of the context:

```java
    Interpolr inter = new Interpolr();

    HandlebarsPlugin customizedPlugin = new HandlebarsPlugin() {

        public HandlebarsContext createContext(InterpolrContext interpolrContext) {
            HandlebarsContext ctx = super.createContext(interpolrContext);
            ctx.getHandlebars().registerHelpers(MyApplicationHelpers.class);
            return ctx;
        }

    }

    inter.setPlugins(new Plugin[] { customizedPlugin });
```
