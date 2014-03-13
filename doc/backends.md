Lackr Backends
==============

Lackr backend is an abstraction that hide the details of how queries sent by the proxy should be executed.

Getting started layout
----------------------

A trivial proxy like the one from the getting-started Demo is plugged on a simple "ClientBackend" that send
request to a given remote http server (that we have also called "backend" so far, just to get things a bit more
confusing).

```
                       +----------------------------+                                                                         
                       |                            |                                                                         
   Incoming            |  +----------------------+  |                                                                         
   Requests    --------+->|       BaseProxy      |  |                                                                         
                       |  +----------------------+  |                                                                         
                       |              |             |                                                                         
                       |              v             |                                                                         
                       |  +----------------------+  |                                                                         
                       |  |     ClientBackend    |--+------> Actual HTTP server Backend                                    
                       |  +----------------------+  |                                                                         
                       |                            |                                                                         
                       |         Lackr Demo         |                                                                         
                       +----------------------------+                                                                         
```

Real-life example
-----------------

But in real life, things can get slightly more interesting. This is the _real_ backend hierarchy that is in
use for the Fotopedia Web app and Web services.

```java
    new TryPassBackend(
        new LoggingBackend(
            new InProcessBackend( [ fast scala stack servlet instance ] )
        ),
        new LoggingBackend(
            new HashRingBackend(
                new LoggingBackend(new ClientBackend()), // varnish server 1
                new LoggingBackend(new ClientBackend()), // varnish server 2
                new LoggingBackend(new ClientBackend())  // varnish server 3
            )
        )
    )
```

It's pseudo-code. Attributes of the various backends have been ommited. Let's have a look at what does what.

- TryPassBackend is a backend that is configured with an orderd list of backends. It will try them in turn until one
  of them handles the query.
- LoggingBackend is logically transparent but... logs stuff. It's currently not part of Lackr but of our
  application specific code. We may move it to Lackr someday, but it needs some work.
- InProcessBackend wraps a standard Servlet (which is actually our fast-stack app) and performs queries against it
  without going through the network.
- HashRingBackend performs consistent hashing against a ring made of its children backends. The choice of backend
  is done by hashing the path and query paramters of the query in order to optimise the use of memory in our
  3-server Varnish cache cluster.
- ClientBackend is our old friend from getting-started. It wraps a jetty http client to perform HTTP over the network
  against another server. It will performs queries against one given varnish, which will in turn forward them to our
  slow Ruby-on-Rails stack if necessary.


