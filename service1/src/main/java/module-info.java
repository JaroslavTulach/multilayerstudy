import org.enso.test.layer.api.Api;
import org.enso.test.layer.service1.ServiceOne;

module ServiceOne {
    requires Api;
    provides Api with ServiceOne;

    exports org.enso.test.layer.service1 to Use;
}
