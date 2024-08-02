package org.enso.test.layer.service1;

import org.enso.test.layer.api.Api;

public class ServiceOne extends Api {
    @Override
    public String sayHi() {
        return "Ahoj";
    }
}
