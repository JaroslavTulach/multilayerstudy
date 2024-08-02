package org.enso.test.layer.api;

import java.util.ServiceLoader;

public abstract class Api {
    protected abstract String sayHi();


    public static String hello() {
        return hello(Api.class.getModule().getLayer());
    }

    public static String hello(ModuleLayer layer) {
        ServiceLoader<Api> loader = ServiceLoader.load(layer, Api.class);
        var sb = new StringBuilder();
        var sep = "";
        for (var impl : loader) {
            sb.append(sep);
            sb.append(impl.sayHi());
            sep = " ";
        }
        return sb.toString();
    }
}
