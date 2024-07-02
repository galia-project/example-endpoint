# Example Galia Resource

This resource adds a basic "thumbnailer" endpoint at `/thumbs`. It
demonstrates how to:

* Add resource routes
* Access the application configuration
* Parse query arguments
* Assemble image processing directives
* Respond to a request
    * Set the response status
    * Add headers
    * Write to the response body
* Process images using an ImageRequestHandler, which itself abstracts away all
  of the complexities related to:
    * Authorization (using the delegate `pre_authorize()` and `authorize()`
      methods)
    * Source access
    * Image processing
    * Caching

# Development

For development, a Galia JAR must be present on the classpath. One way to
get it there is to run `mvn install` from the Galia source root, which will
install it into your local repository. (This is the method used by this
example.)

# How it works

The idea is to use Maven to build a JAR file that declares an implementation
of Galia's `is.galia.resource.Resource`
[service](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/class-use/ServiceLoader.html).
So you must have Maven installed, but that is the only dependency. You could
also use Gradle, or some other build tool.

`mvn package` will build a JAR file in the `target` directory. Copy it into
Galia's `plugins` directory and restart Galia. The resource is now available at
`/thumbs`. Try it out with a request like:

[http://localhost:8182/thumbs?identifier=my-image.jpg](http://localhost:8182/thumbs?identifier=my-image.jpg)

# Using dependencies

This example does not use any third-party dependencies, which keeps things
simple. Most real-world resources probably wouldn't need to rely on any such
dependencies.

If you wanted to bring in other dependencies, the way other plugins do it is by
including them in a `lib` folder within the plugin's folder.

When choosing a dependency, first check whether Galia includes it, and use
that, if possible. Bundling a different version of a dependency than the one
used by Galia could cause problems.

# API Compatibility

Galia makes an effort to maintain semantic versioning, but API compatibility
across minor releases is not guaranteed. 
