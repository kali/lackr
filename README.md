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

So at this point our stack is looking like that:

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
                        |       Nginx        | 
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

For one single Internet query, Nginx performs one "root" request, then several SSI fragment request to
Varnish. Varnish only lets a few simple and fast queries trickle down to the Ruby On Rails application. A
non-represented invalidation loop, triggered by write operations on the the Rails backend, takes care of PURGE-ing
impacted fragments from Varnish.

Introducing Lackr
-----------------

Even if this architecture allowed us to get serious performance improvements without redesigning the whole app and
database, we were still not fully satisfied with it. We had a serious feel of lack of control on what was going in
the Nginx-Varnish interaction:
- esi support for ajax queries: as we were expanding the web application design to get it more interactive, the need
  arose for fragment expansion inside a json document: the same performance issues we had in the backend were starting
  to show
- better control over caching: we need a robust and efficient way of using Varnish servers RAM. We wanted a consistent
  hash ring.
- error management: when something wrong is happening deep inside the Ruby on Rails, the page being composed in an
  unbuffered way lead to really ugly error pages

We also wanted to be able to solve efficiently, without jumping though dozen of loops, some of the key issues in the
application. We were thinking about things like generating signed urls for our images, verifying sessions tokens, ...
As writting application code in varnish or nginx, both in C with complex concurrency approaches, was not appealing to
us, we started thinking about a more developper friendly java layer somewhere in between the internet and varnish.

And, ho yeah. Also. We, at Fotonauts, do love developping stuff. Much more fun than integrating.

Lackr first baby steps
----------------------

So, on a cold and rainy november weekend, lackr was born and baptized by the name being a far-fetched pun on
Varnish name.

The core was basically a buffered extensible ESI-like engine. The focus was on performance and scalability, so
everything was written to run in asynchronous fashion.
The ESI engine, called "Interpolr", was able to detect include-like patterns in HTML, XML, JS and JSON documents,
grab them from the backend, and re-encode them on the fly to accomodate the layers of escaping required by the
originating document. For instance a JS chunk from an HTML document may require an HTML fragment, in which case
double quotes, line feeds, but also less than sign have to be dealt with not to break the main document syntax.

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
                        |       Nginx        | 
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

Not a huge change in terms of architecture. Nginx was kept as an efficient way to directs queries to
non-represented static resources, implementing a few redirects and protecting against various internet
hazards.


