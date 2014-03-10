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

Edge Side Include
-----------------

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
