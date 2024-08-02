# Study of using `--module-info`

First of build all the Maven project with:

```bash
$ mvn clean install
```

Run from boot layer with `Use`, `Api` modules and also `ServiceOne` and `ServiceTwo` modules:
```bash
$ mvn -q -f use exec:exec
bootLayer says: Ahoj Ciao
```

Control the list of modules with `--limit-modules`:
```bash
$ mvn -q -f use exec:exec "-Dexec.vmArgs=--limit-modules Use"
bootLayer says:
$ mvn -q -f use exec:exec "-Dexec.vmArgs=--limit-modules Use,ServiceOne"
bootLayer says: Ahoj
$ mvn -q -f use exec:exec "-Dexec.vmArgs=--limit-modules Use,ServiceTwo"
bootLayer says: Ciao
$ mvn -q -f use exec:exec "-Dexec.vmArgs=--limit-modules Use,ServiceTwo,ServiceOne"
bootLayer says: Ahoj Ciao
```
