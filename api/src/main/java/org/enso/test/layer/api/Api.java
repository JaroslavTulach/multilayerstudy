package org.enso.test.layer.api;

import java.util.ServiceLoader;

public abstract class Api {
    protected abstract String sayHi();


    public static String hello() {
        return hello(Api.class.getModule().getLayer());
    }

    public static String hello(ModuleLayer... layers) {
        var sb = new StringBuilder();
        for (var layer : layers) {
            ServiceLoader<Api> loader = ServiceLoader.load(layer, Api.class);
            for (var impl : loader) {
                sb.append("\n");
                sb.append(impl.sayHi());
            }
        }
        return sb.toString();
    }
}
