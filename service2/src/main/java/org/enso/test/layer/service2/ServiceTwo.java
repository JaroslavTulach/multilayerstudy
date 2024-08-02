package org.enso.test.layer.service2;

import org.enso.test.layer.api.Api;

public class ServiceTwo extends Api {

    @Override
    public String sayHi() {
        return "Ciao by " + getClass().getClassLoader();
    }
}
