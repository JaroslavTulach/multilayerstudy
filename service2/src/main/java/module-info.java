module ServiceTwo {
    requires Api;
    provides org.enso.test.layer.api.Api with 
            org.enso.test.layer.service2.ServiceTwo;
}
