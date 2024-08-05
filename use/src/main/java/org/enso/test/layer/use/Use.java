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
import java.util.function.Function;
import org.enso.test.layer.fake.Fake;
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
            case "multi" -> multiLayers();
            default -> throw new IllegalArgumentException("Unknown command: " + args[0]);
        }
    }

    private static void bootLayer() {
        println("bootLayer says: ", Api.hello());
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
        println(apiClass.getClassLoader() + " says ", reply);
    }

    private static void doubleLayers() throws Exception {
        final ClassLoader baseLoader = new URLClassLoader(new URL[0], null);
        ModuleLayer apiLayer;
        Class<?> apiClass;
        {
            var layer = ModuleLayer.boot();

            var apiUrl = urlOf(Api.class);
            var finder = finderOf(apiUrl);
            var apiNames = Arrays.asList("Api");
            var loader = new ModuleLayerLoader(new URL[] { apiUrl }, baseLoader, apiNames);
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
        var fakeUrl = urlOf(Fake.class);
        var finder = finderOf(oneUrl, twoUrl, fakeUrl);
        var moduleNames = Arrays.asList("ServiceOne", "ServiceTwo", FindCL.FAKE_MODULE_NAME);
        var implLoader = new ModuleLayerLoader(new URL[] { oneUrl, twoUrl }, apiClass.getClassLoader(), moduleNames);
        var pConfs = Collections.singletonList(apiLayer.configuration());
        var pConf = Configuration.resolveAndBind(finder, pConfs, ModuleFinder.ofSystem(), moduleNames);
        var pLayers = Collections.singletonList(apiLayer);
        var cntrl = ModuleLayer.defineModules(pConf, pLayers, new FindCL(baseLoader, implLoader, moduleNames));
        var implLayer = cntrl.layer();
        var apiClass2 = implLayer.findLoader("Api").loadClass(Api.class.getName());
        assert apiClass2.getClassLoader() == apiClass2.getClassLoader();
        var serviceOneClass = implLayer.findLoader("ServiceOne").loadClass(ServiceOne.class.getName());
        assert serviceOneClass.getClassLoader() == implLoader;

        var reply = apiClass.getMethod("hello").invoke(null);
        println(apiClass.getClassLoader() + "  says ", reply);
    }

    private static void multiLayers() throws Exception {
        var fakeUrl = urlOf(Fake.class);

        final ClassLoader baseLoader = new URLClassLoader(new URL[0], null);
        final ClassLoader sndLoader = new URLClassLoader(new URL[0], baseLoader);

        final ModuleLayer apiLayer;
        final Class<?> apiClass;
        {
            var layer = ModuleLayer.boot();

            var apiUrl = urlOf(Api.class);
            var finder = finderOf(apiUrl);
            var apiNames = Arrays.asList("Api");
            var loader = new ModuleLayerLoader(new URL[] { apiUrl }, sndLoader, apiNames);
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
        final ClassLoader oneLoader;
        final ModuleLayer oneLayer;
        {
            var oneUrl = urlOf(ServiceOne.class);
            var finder = finderOf(oneUrl, fakeUrl);
            var moduleNames = Arrays.asList("ServiceOne", FindCL.FAKE_MODULE_NAME);
            oneLoader = new ModuleLayerLoader(new URL[] { oneUrl }, apiClass.getClassLoader(), moduleNames);
            var pConfs = Collections.singletonList(apiLayer.configuration());
            var pConf = Configuration.resolveAndBind(finder, pConfs, ModuleFinder.ofSystem(), moduleNames);
            var pLayers = Collections.singletonList(apiLayer);
            var cntrl = ModuleLayer.defineModules(pConf, pLayers, new FindCL(baseLoader, oneLoader, moduleNames));
            oneLayer = cntrl.layer();
        }
        final ClassLoader twoLoader;
        final ModuleLayer twoLayer;
        {
            var twoUrl = urlOf(ServiceTwo.class);
            var finder = finderOf(twoUrl, fakeUrl);
            var moduleNames = Arrays.asList("ServiceTwo", FindCL.FAKE_MODULE_NAME);
            twoLoader = new ModuleLayerLoader(new URL[] { twoUrl }, apiClass.getClassLoader(), moduleNames);
            var pConfs = Collections.singletonList(apiLayer.configuration());
            var pConf = Configuration.resolveAndBind(finder, pConfs, ModuleFinder.ofSystem(), moduleNames);
            var pLayers = Collections.singletonList(apiLayer);
            var cntrl = ModuleLayer.defineModules(pConf, pLayers, new FindCL(sndLoader, twoLoader, moduleNames));
            twoLayer = cntrl.layer();
        }
        var apiClass2 = twoLayer.findLoader("Api").loadClass(Api.class.getName());
        assert apiClass2.getClassLoader() == apiClass2.getClassLoader();
        var serviceOneClass = oneLayer.findLoader("ServiceOne").loadClass(ServiceOne.class.getName());
        assert serviceOneClass.getClassLoader() == oneLoader;
        var serviceTwoClass = twoLayer.findLoader("ServiceTwo").loadClass(ServiceTwo.class.getName());
        assert serviceTwoClass.getClassLoader() == twoLoader;

        var reply = apiClass.getMethod("hello").invoke(null);
        println(apiClass.getClassLoader() + "  says ", reply);
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

    private static final class ModuleLayerLoader extends URLClassLoader {

        private final List<String> moduleNames;

        ModuleLayerLoader(URL[] urls, ClassLoader parent, List<String> moduleNames) {
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

    private static final class FindCL implements Function<String, ClassLoader> {
        static final String FAKE_MODULE_NAME = "Fake000Module";

        private final ClassLoader useLoader;
        private final ClassLoader loader;
        private final List<String> moduleNames;

        FindCL(ClassLoader useLoader, ClassLoader loader, List<String> moduleNames) {
            this.useLoader = useLoader;
            this.loader = loader;
            this.moduleNames = moduleNames;
        }

        @Override
        public ClassLoader apply(String module) {
            if (FAKE_MODULE_NAME.equals(module)) {
                return useLoader;
            }
            if (moduleNames.contains(module)) {
                return loader;
            }
            return null;
        }
    }

    private static void println(String prefix, Object reply) {
        System.out.println(prefix + reply);
        assert reply instanceof String : "Expecting reply to be a String";
        assert reply.toString().contains("Ahoj") : "There should be Ahoj in the reply";
        assert reply.toString().contains("Ciao") : "There should be Ciao in the reply";
    }
}
