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
        var action = "boot";
        if (args.length > 0) {
            action = args[0];
        }
        switch (action) {
            case "boot" -> bootLayer();
            case "single" -> singleLayer();
            case "double" -> doubleLayers();
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }

    private static void bootLayer() {
        System.err.println("bootLayer says: " + Api.hello());
    }

    private static void singleLayer() throws Exception {
        var layer = ModuleLayer.boot();

        var apiUrl = urlOf(Api.class);
        var oneUrl = urlOf(ServiceOne.class);
        var twoUrl = urlOf(ServiceTwo.class);
        var finder = finderOf(apiUrl, oneUrl, twoUrl);
        var moduleNames = Arrays.asList("Api", "ServiceOne", "ServiceTwo");
        var loader = new ModuleLayerLoader(new URL[] { apiUrl, oneUrl, twoUrl }, null, moduleNames);
        var pConfs = Collections.singletonList(layer.configuration());
        var pConf = Configuration.resolveAndBind(finder, pConfs, ModuleFinder.ofSystem(), moduleNames);
        var pLayers = Collections.singletonList(layer);
        var cntrl = ModuleLayer.defineModules(pConf, pLayers, (n) -> {
            if (moduleNames.contains(n)) {
                return loader;
            } else {
                return null;
            }
        });
        var apiClass = cntrl.layer().findLoader("Api").loadClass(Api.class.getName());
        assert apiClass.getClassLoader() == loader;

        var reply = apiClass.getMethod("hello").invoke(null);
        System.out.println(apiClass.getClassLoader() + " says " + reply);
    }

    private static void doubleLayers() throws Exception {
        ModuleLayer apiLayer;
        Class<?> apiClass;
        {
            var layer = ModuleLayer.boot();

            var apiUrl = urlOf(Api.class);
            var finder = finderOf(apiUrl);
            var apiNames = Arrays.asList("Api");
            var loader = new ModuleLayerLoader(new URL[] { apiUrl }, null, apiNames);
            var pConfs = Collections.singletonList(layer.configuration());
            var pConf = Configuration.resolveAndBind(finder, pConfs, ModuleFinder.ofSystem(), apiNames);
            var pLayers = Collections.singletonList(layer);
            var cntrl = ModuleLayer.defineModules(pConf, pLayers, (n) -> {
                if (apiNames.contains(n)) {
                    return loader;
                } else {
                    return null;
                }
            });
            apiLayer = cntrl.layer();
            apiClass = apiLayer.findLoader("Api").loadClass(Api.class.getName());
            assert apiClass.getClassLoader() == loader;
        }
        var oneUrl = urlOf(ServiceOne.class);
        var twoUrl = urlOf(ServiceTwo.class);
        var finder = finderOf(oneUrl, twoUrl);
        var moduleNames = Arrays.asList("ServiceOne", "ServiceTwo");
        var implLoader = new ModuleLayerLoader(new URL[] { oneUrl, twoUrl }, apiClass.getClassLoader(), moduleNames);
        var pConfs = Collections.singletonList(apiLayer.configuration());
        var pConf = Configuration.resolveAndBind(finder, pConfs, ModuleFinder.ofSystem(), moduleNames);
        var pLayers = Collections.singletonList(apiLayer);
        var cntrl = ModuleLayer.defineModules(pConf, pLayers, (n) -> {
            if (moduleNames.contains(n)) {
                return implLoader;
            } else {
                return null;
            }
        });
        var implLayer = cntrl.layer();
        var apiClass2 = implLayer.findLoader("Api").loadClass(Api.class.getName());
        assert apiClass2.getClassLoader() == apiClass2.getClassLoader();
        var serviceOneClass = implLayer.findLoader("ServiceOne").loadClass(ServiceOne.class.getName());
        assert serviceOneClass.getClassLoader() == implLoader;

        var reply = apiClass.getMethod("hello", ModuleLayer.class).invoke(null, implLayer);
        System.out.println(apiClass.getClassLoader() + "  says " + reply);
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

    private static class ModuleLayerLoader extends URLClassLoader {

        private final List<String> moduleNames;

        public ModuleLayerLoader(URL[] urls, ClassLoader parent, List<String> moduleNames) {
            super(urls, parent);
            this.moduleNames = moduleNames;
        }

        @Override
        protected Class<?> findClass(String moduleName, String name) {
            if (moduleNames.contains(moduleName)) {
                try {
                    return findClass(name);
                } catch (ClassNotFoundException ex) {
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return "ModuleLayerLoader for " + moduleNames;
        }
    }
}
