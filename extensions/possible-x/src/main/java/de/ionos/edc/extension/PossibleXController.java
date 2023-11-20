package de.ionos.edc.extension;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Produces({MediaType.APPLICATION_JSON})
@Path("/isAlive")
public class PossibleXController {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getAll() {
        return "{\"isAlive\": \"true\"}";
    }
}
