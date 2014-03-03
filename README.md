Lackr is a java jetty-based library for building high performance web sites.

Rationale and history
=====================

The need for Lackr emerged from the organic — but rationale — way Fotonauts stack has evolved over a few years. This is
the edifying tale of our long way towards modern architecture and good web performance.

Once upon a time, something like 2008, there was a Ruby on Rails application, a relational database, a rich client,
and demanding data and application specifications.
We were dealing mostly with hierarchical photo albums, each page containing an ordered photo collection,
children pages, and links of various types to other albums and pages of other albums.

Link density
------------

The typical web page (album, page, user, picture, ...) was containing up to hundreds of so-called *cards*, usually an
icon and some label representing the external resource link.
For instance, an album page about Paris was made of a slideshow of selected pictures and a set of links to other
resources represented by a card:
- other albums about Paris by other album authors
- pages in other albums
- other albums about other cities

The user interface, both rich client and web, was focusing heavily on link creation, in order to incitate users to
create a very dense network of albums and pictures.

First performance difficulties and page-level caching
-----------------------------------------------------

Even without a significant load on a sensible size server, it quickly became obvious that the MySQL/RoR stack was
not up to the task of generating pages of such a complexity with acceptable performance.
During these dark ages, the general feeling shared by both our team and the overall web development community could be
summarize by: "No matter how bad the backend performance is, the cache layer will save the day."

At that point, we introduced Varnish and start caching whole pages. Just for unlogged traffic at the beginning, the
ruby codebase being entirely permeated with pages where the content had to be tailored to the logged-in user:
- mandatory login box vs "logged in as" top left of the screen
- filtered lists of links according to various album-vs-user permissions

If the first could easily be worked out with an Ajax query, the second was way more tricky.

Even so, we started to worry about our users not understanding what was happening with the website: "I changed the
album title, still when I share it to my friend, they see the old name" and the like. Cache staleness was getting
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

One of the simplest usage of ESI with Varnish is to extract from a common page the infamous log-in/logged-as html
fragment, so one single cached page can be served to all users, the backend being hit only to fetch a login box
or a label in a very simple — so hopefully fast — query.

But we chose to use much more of it. By isolating each of our *cards* in its own backend endpoint, we hit several
birds:
- ability to cache the root "page" itself
- ability to invalidate a resource *cards* without invalidating the page
- an order of magnitude faster page composition.

After a few months on this regimen, we actually switched from Varnish ESI to nginx Server Side Include. We already
had a nginx layer on top of Varnish for SSL and zipping support, so it was merely a matter of changing syntax. The
purpose of this change was to take advantage of the ability of nginx to perform SSI expansion in parallel
whereas Varnish performed the sub-queries one after the other.

So at this point our stack was looking like that:

```
                            *************
                         ***             ***
                       **     Internet      **
                         ***             ***
                            *************
                                  |
                                  |
                                  v
                        +--------------------+
                        |       nginx        |
                        +--------------------+
                            | | | | | | |
                            | | | | | | |
                            v v v v v v v
                        +--------------------+
                        |      Varnish       |
                        +--------------------+
                               |   |
                               |   |
                               v   v
                        +--------------------+
                        |   Ruby on Rails    |
                        +--------------------+

```

For one single Internet query, nginx performed one "root" request, then several SSI fragment requests to
Varnish. Varnish only let a few simple and fast queries trickle down to the Ruby on Rails application. A
non-represented invalidation loop, triggered by write operations on the the Rails backend, takes care of PURGE-ing
impacted fragments from Varnish.

Introducing Lackr
-----------------

Even if this architecture allowed us to get serious performance improvements without redesigning the whole app and
database, we were still not fully satisfied with it. We had a serious feel of lack of control on what was going in
the nginx-Varnish interaction:
- ESI support for Ajax queries: as we were expanding the web application design to get it more interactive, the need
  arose for fragment expansion inside a json document. The same performance issues we had in the backend were starting
  to show.
- better control over caching: we needed a robust and efficient way of using Varnish servers RAM. We wanted a
  consistent hash ring.
- error management: when something wrong was happening deep inside the Ruby on Rails, the page being composed in an
  unbuffered way lead to really ugly error pages

We also wanted to be able to solve efficiently, without jumping though dozens of hoops, some of the key issues in the
application. We were thinking about things like generating signed urls for our images, verifying sessions tokens, ...
As writing application code in Varnish or nginx, both in C with complex concurrency approaches, was not appealing to
us, we started thinking about a more developper friendly java layer somewhere in between the internet and Varnish.

And, ho yeah. Also. We, at Fotonauts, do love developping stuff. Much more fun than integrating.

Lackr first baby steps
----------------------

So, on a cold and rainy november weekend, Lackr was born and baptized by the name being a far-fetched pun on
Varnish name.

