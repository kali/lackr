Embed Lackr
===========

Lackr is meant to be embedded. That is, you'll need to write some kind of Java code to run it (or Scala, or other
JVM-targeted language).

Embed as a standalone jetty server
----------------------------------

Jetty is actually a very nice HTTP implementation for developers to play with. Creating a server and embedding lack
in it is a matter of only a few Java lines.

Let's use maven to generate an empty Java project

```
% mvn archetype:generate -DgroupId=some.groupid -DartifactId=lackr-demo-server \
    -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

We need to add fotonauts repository (FIXME) and, of course, a dependency on Lackr.

```xml
[...]
  <repositories>
    <repository>
      <id>fotonauts</id>
      <url>http://maven-fotonauts.s3.amazonaws.com/release/</url>
    </repository>
  </repositories>
[...]
    <dependency>
      <groupId>com.fotonauts</groupId>
      <artifactId>lackr</artifactId>
      <version>0.5.37</version>
    </dependency>
[...]
```

We have implemented a few factory helpers to create Lackr running, so setting it up 
is only a matter of a few lines of code. Let's edit the App.java main() method...

```java
import com.fotonauts.lackr.Backend;
import com.fotonauts.lackr.interpolr.Interpolr;
import com.fotonauts.lackr.testutils.Factory;

public static class App {

    public static void main(String[] args) throws Exception {
        Backend backend = Factory.buildFullClientBackend("http://localhost/~kali/lackr-examples/", null);
        Interpolr interpolr = Factory.buildInterpolr("handlebars esi");
        Server jettyServer = Factory.buildInterpolrProxyServer(interpolr, backend, 8000);

        jettyServer.start();
        jettyServer.join();
    }

}
```

Note that this is exactly what Demo.java does, when backend and port guesswork is stripped out.

If you want to run this code, you'll probably want to alter the backend location and the jettyServer port.

What does this do ?
-------------------

```
           Incoming requests world                Lackr Interpolr world                Lackr Backend world

        +----------------------------+
 ------>|       Jetty Server         |
        +----------------------------+
                      |
                      v
        +----------------------------+        +----------------------------+        +----------------------------+
        |   LackrProxyJettyHandler   | ------>|       InterpolrProxy       |------->|        ClientBackend       |
        +----------------------------+        +----------------------------+        +----------------------------+
                                                            |                                      |
                                                            v                                      \---> ${BACKEND}
                                              +----------------------------+
                                              |         Interpolr          |
                                              +----------------------------+
                                                     |              |
                                                     v              v
                                              +------------+  +------------+
                                              |     ESI    |  | Handlebars |
                                              +------------+  +------------+

```

Lackr main entry point is a _proxy_. Lackr contains a basic implementation called _BaseProxy_ which is mostly meant 
to be use in unit and integration tests. The useful one is its subclass _InterpolrProxy_, which embeds an instance of
Interpolr.

BaseProxy, as well as InterpolrProxy delegates all HTTP queries to one or several backends server through the
composable Backend abstraction. Composing Backend allows to build complex scenarios like hashring based sharding,
try-and-pass request handling. The final provider of content will be in-process backends or real HTTP backends.

Interpolr is where all the magic will happen. ESI detection, handlebars expansion, and smart JSON management.

- Jetty Server is a "standard" instance of a Jetty Server (coming from jetty-server.jar).
- InterpolrProxy is our main workhorse.
- LackrProxyJettyHandler is a thin wrapper to make InterpolrProxy pluggable into a Jetty server (implements Handler)
- ClientBackend is the backend to which InterpolrProxy will forward all incoming requests and all subsequent
  ESI-triggered requests too
- Interpolr is the text-processor that will detect and process ESI and handlebars markup, collaborating with the proxy
  to generate more backend requests.

Only the left column is deeply jetty-server tainted. [Embedding as a Servlet](servlet.md) will only alter the left
column.


