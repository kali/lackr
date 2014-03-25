Lackr is Fotonauts high speed front-side HTTP proxy server.

ESI on Steroids
========

ESI, or Edge-Side Include, allows to mix, at request time, markup text and/or data
from one or more backends HTTP server. This is a way to build hybrid stacks and
support real-life scenarios ranging from
- stack optimisation "Let's get this bit of the page faster by moving its computation
  from a slow backend technology to a faster one, and leave the rest of the page alone"
- progressive migration "Let's migrate the top bar to the new stack, and keep
  the rest where it is for now"
- differentiated caching "Let's cache the whole page without the top level corner where
  we will have either a small login form or user info"
- etc.

Its main features are:
- extended (and extensible) ESI support,
- Handlebars evaluation support,
- being fast and scalable.

It is distributed as a library, and principally meant to be integrated in a
jetty 9.0.x server.

Introduction
=============

Lackr is meant to build reverse proxies, something you will setup on your server stack,
between the internet and one ore more "backends" servers.

                          Internet
                             |
                             v
                   +--------------------+
                   | Lackr-based proxy  |
                   +--------------------+
                        /         \
                       /           \
                      v             v
       +--------------------+ +--------------------+
       |      Backend 1     | |      Backend 2     |
       +--------------------+ +--------------------+

ESI
---

If a backend emits something like that:

```
I'm some text at the top of the main document before an ESI call.
<!--# include virtual="/ex2-shared-esi.html" -->
I'm some text at the bottom of the main document after an ESI call.
```

Lackr will send one more query to the backend(s) to resolve the placeholder and
substitute the content.

Handlebars
----------

[Handlebars](http://handlebarsjs.com/) is a lightweight template engine available in many languages ([Java](https://github.com/jknack/handlebars.java), [Javascript](http://handlebarsjs.com/), [Ruby](https://github.com/cowboyd/handlebars.rb)). We've also developed an [Objective-C](https://github.com/fotonauts/handlebars-objc) for Mac and iOS applications. 

So if the backend sends something like that:

```
<!-- lackr:handlebars:template name="template_name" -->
    some text from the template name:{{name}} value:{{value}}
<!-- /lackr:handlebars:template -->
<!-- lackr:handlebars:eval name="template_name" -->
    { "name": "the name", "value": "the value" }
<!-- /lackr:handlebars:eval -->
```

Lackr will compile the template (in the first pair of lackr tags), and evalutates
it against the JSON data in the "eval" tag:

```
    some text from the template name:the name value:the value
```

In details
==========

- [Getting Started](doc/getting-started.md) will demonstrate how to start a dummy Lackr server with ESI and Handlebars
  support
- [History](doc/history.md) explains why we did all this, and maybe why you could be interested.

In excruciating details
=======================

- [Embed](doc/embed.md) provides hints about Lackr internal architecture, and how to embed Lackr
- [Backends](doc/backends.md) example use cases of the composable Backends
- [Interpolr](doc/interpolr.md) how Interpolr can be extended
- [Handlebars support](doc/handlebars.md) Plugin handlebars "helpers"
- [Servlet](doc/servlet.md) embed as a Servlet


Credits
=======

From Fotonauts:

- Mathieu Poumeyrol (http://github.com/kali)

Copyright (c) 2011-2014 Fotonauts released under the ASF license.
