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
    * Authorization (using delegate methods)
    * Source access
    * Image processing
    * Caching

# Development

For development, a Galia JAR must be present on the classpath. One way to
get it there is to install it into your local Maven repository:

```sh
mvn install:install-file -Dfile=galia-core-1.0.1.jar -DgroupId=is.galia \
    -DartifactId=galia-core -Dversion=1.0.1 -Dpackaging=jar
```

# How it works

The idea is to use Maven to build a JAR file that declares an implementation
of Galia's `is.galia.resource.Resource`
[service](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/class-use/ServiceLoader.html).
So you must have Maven installed, but that is the only dependency. You could
also use Gradle, or some other build tool, or no build tool.

`mvn package` will build a JAR file in the `target` directory. Copy it into
Galia's `plugins` directory and restart Galia. The resource is now available at
`/thumbs`. Try it out with a request like:

[http://localhost:8182/thumbs?identifier=my-image.jpg](http://localhost:8182/thumbs?identifier=my-image.jpg)

# Using dependencies

This example does not use any third-party dependencies, which keeps things
simple. But if you wanted to bring in other dependencies, the way other plugins
do it is by including them in a `lib` folder within the plugin's folder, where
the plugin class loader will find them.

When choosing a dependency, first check whether Galia includes it, and use
that, if possible. Bundling a different version of a dependency than the one
used by Galia could cause problems.

# API Compatibility

Galia makes an effort to maintain semantic versioning. New patch versions
are always compatible; new minor releases should be backwards-compatible;
and new major ones will probably not be.
