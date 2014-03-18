Lackr is Fotonauts high speed front-side HTTP proxy server.

In short
========

It was meant to be a way to workaround the slugginess of our Rails backend.

In our web stack, it complements Varnish, Haproxy and nginx features and allows
us to give a high degree of sophistication and control upon what our front stack
does.

Its main features are:
- small overhead when used as a proxy, scalable,
- extended (and extensible) ESI support,
- Handlebars evaluation support.

It is distributed as a library, and principally meant to be integrated in a
jetty 9.x server.
A very simple proxy with ESI and Handlebars is provided in
[Demo.java](/src/main/java/com/fotonauts/lackr/Demo.java) to help getting started. It
can be tweaked to become a standalone server, but Lackr will be most beneficial when
integrated to meet your own features need. Lackr is a tool for developpers.

In our case, our server integrated a top level Lackr making hybrid pages from both
an in-process unfiltered/Scala stack, and a varnish/Rails fragment cache cluster.
Integrating the library with application code allows to "hook" at several places in
request processing with minimum headache:
- on the top-level jetty handler: centralized user-agent detection (phone vs rest),
  request and error logging, etc
- on backend request processing: normalize headers for Varnish cache, manipulate query,
  backend sharding and balancing
- Handlebars ad-hoc "handlers" for consistent HTML creation in Backend, browser and
  Lackr.

In less short
=============

It's meant to build reverse proxies, something you will setup on your server stack,
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

[Handlebars](http://handlebarsjs.com/) is a lightweight template engine.

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

These two ideas (and some work) provide ways to mix together fresh data from a http source and
presentation from another...

Interested ? [Try it](/doc/getting-started.md), or [read more](/doc/README.md).

Credits
=======

From Fotonauts:

- Mathieu Poumeyrol (http://github.com/kali)

Copyright (c) 2011-2014 Fotonauts released under the ASF license.
