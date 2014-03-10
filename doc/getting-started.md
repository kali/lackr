Try lackr
=========

Build and run the demos
-----------------------

You will need git, java and maven installed. A working http server able to deliver files
will be required too. The built-in OSX support for http sharing will do, or a userdir-able
server on linux.

```
git clone git@github.com:fotonauts/lackr.git
cd lackr
mvn compile exec:java
```

When maven is down downloading half the Internet, you should get:

```
[...]
export BACKEND=http://localhost/~kali/lackr-examples/
export PROXY=http://localhost:8000/
```

The Demo server does its best to find an available TCP prot above 8000, and to
guess a "backend" url. If the backend url does not work for you, you can override it by running instead:

```
mvn exec:java -Dexec.args=http://whatever.suits/you/
```

If you want to run the snippets below, I suggest you cut and paste the two last
lines of maven output to setup the shell variables.

Leave the proxy alone, and copy or link the directory doc/lackr-examples at the appropriate
location. Usually symlink are not followed so it's better to copy. 
To check that the "backend" is working as expected, run:

```
% curl "${BACKEND}ex1.html"
Hello world!
```

Now let's try the proxy:

```
% curl "${BACKEND}ex1.html"
Hello world!
```

Ok!

Basic Edge Side Include
-----------------------

Let's have a look at ex2-esi.html.

```
% curl ${BACKEND}ex2-esi1.html
I'm some text at the top of the main document before an ESI call.
<!--# include virtual="/ex2-shared-esi.html" -->
I'm some text at the bottom of the main document after an ESI call.
```

There is obviously some exotic markup on the second line that lackr will catch, and 
replace with the result of another request:

```
% curl ${BACKEND}ex2-shared-esi.html
I'm the content of ex2-shared-esi.html.
```

Let's check out the result:

```
% curl ${PROXY}ex2-esi1.html
I'm some text at the top of the main document before an ESI call.
I'm the content of ex2-shared-esi.html.

I'm some text at the bottom of the main document after an ESI call.
```

Here we go. That's probably the most basic feature of lackr. At this point this is roughly
equivalent of what Varnish and nginx (or apache respectable mod_ssi) do out of the box.

ESI and JSON
------------

But we can go a bit further. Lackr detects other variants of ESI markup...

```
% curl ${BACKEND}ex2-esi2.html
<script> var esi = "ssi:include:virtual:/ex2-shared-esi.json"; </script>
% curl ${BACKEND}/ex2-shared-esi.json
{ "foo" : "bar", "bar": "baz" }
% curl ${PROXY}ex2-esi2.html
<script> var esi = { "foo" : "bar", "bar": "baz" }
; </script>
```

And as lackr also look for markup in JSON files...

```
% curl ${BACKEND}ex2-esi2.json
{ "some" : "wrapper", "content" : "ssi:include:virtual:/ex2-shared-esi.json" }
% curl ${PROXY}ex2-esi2.json
{ "some" : "wrapper", "content" : { "foo" : "bar", "bar": "baz" }
 }
```

Note that _some_ provision is made for escaping combination like html included in json:

```
% curl ${BACKEND}ex2-esi3.json
{ "some" : "wrapper", "content" : "ssi:include:virtual:/ex2-shared-esi2.html" }
% curl ${BACKEND}ex2-shared-esi2.html
I'm some "complicated" html to be <i>included</i> in JSON.
% curl ${PROXY}ex2-esi3.json
{ "some" : "wrapper", "content" : "I'm some \"complicated\" html to be <i>included<\/i> in JSON.\n" }
```

Note how the html is transformed in a json string, with its double-quote escaped.

All escaping scenario are not covered here (and some would be very difficult to do), 
so if you plan on doing some very tricky inclusion, you may encounter some issues.
Test thoroughly your use cases. But HTML in HTML works, and JSON in JSON works.

Handlebars
----------

Handlebars, as you may now, is a very lightweight template language. It is an extension
of Mustache, meant to solve some of Mustache limitations while staying easy to 
port to various virtual machines. The idea is that you can use the same Handlebars
template in various contexts, like a Ruby-on-Rails backend, a Java proxy (lackr, for
instance) and the brosers javascript engine.

```
% curl ${BACKEND}ex3-hbs1.html
<!-- lackr:handlebars:template name="template_name" -->
    some text from the template name:{{name}} value:{{value}}
<!-- /lackr:handlebars:template -->
<!-- lackr:handlebars:eval name="template_name" -->
    { "name": "the name", "value": "the value" }
<!-- /lackr:handlebars:eval -->
% curl ${PROXY}ex3-hbs1.html


    some text from the template name:the name value:the value

```

Here we introduce more lackr exotic markup, to declare a handlebars template (which will
be filtered out of the resulting page) and use it once by evaluating it against a JSON
object.

Now, the *real* fun starts when we mix this with the ESI from the previous section...

```
% curl ${BACKEND}ex3-hbs2.html
<!-- lackr:handlebars:template name="template_name" -->
    some text from the template name:{{bar}} value:{{foo}}
<!-- /lackr:handlebars:template -->
<!-- lackr:handlebars:eval name="template_name" -->
    "ssi:include:virtual:/ex2-shared-esi.json"
<!-- /lackr:handlebars:eval -->
% curl ${PROXY}ex3-hbs2.html


    some text from the template name:baz value:bar
```
