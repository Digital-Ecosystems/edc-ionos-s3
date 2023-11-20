package de.ionos.edc.extension;

import org.eclipse.edc.connector.api.control.configuration.ControlApiConfiguration;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;
import org.eclipse.edc.web.spi.WebService;

public class PossibleXExtension implements ServiceExtension {

    public static final String EXTENSION_NAME = "PossibleXExtension";
    @Inject
    private ControlApiConfiguration config;
    @Inject
    private WebService webService;

    @Override
    public String name() {
        return EXTENSION_NAME;
    }

    @Override
    public void initialize(ServiceExtensionContext context) {
        var controller = new PossibleXController();
        webService.registerResource(config.getContextAlias(), controller);
    }
}
