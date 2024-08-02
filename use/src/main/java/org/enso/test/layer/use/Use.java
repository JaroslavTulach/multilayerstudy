package org.enso.test.layer.use;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Use {

    public static void main(String[] args) throws Exception {
        final URL myUrl = Use.class.getProtectionDomain().getCodeSource().getLocation();
        var myPath = Paths.get(myUrl.toURI());
        ModuleFinder finder = ModuleFinder.of(myPath);
        final List<String> moduleNames = finder.findAll().stream().map(x -> x.descriptor().name()).collect(Collectors.toList());
        ModuleLayer emptyL = ModuleLayer.empty();
        ClassLoader emptyLoader = new URLClassLoader(new URL[] { myUrl });
        final List<Configuration> pConfs = Collections.singletonList(ModuleLayer.boot().configuration());
        Configuration pConf = Configuration.resolve(finder, pConfs, finder, moduleNames);
        List<ModuleLayer> pLayers = Collections.singletonList(ModuleLayer.boot());
        ModuleLayer.Controller cntrl = ModuleLayer.defineModules(pConf, pLayers, (n) -> {
            System.err.println("loader for " + n);
            return emptyLoader;
        });
        System.out.println("Hello World!" + cntrl.layer().modules());
    }
}
