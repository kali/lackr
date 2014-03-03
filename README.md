Lackr is a java jetty-based library for building high performance web sites.

Rationale and history
=====================

The need for Lackr emerged from the organic — but rationale — way Fotonauts stack has evolved over a few years. This is
the edifying tale of our long way towards modern architecture and good web performance.

Once uppon a time, something like 2008, there was a Ruby on Rails application, a relational database and
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

The user interface, both rich client and web, was focusing heavily on link creation, in order to incitate users to
create a very dense network of albums and pictures.

First performance difficulties and page-level caching -----------------------------------------------------

Even without a significant load on a sensible size server, it quickly became obvious that the MySQL/RoR stack was
not up to the task of generating pages of such a complexity with acceptable performance.
During these dark ages, the general feeling shared by both our team and the overall web development community could be
summarize by: "No matter how bad the backend performance is, the cache layer will save the day."

At that point, we introduced varnish and start caching whole pages. Just for unlogged traffic at the beginning, the
ruby codebase being entirely permeated with pages where the content had to be tailored to the logged-in user:
- mandatory login box vs "logged in as" top left of the screen
- filtered lists of links according to various album-vs-user permissions

If the first could easily be worked out with an ajax query, the second was way more tricky.

Even so, we started to worry about our users not understanding what was happening with the website: "I changed the
album title, still when I share it to my friend, they see the old name" and the like. Cache stale-ness was getting
seriously in the way of the editing features. On the other hand, due to a "long-tail" shaped read traffic,
invalidating all pages including a "card" whenever it was altered was impacting our hit ratio enough to make our
caching layer next to useless.

It was time for a bigger hammer.

Edge Side Include
-----------------

As an alternative to full page caching, we briefly considered html fragment caching inside the Rails application
itself. Each card, for instance, could have been cached as a separate html fragment in memcache, replacing
each time expensive database queries by a single memcache request. We had to discard this approach as it was
not allowing us to cache the top content of the page (without expanding the card) thus solving only half our
problem.

We preferred to leverage a nifty feature that Varnish was including: edge-side include support. Edge Side Include
main feature is the availibility of a cached delivered page to contain placeholder in the HTML text to be resolved
at service time.

One of the simplest usage of ESI with varnish is to extract from a common page the infamous log-in/logged-as html
fragment, so one single cached page can be served to all users, the backend being hit only to fetch a login box
or a label in a very simple — so hopefully fast — query.

But we chose to use much more of it. By isolating each of our "cards" in its own backend endpoint, we hit several
birds:
- ability to cache the "page"
- ability to invalidate a resource cards without invalidating the page
- an order of magnitude faster page composition

After a few monthes on this regimen, we actually switched from Varnish ESI to nginx Server Side Include. We already
had a nginx layer on top of Varnish for SSL and zipping support, so it was merely a matter of changing syntax. The
purpose of this change was the fact nginx SSI expansion is performed in parallel if there are more than one
fragment inclusion in the page, whereas Varnish performs the sub-queries one after the other.

At this point our stack was

Introducing Lackr
-----------------


