module ServiceOne {
    requires Api;
    provides org.enso.test.layer.api.Api with 
            org.enso.test.layer.service1.ServiceOne;
}
