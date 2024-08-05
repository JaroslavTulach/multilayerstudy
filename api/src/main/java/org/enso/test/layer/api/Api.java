package org.enso.test.layer.api;

import java.util.ServiceLoader;

public abstract class Api {
    protected abstract String sayHi();

    public static String hello() {
        var sb = new StringBuilder();
        ServiceLoader<Api> loader = ServiceLoader.load(Api.class, Api.class.getClassLoader());
        for (var impl : loader) {
            sb.append("\n");
            sb.append(impl.sayHi());
        }
        return sb.toString();
    }
}
