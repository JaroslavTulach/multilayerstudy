package org.enso.test.layer.use;


import org.enso.test.layer.api.Api;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.enso.test.layer.service1.ServiceOne;
import org.enso.test.layer.service2.ServiceTwo;

public class Use {

    public static void main(String[] args) throws Exception {
        var action = "single";
        if (args.length > 0) {
            action = args[0];
        }
        switch (action) {
            case "boot" -> bootLayer();
            case "single" -> singleLayer();
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }

    private static void bootLayer() {
        System.err.println("bootLayer says: " + Api.hello());
    }

    private static void singleLayer() throws Exception {
        ModuleLayer layer = ModuleLayer.boot();

        URL apiUrl = urlOf(Api.class);
        URL oneUrl = urlOf(ServiceOne.class);
        URL twoUrl = urlOf(ServiceTwo.class);
        ModuleFinder finder = finderOf(apiUrl, oneUrl, twoUrl);
        var moduleNames = Arrays.asList("Api", "ServiceOne", "ServiceTwo");
        var loader = new URLClassLoader(new URL[] { apiUrl, oneUrl, twoUrl }, null);
        var pConfs = Collections.singletonList(layer.configuration());
        var pConf = Configuration.resolveAndBind(finder, pConfs, ModuleFinder.ofSystem(), moduleNames);
        var pLayers = Collections.singletonList(layer);
        ModuleLayer.Controller cntrl = ModuleLayer.defineModules(pConf, pLayers, (n) -> {
            System.err.println("loader for " + n);
            return loader;
        });
        var apiClass = cntrl.layer().findLoader("Api").loadClass(Api.class.getName());
        assert apiClass.getClassLoader() == loader;

        Thread.currentThread().setContextClassLoader(loader);
        var reply = apiClass.getMethod("hello").invoke(null);
        System.out.println(apiClass.getClassLoader() + " " + apiClass.getProtectionDomain().getCodeSource().getLocation() + " says " + reply);
    }

    private static URL urlOf(Class<?> aClass) {
        final URL myUrl = aClass.getProtectionDomain().getCodeSource().getLocation();
        return myUrl;
    }

    private static ModuleFinder finderOf(URL... urls) {
        var paths = Arrays.asList(urls).stream().map((u) -> {
            try {
                return Paths.get(u.toURI());
            } catch (URISyntaxException ex) {
                throw new IllegalStateException(ex);
            }
        });
        return ModuleFinder.of(paths.toArray(Path[]::new));
    }
}
