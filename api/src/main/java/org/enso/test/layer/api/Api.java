package org.enso.test.layer.api;

import java.util.ServiceLoader;

public abstract class Api {
    protected abstract String sayHi();


    public static String hello() {
        var sb = new StringBuilder();
        var sep = "";
        for (var impl : ServiceLoader.load(Api.class)) {
            sb.append(sep);
            sb.append(impl.sayHi());
            sep = " ";
        }
        return sb.toString();
    }
}
