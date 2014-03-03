Lackr is a java jetty-based library for building high performance web sites.

Rationale and history
=====================

The need for Lackr emerged from the organic — but rationale — way Fotonauts stack has evolved over a few years. This is
the edifying tale of our long way towards modern architecture and good web performance.

Once uppon a time — something like 2008 — there was a Ruby on Rails application, a relational database and
demanding data and application specification.
We were dealing mostly with hierarchical photo albums, each page containing an ordered photo collection,
children pages, and links of various types to other albums and pages of other albums.

Link density
------------

The typical web page (album, page, user, picture, ...) was containing up to hundreds of so-called "cards", usually an
icon and some label representing the external resource link.
For instance, an album page about Paris was made of a slideshow of selected pictures and a set of links to other
resources represented by a card:
- other albums about Paris by other album authors
- pages in other albums
- other albums about other cities

The user interface — both rich client and web — was focusing heavily on link creation, in order to incitate users to
create a very dense network of albums and pictures.


