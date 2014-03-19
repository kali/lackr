Embed Lackr as a servlet
========================

Our preferred way to run lackr is a standalone jetty process. Nevertheless, we will try to give a rough
idea of how to run it in a servlet environment. This should be easy to transpose to other containers.

Note that the current Lackr version can only run in a 3.0 servlet container. We will support 3.1 in the 
near future, but older 2.x versions will not be supported because they lack the standard async support.

In terms of jetty versions, this constraint maps to the 9.0 branch. We'll use the jetty runner to check
that everything is running.

Creating the maven web project
------------------------------

```
    mvn archetype:generate \
        -DarchetypeGroupId=org.apache.maven.archetypes \
        -DarchetypeArtifactId=maven-archetype-webapp \
        -DarchetypeVersion=1.0
```

Maven will prompt for groupId and artifactId of your demo. I used: `lackr-servlet-demo` as artifactId, you
may want to use the same (it's the name of the directory where the project is created). The rest does not 
really matter.

Declare maven dependency
------------------------

In pom.xml, nothing exotic, just a repository location (FIXME) and a dependency.

```xml
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
```

Implement a servlet wrapper around an InterpolrProxy
----------------------------------------------------

This is a barebone servlet. Note that I have hardcoded the backend location,
borowing from [Getting Started](getting-started.md). You will certainly need to adjust this.

```java
import java.io.*;
import javax.servlet.*;
import javax.servlet.http.*;

import com.fotonauts.lackr.*;
import com.fotonauts.lackr.interpolr.*;
import com.fotonauts.lackr.testutils.Factory;

public class LackrServlet extends HttpServlet {

    private BaseProxy proxy;

    public void init() throws ServletException {
        try {
            Backend backend = Factory.buildFullClientBackend("http://localhost/~kali/lackr-examples/", null);
            Interpolr interpolr = Factory.buildInterpolr("handlebars esi");
            this.proxy = Factory.buildInterpolrProxy(interpolr, backend);
            proxy.start();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        proxy.handle(request, response);
    }

    public void destroy() {
        try {
            proxy.stop();
        } catch (Exception e) {
            // nothing
        }
    }
}
```

Wiring the servlet in web.xml
-----------------------------

```xml
     <servlet>
        <servlet-name>lackr</servlet-name>
        <servlet-class>LackrServlet</servlet-class>
        <async-supported>true</async-supported>
    </servlet>

    <servlet-mapping>
        <servlet-name>lackr</servlet-name>
        <url-pattern>/*</url-pattern>
    </servlet-mapping>
```

Note the "async-supported" attribute on the servlet declaration!

Building and testing it
-----------------------

Let's download the very convenient jetty-runner, build the project, and run it.

```
wget http://repo1.maven.org/maven2/org/eclipse/jetty/jetty-runner/9.0.7.v20131107/jetty-runner-9.0.7.v20131107.jar
mvn compile
java -jar jetty-runner-9.0.7.v20131107.jar target/lackr-servlet-demo/
```

I get a nasty-looking error and stack from jetty-runner (something about svn-javahl), but it runs fine anyway:

```
% curl -sv http://localhost:8080/ex2-esi1.html
I'm some text at the top of the main document before an ESI call.
I'm the content of ex2-shared-esi.html.

I'm some text at the bottom of the main document after an ESI call.
```

The rest is up to you :)
