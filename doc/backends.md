Lackr Backends
==============

Lackr backend is an abstraction that hide the details of how queries sent by the proxy should be executed.

Getting started layout
----------------------

A trivial proxy like the one from the getting-started Demo is plugged on a simple "ClientBackend" that send
request to a given remote HTTP server (that we have also called "backend" so far, just to get things a bit more
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

a real life example
-------------------

But in real life, things can get slightly more interesting. This is the _real_ backend hierarchy that is in
use for the Fotopedia Web App and Web services.

```java
    new TryPassBackend(
        new LoggingBackend(
            new InProcessBackend( [ fast Scala stack servlet instance ] )
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

It's pseudo-code. Attributes of the various backends have been omitted. Let's have a look at what does what.

- TryPassBackend is a backend that is configured with an ordered list of backends. It will try them in turn until one
  of them handles the query.
- LoggingBackend is logically transparent but... logs stuff. It's currently not part of Lackr but of our
  application specific code (we describe a dummy logging backend in the next section).
- InProcessBackend wraps a standard Servlet (which is actually our fast-stack app) and performs queries against it
  without going through the network.
- HashRingBackend performs consistent hashing against a ring made of its children backends. The choice of backend
  is done by hashing the path and query parameters of the query in order to optimize the use of memory in our
  3-server Varnish cache cluster.
- ClientBackend is our old friend from getting-started. It wraps a jetty HTTP client to perform HTTP over the network
  against another server. It will performs queries against one given varnish, which will in turn forward them to our
  slow Ruby-on-Rails stack if necessary.

So basically, we give the *fast* and *in-process* stack a chance to deal with the request itself before falling back
to a cache cluster, which in turn falls back to the Ruby-on-Rails App. We could use a ClientBackend and have the fast
stack run elsewhere in its own JVM, but the use of InProcessBackend is an optimization to avoid a network round trip to
a remote HTTP server that in many cases would result to a 501 "please pass to the next" response.

Obviously this optimization can only work for backends that are implemented as Servlet. If the fast stack was in go,
for instance, we would have no choice but use the ClientBackend. Another constraint on the InProcessBackend is for the
wrapped Servlet to be strictly synchronous. Once again, if it was to use asynchronous Servlet processing (as Lackr
proxies do, by the way), we would have to go through a separate server and a ClientBackend.

Implementing a pass-through backend (LoggingBackend)
----------------------------------------------------

If LoggingBackend is not present in itself, it is interesting to know how it is actually implemented. Running code
before and after a request is made a bit tedious by the asynchronous nature of Lackr processing.

```
public class LoggingBackend extends BackendWrapper {

    public LoggingBackend(Backend wrapped) {
        super(wrapped);
    }

    @Override
    public LackrBackendExchange createExchange(final LackrBackendRequest request) {
        final long start = System.currentTimeMillis();
        final LackrBackendExchange innerExchange = getWrapped().createExchange(request);
        final CompletionListener innerCompletionListener = innerExchange.getCompletionListener();
        innerExchange.setCompletionListener(new CompletionListener() {

            @Override
            public void fail(Throwable t) {
                System.err.println(request + " took " + (System.currentTimeMillis() - start) + " millisecs but failed: " + t);
                innerCompletionListener.fail(t);
            }

            @Override
            public void complete() {
                System.err.println(request + " took " + (System.currentTimeMillis() - start) + " millisecs.");
                innerCompletionListener.complete();
            }
        });
        return innerExchange;
    }
}
```

Note:
- Executing code before the request is quite trivial. The problematic part is plugging into the completion listener
  chain to run after the request is done.
- BackendWrapper provides doStart() and doStop() implementations managing the wrapped Backend life cycle. They can be
  overridden for managing additional resources, but they *must* call their _super_ implementation.

Implementing a multi backend router
-----------------------------------

RoundRobinBackend is a backend that will hit every backend children in a round robin fashion, skipping the down
ones.

We are not using it in production but we feel pretty confident it does work. It is meant to be a starting point for
implementing alternative cluster targeting backends.
[Check out the code.](/src/main/java/com/fotonauts/lackr/backend/RoundRobinBackend.java)

It leverages the same Cluster/ClusterMember abstraction than the HashRingBackend, for background health checking
(aka probe() ) of a list of backends. Implementing a routing Backend is actually easier than a Logging one, as
the magic happens at the beginning of the processing.

A good starting point is to subclass jetty's AbstractLifeCycle to get a reasonable and robust LifeCycle
implementation. doStart() and doStop() *must* call stop() and start() on any inner backend (or inner cluster of
backends). probe() must return true if the backend is on a working state. Finally the all important "createExchange"
method can be forwarded to an inner backend according to any suitable logic.

More off-the-shelf backends may be added in the future...
