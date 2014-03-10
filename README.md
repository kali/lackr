Lackr is Fotonauts high speed front-side HTTP proxy server.

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
[/src/main/java/com/fotonauts/lackr/Demo.java](Demo.java) to help getting started. It
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

Interested ? [/doc/getting-started.md](Try it,) or [/doc/history.md](read more).

