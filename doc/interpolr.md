Interpolr
=========

Internal Architecture
---------------------

Interpolr is where the response bodies are scanned and processed. It is quite modular in its design, allowing to 
extend it featureset with generic features (like ESIs) or with very application oriented features.

Interpolr has been designed to be testable without HTTP, so we have introduced a few classes and interfaces
abstracting away the HTTP and Servlet stuff.

InterpolrProxy and a few others classes from its package are the link between the Interpolr world and the
"Proxy" world:

- Interpolr is meant to be a singleton in your application, holding, as a list of Plugins the various processor
  that will work over the documents
- InterpolrContext is an interface covering the context of processing of a page. It is implemented by
  InterpolrFrontendRequest in the context of proxy: it contains all the information associated with the execution
  of one incoming request on lackr. One will be created for every incoming request.
  For tests, we use a InterpolrContextStub implementation.
- In a similar fashion, InterpolrScope abstracts the Backend requests. It is implemented by InterpolrBackendRequest
  for use in real life, or InterpolrScopeStub in tests.
- Special attention has been given to sensible optimization of the text and string processing: we have chose to keep
  response bodies in byte array form as much as possible, and make efforts to avoid copying the same text over and
  over. The Chunk interface generalize what we need to be able to do with these byte[]-like. In its simple form, it is 
  implemented as a ConstantChunk (an actual byte[]), but it can go as far as representing a Future-like response to
  an ESI request, (see RequestChunk) or the result of Handlebars evaluation. Document is a list of Chunk to be 
  concatenated.


