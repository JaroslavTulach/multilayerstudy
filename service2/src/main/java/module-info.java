import org.enso.test.layer.api.Api;
import org.enso.test.layer.service2.ServiceTwo;


module ServiceTwo {
    requires Api;
    provides Api with ServiceTwo;

    exports org.enso.test.layer.service2 to Use;
}
