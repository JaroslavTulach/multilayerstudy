package org.enso.test.layer.api;

import java.util.ServiceLoader;

public abstract class Api {
    protected abstract String sayHi();


    public static String hello() {
        ServiceLoader<Api> loader = null;
        if (loader == null) {
            try {
                loader = iterateThru(ServiceLoader.load(Api.class));
            } catch (Error | Exception ex) {
                ex.printStackTrace();
            }
        }
        if (loader == null) {
            try {
                loader = iterateThru(ServiceLoader.load(Api.class.getModule().getLayer(), Api.class));
            } catch (Error | Exception ex) {
                ex.printStackTrace();
            }
        }
        if (loader == null) {
            try {
                loader = iterateThru(ServiceLoader.load(Api.class, Api.class.getClassLoader()));
            } catch (Error | Exception ex) {
                ex.printStackTrace();
            }
        }

        var sb = new StringBuilder();
        var sep = "";
        for (var impl : loader) {
            sb.append(sep);
            sb.append(impl.sayHi());
            sep = " ";
        }
        return sb.toString();
    }

    private static ServiceLoader<Api> iterateThru(ServiceLoader<Api> load) {
        for (var api : load) {
            System.out.println(api);
        }
        return load;
    }
}