The core was basically a buffered extensible ESI-like engine. The focus was on performance and scalability, so
everything was written to run in asynchronous fashion.
The ESI engine, called "Interpolr", was able to detect include-like patterns in HTML, XML, JS and JSON documents,
grab them from the backend, and re-encode them on the fly to accomodate the layers of escaping required by the
originating document. For instance a JS chunk from an HTML document may query an HTML fragment, in which case
double quotes, line feeds, but also less-than signs had to be dealt with, in order not to break the main document
syntax.

The support for consistent backend hashing was to come soon after.

Our stack became:

```
                            *************
                         ***             ***
                       **     Internet      **
                         ***             ***
                            *************
                                  |
                                  |
                                  v
                        +--------------------+
                        |       nginx        |
                        +--------------------+
                                  |
                                  |
                                  v
                        +--------------------+
                        |       Lackr        |
                        +--------------------+
                            | | | | | | |
                            | | | | | | |
                            v v v v v v v
                        +--------------------+
                        |      Varnish       |
                        +--------------------+
                               |   |
                               |   |
                               v   v
                        +--------------------+
                        |   Ruby on Rails    |
                        +--------------------+

```

Not a huge change in terms of architecture. nginx was kept as an efficient way to direct queries to
non-represented static resources, to implement a few redirects and to protect against various internet
hazards.

More application needs
----------------------

Now that we felt again in control of the top levels of our stack, we could move on to solve various issues
that had been bugging us for months. Both old and new application requirements were calling for
uncacheable, application-level, request-time processing:
- vote information: when a user was shown an album, the webpage needed to know whether the user had already
  cast a vote on the current resource
- ad targetting: depending on the geolocalisation, language, device kind and another dozen other parameters,
  one ad or another had to be shown in a given placeholder
- ad tracking: a given ad insertion had a unique id for performance evaluation
- customized views: new mosaic views had to be tailored to the device size of the user, while enforcing a
  consistent pagination on updatable collections

If having a Java server was a great improvement to application flexibility compared to nginx or Varnish code,
plugging extension in Lackr was still a bit awkward. Its asynchronous nature made the code trickier to write
than necessary, while Java felt like assembly with our Ruby bad habits.

But being in control of the stack allowed us to introduced another language and framework in the mix. We picked
Scala and Unfiltered to implement a "fast stack" alongside the Varnish/Rails one. That way we could pick a few
expensive rails endpoints and move them to the new stack to improve the general performance.

The new stack was to have its own server process, but to live in the same git repository as the Rails
app, to simplify deployments with cross-dependencies between the Rails and Scala apps.

```
                              *************
                           ***             ***
                         **     Internet      **
                           ***             ***
                              *************
                                    |
                                    |
                                    v
                          +--------------------+
                          |       nginx        |
                          +--------------------+
                                    |
                                    |
                                    v
                      +---------------------------+
                      |           Lackr           |
                      +---------------------------+
                      / / / / / / /      \
                     / / / / / / /         \
                    v v v v v v v           v
            +--------------------+       +--------------------+
            |      Varnish       |       |    Scala stack     |
            +--------------------+       +--------------------+
                   |   |
                   |   |
                   v   v
            +--------------------+
            |   Ruby on Rails    |
            +--------------------+

```

We experimented with several ways of implementing the actual dispatching between the Varnish/Rails and
the Scala stacks. After a few months, the best solution we found was to actually run the Scala app in the same
JVM as Lackr, bypassing the whole HTTP network interaction, at the price of some tricky black magic.

The Scala stack is called first and offered a chance to process every query Lackr is emitting, and Lackr will then
try to call Varnish when the Scala app does not show interest.

Both stack, the Scala and the ruby one, were accessing the same MongoDB databases where most stuff had been migrated
from MySQL. To date, we are quite happy with the combination of sluggish but very developper friendly Rails stack,
still the workhouse of our app, containing all views templates and, on the other side, the more webservice-oriented 
Scala stack producing fresh JSON data to the empty web views, or XML data to our rich-client iOS applications.

Lackr get handlebars
--------------------

On the application side, we found out that use of Handlebars (both .js and .rb implementations) was helping a lot
with content vs format separation. In order to provide static HTML views for Javascript-less browser
(including crawlers)
as well as improving the rendering performance of rich web page, we decided to add handlebars support to Lackr too.

It worked very well, so well we also wanted handlebars support in our iOS applications. The price of it was that more
and more really application level stuff (handlebars handlers for instance) was migrating to the Lackr codebase making
occasionally deployments a bit difficult to manage, by binding Lackr versions to the app versions.

Time for a refactoring
----------------------

And there was still the issue of the ugly "unfiltered stack loaded in Lackr" hack. Definitely a good time to rethink
a few things on the black board.

We decided to switch roles: the unfiltered stack would be the container, and Lackr was to become a library.
Jetty, heavily used by both components, provided most structural interfaces to build upon.

Swapping roles would also solve most of the boring configuration issues: the Scala app becoming the container, it had
all the necessary knowledge to setup Lackr components the right way, without duplicating information in
pseudo-generic configuration files all over the place.

Also, that way Lackr becomes something other people might have interest in instead of a ugly kludge at the core of 
our web stack...
