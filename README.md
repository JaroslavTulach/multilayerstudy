# Case Study of using `ModuleLayer`

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

This case sets three `ModuleLayer`s up:
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
