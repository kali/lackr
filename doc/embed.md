Embed Lackr
===========

Lackr is meant to be embeded. That is, you'll need to write some kind of java code to run it (or scala, or other
JVM-targetted language).

Why is that ? Well, our idea is that you probably don't want to use it if you don't have serious reason to. The
marginal benefits over Varnish, nginx or apache feature set would not justify it. You will use Lackr if you want to,
at least extends Handlebars with application specific handlers, or do some sophisticated and specific backend
management.

Covering a significant part of the extension point of Lackr through configuration would require such a huge amout of
code and debug that we prefer to stick, for now at least to stick to a good old library.

Disagree ? Well, talk to us.

Architecture
------------

Lackr main entry point a _proxy_. Lackr contains a basic implementation called _BaseProxy_ which is mostly meant to be
use is unit and integration tests. The usefull one is its subclass _InterpolrProxy_, which embeds an instance of
Interpolr.

BaseProxy, as well as InterpolrProxy delegates all Http queries to one or several backends server through the
composable Backend abstraction. Composing Backend allows build complex scenarios like ring based sharding,
try-and-pass request handling, abstracting finally in process backends as well as real http backends.

Interpolr is where all the magic will happen. ESI detection, handlebars expansion, and smart json management.

A typical setup, like the one discussed in getting-started will look like this:

```
           Incoming requests world                Lackr Interpolr world                Lackr Backend world

        +----------------------------+                                       
 ------>|       Jetty Server         |                                       
        +----------------------------+                                       
                      |
                      v
        +----------------------------+                         
        |   LackrProxyJettyHandler   |                         
        +----------------------------+                         
                      |
                      v
        +----------------------------+        +----------------------------+ 
        |       InterpolrProxy       |------->|         Interpolr          | 
        +----------------------------+        +----------------------------+ 
                      |                              |              |
                      |                              v              v
                      |                       +------------+  +------------+
                      |                       |    ESI     |  | Handlebars |
                      |                       +------------+  +------------+
                      |
                      |
                      |                                                           +----------------------------+
                      \---------------------------------------------------------->|        ClientBackend       |-----> ${BACKEND}
                                                                                  +----------------------------+
```



Embed as a standalone jetty server
----------------------------------

Jetty is actually a very nice HTTP implementation for developpers to play with. Creating a server and embeding lack
in it is a matter of only a few java lines.

Let's use maven to generate an empty java project

```
% mvn archetype:generate -DgroupId=some.groupid -DartifactId=lackr-demo-server -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
```

We need to add fotonauts repository (FIXME) and, of course, a dependency on lackr.

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
      <version>0.5.36</version>
    </dependency>
[...]
```

