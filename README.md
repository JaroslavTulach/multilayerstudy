# Case Study of using `ModuleLayer`

Java introduced [Module system](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/module/package-summary.html)
in JDK 9. Among other things the _module system_ provides solution to two
major problems:
- definition of dependencies among modules
- registration and discovery of services via [ServiceLoader](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/ServiceLoader.html)

Both concepts works reasonably well in orchestration and
are subject of many introductory tutorials.
While this case study describes such a simple ["Boot Layer" case](#boot-layer) as well,
the goal of this study is to describe _something way more complex_.

Java module system offers **isolation** via so called [ModuleLayer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ModuleLayer.html)s.
There can be any number of layers and they run in a complete isolation.
Useful for contrainers that need to run _unknown libraries along each other_
(just like [Enso Java inter-operability](https://github.com/enso-org/enso/blob/develop/docs/polyglot/java.md#polyglot-syntax-system) support needs to load
many independently developed libraries).
Layers may have dependencies among each other - as such one can
form a _hierarchy of isolated layers_ and share those providing some APIs
and isolate those that need to be isolated.

One question remains: How can one use `ServiceLoader` in such a _multi layer_ setup? Is it possible
to **collect services from all layers**?


## TL;DR

The short answer is **yes**. The longer answer is given in ["Multi Layers"
example](#multi-layers). The short steps are:
- define [module with Api](https://github.com/JaroslavTulach/multilayerstudy/blob/master/api/src/main/java/org/enso/test/layer/api/Api.java)
- use `ServiceLoader.load(Api.class)` to load all implementations
- put that module into its own [ModuleLayer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ModuleLayer.html)
with special _parent class loader_ (described later)
- define as many modules as desirable providing implementation of the `Api` interface
- group those modules into as many [ModuleLayer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ModuleLayer.html)s
as necessary for isolation
- create an _artificial, empty module_
- include that artificial module into each of those layers
- create a chain of dummy `ClassLoader` instances linked via [getParent() field](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ClassLoader.html#%3Cinit%3E(java.lang.ClassLoader))
- in each of the layers associate the _artificial module_ with one of the class loaders
- when defining the _module layer_ with `Api` module, set its parent class loader to
the chain of the classloaders

The search logic in [ServiceLoader.load](https://docs.oracle.com/en%2Fjava%2Fjavase%2F21%2Fdocs%2Fapi%2F%2F/java.base/java/util/ServiceLoader.html#load(java.lang.Class,java.lang.ClassLoader))
in the _"bottom layer"_ of `Api` module will scan all parent classloader layers.
Thanks to the _artificial chain of parent classloaders_, it will find implementations
from all the [ModuleLayer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/ModuleLayer.html)
that otherwise **depend** on the _"botton layer"_.

Full implementation is in [multiLayers() method](https://github.com/JaroslavTulach/multilayerstudy/blob/d542d1532a29d9cd8e30a1f7246dc609ad5daae3/use/src/main/java/org/enso/test/layer/use/Use.java#L111).
Instructions how to use and debug the example are in the ["Multi Layers" section](#multi-layers).

### Building

First of build all the Maven projects with:

```bash
$ mvn clean install
```

### Note on Debugging

Every [decent IDE](http://netbeans.org) shall be able to open the sources
as they are organized into few nested Maven projects.

Each `exec:exec` command in this example accepts `-Dexec.vmArgs` parameter.
As such the _best way to configure debugging_ is to add:
```
-Dexec.vmArgs=-agentlib:jdwp=transport=dt_socket,address=5005
```
or similar and configure your IDE to listen on port 5005 for the connection
from the JVM running the given example.

### Boot Layer

Run from boot layer with `Use`, `Api` modules and also `ServiceOne` and `ServiceTwo` modules:
```bash
$ mvn -q -f use exec:exec
bootLayer says:
Ahoj by jdk.internal.loader.ClassLoaders$AppClassLoader@3f99bd52
Ciao by jdk.internal.loader.ClassLoaders$AppClassLoader@3f99bd52
```


Control the list of modules with `--limit-modules`:
```bash
$ mvn -q -f use exec:exec "-Dexec.vmArgs=--limit-modules Use"
bootLayer says:
$ mvn -q -f use exec:exec "-Dexec.vmArgs=--limit-modules Use,ServiceOne"
bootLayer says:
Ahoj by jdk.internal.loader.ClassLoaders$AppClassLoader@3d8c7aca
$ mvn -q -f use exec:exec "-Dexec.vmArgs=--limit-modules Use,ServiceTwo"
bootLayer says:
Ciao by jdk.internal.loader.ClassLoaders$AppClassLoader@668bc3d5
$ mvn -q -f use exec:exec "-Dexec.vmArgs=--limit-modules Use,ServiceTwo,ServiceOne"
bootLayer says:
Ahoj by jdk.internal.loader.ClassLoaders$AppClassLoader@668bc3d5
Ciao by jdk.internal.loader.ClassLoaders$AppClassLoader@668bc3d5
```

### Single Layer

Use `single` argument to create one instance of `ModuleLayer` and insert
into it the `Api`, `ServiceOne` and `ServiceTwo` modules. Then verify the
`ServiceLoader` lookup works with:
```bash
$ mvn -q -f use exec:exec -Dexec.appArgs=single
ModuleLayerLoader for [Api, ServiceOne, ServiceTwo] says
Ciao by ModuleLayerLoader for [Api, ServiceOne, ServiceTwo]
Ahoj by ModuleLayerLoader for [Api, ServiceOne, ServiceTwo]
```
As can be seen both `ServiceOne` and `ServiceTwo` are located and loaded.

### Double Layers

This case sets two `ModuleLayer`s up: one layer for the `Api` module and
second layer for `ServiceOne` and `ServiceTwo` modules. Verify with:

```bash
$ mvn -q -f use package exec:exec -Dexec.appArgs=double
ModuleLayerLoader for [Api]  says
Ciao by ModuleLayerLoader for [ServiceOne, ServiceTwo]
Ahoj by ModuleLayerLoader for [ServiceOne, ServiceTwo]
```

As can be seen the `Api` module finds both services loaded by the other layer.

### Multi Layers

This case let the
[multiLayers() method](https://github.com/JaroslavTulach/multilayerstudy/blob/d542d1532a29d9cd8e30a1f7246dc609ad5daae3/use/src/main/java/org/enso/test/layer/use/Use.java#L111)
set three `ModuleLayer`s up:
- one layer for the `Api` module
- one layer for the `ServiceOne` module
- one layer for the `ServiceTwo` module

Verify the example by running:

```bash
$ mvn -q -f use package exec:exec -Dexec.appArgs=multi
ModuleLayerLoader for [Api]  says
Ahoj by ModuleLayerLoader for [ServiceOne]
Ciao by ModuleLayerLoader for [ServiceTwo]
```

As can be seen the `Api` module finds both services loaded by the other layers.
