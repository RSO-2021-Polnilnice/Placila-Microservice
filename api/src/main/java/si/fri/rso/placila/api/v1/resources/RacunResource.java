package si.fri.rso.placila.api.v1.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.cors.annotations.CrossOrigin;
import com.kumuluz.ee.discovery.annotations.DiscoverService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.json.JSONObject;
import si.fri.rso.placila.lib.Racun;
import si.fri.rso.placila.lib.Termin;
import si.fri.rso.placila.lib.Uporabnik;
import si.fri.rso.placila.models.converters.RacunConverter;
import si.fri.rso.placila.models.entities.RacunEntity;
import si.fri.rso.placila.services.beans.RacunBean;
import si.fri.rso.placila.services.config.RestProperties;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Path("/placila")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@CrossOrigin(allowOrigin = "*")
public class RacunResource {

    @Inject
    private RestProperties pricePerHour;

    @Inject
    private RacunBean racunBean;

    // Dependency on polnilnice microservice
    @Inject
    @DiscoverService(value = "polnilnice-service", environment = "dev", version = "1.0.0")
    private Optional<String> polnilnice_host;

    // Dependency on users microservice
    @Inject
    @DiscoverService(value = "uporabniki-service", environment = "dev", version = "1.0.0")
    private Optional<String> uporabniki_host;

    CloseableHttpClient httpClient = HttpClients.createDefault();
    ObjectMapper mapper = new ObjectMapper();


    /** GET price per hour of charging **/
    @Operation(description = "Get price per hour for charging station bookings.", summary = "Per hour")
    @APIResponses({
        @APIResponse(responseCode = "200",
                description = "Price per hour"),
        @APIResponse(responseCode = "404",
                description = "Price per hour not found.")
    })
    @GET
    @Path("/cenik")
    @Produces("application/json")
    public Response getRacuniByUserId() {
        if (pricePerHour.getPricePerHour() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.OK).entity(pricePerHour.getPricePerHour()).build();
    }
    @Operation(description = "Get transactions for user with provided ID.", summary = "Get transactions for user.")
    @APIResponses({
            @APIResponse(responseCode = "200",
                    description = "List of transactions",
                    content = @Content(
                            schema = @Schema(implementation = Racun.class))),
            @APIResponse(responseCode = "404",
                    description = "User with this ID was not found")
    })
    /** GET racuni for user **/
    @GET
    @Path("/uporabnik/{userId}")
    @Produces("application/json")
    public Response getRacuniByUserId(@PathParam("userId") Integer userId) {

        List<Racun> racunList = racunBean.getRacuniByCustomerId(userId);
        if (racunList == null || racunList.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.OK).entity(racunList).build();
    }

    /** GET racun by id **/
    @Operation(description = "Get transactions with provided ID.", summary = "Get transaction.")
    @APIResponses({
            @APIResponse(responseCode = "200",
                    description = "Transactions with provided ID.",
                    content = @Content(
                            schema = @Schema(implementation = Racun.class))),
            @APIResponse(responseCode = "404",
                    description = "Transaction ID was not found")
    })
    /** GET racuni for user **/
    @GET
    @Path("/{id}")
    @Produces("application/json")
    public Response getRacunById(@PathParam("id") Integer id) {

        Racun racun = racunBean.getRacunById(id);
        if (racun == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.OK).entity(racun).build();
    }

    @Operation(description = "Book a timeslot using service discovery. Checks funds by calling user microservice and availability on charging stations microservice.", summary = "Book a timeslot.")
    @APIResponses({
            @APIResponse(responseCode = "400",
                    description = "Incorrect POST body."
            ),
            @APIResponse(responseCode = "404",
                    description = "Charging station with this ID does not exist."
            ),
            @APIResponse(responseCode = "500",
                    description = "Unknown error while booking."
            ),
            @APIResponse(responseCode = "200",
                    description = "Booking succesful, returns a transaction confirmation.",
                    content = @Content(
                            schema = @Schema(implementation = Racun.class))),
    })
    /** Create new racun **/
    @POST
    @Path("/rezerviraj_")
    @Produces("application/json")
    public Response createRacun(@RequestBody(
            description = "DTO object with image metadata.",
            required = true, content = @Content(
            schema = @Schema(implementation = Racun.class))) Racun r) {
        System.out.println("Racun: " + r.toString());
        // check body
        if (r.getCustomerId() == null || r.getCustomerUsername().isEmpty() || r.getCustomerEmail().isEmpty()
            || r.getCustomerFirstName().isEmpty() || r.getCustomerLastName().isEmpty()
            || r.getPolnilnicaId() == null || r.getTerminDateFrom() == null || r.getTerminDateTo() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("customerId, customerUsername, customerEmail, customerFirstName, customerLastName, terminId, terminDateFrom, terminDateTo are necessary parameters").build();
        }

        if (r.getTerminDateTo() - r.getTerminDateFrom() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Impossible time bracket provided.").build();
        }


        /** Call polnilnica MS and check if termin is actually available  **/
        // Get list of termini for polnilnica with provided id
        String polnilnicaTerminiString = myHttpGet(polnilnice_host.get() + "/v1/polnilnice/" + r.getPolnilnicaId() + "/termini");
        // Map json string into Termin list
        List<Termin> terminList = null;
        try {
            terminList =  mapper.readValue(polnilnicaTerminiString, new TypeReference<List<Termin>>() {});
        } catch (JsonProcessingException e) {
            // Didn't receive list of termini, check if polnilnica exists
            if (polnilnicaTerminiString.startsWith("Polnilnica with id")) {
                return Response.status(Response.Status.NOT_FOUND).entity(polnilnicaTerminiString).build();
            }
            // If polnilnica doesn't exist and the service didn't say the termini list is empty, something went wrong there
            if (!polnilnicaTerminiString.startsWith("No termini found")) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(polnilnicaTerminiString).build();
            }
        }
        // Check for overlapping if we received a list of termini
        if (terminList != null && !terminList.isEmpty()) {
            for (int i = 0; i < terminList.size(); i++) {
                Long listTerminFrom = terminList.get(i).getDateFrom();
                Long listTerminTo = terminList.get(i).getDateTo();

                if (r.getTerminDateFrom() <= listTerminTo && listTerminFrom <= r.getTerminDateTo()) {
                    System.out.println("Your timeframe overlaps with termin with id" + terminList.get(i).getId() + ".");
                    return Response.status(Response.Status.BAD_REQUEST).entity("Your timeframe overlaps with termin with id " + terminList.get(i).getId() + ".").build();
                }
            }
        }
        // No overlapping found

        /** Call user MS and check if user has adequate funds **/
        String uporabnikiString = myHttpGet(uporabniki_host.get() + "/v1/uporabniki/" + r.getCustomerId());
        Uporabnik uporabnik = null;
        try {
            uporabnik =  mapper.readValue(uporabnikiString, Uporabnik.class);
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Uporabnik with id " + r.getCustomerId() + " does not exist.").build();
        }
        // Check his balance
        if (pricePerHour.getPricePerHour() == null ) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Price per hour is not available and we cannot book. Sorry.").build();
        }
        Long diff = r.getTerminDateTo() - r.getTerminDateFrom();
        Float wtf = (diff.floatValue() / 3600f);
        Float terminPrice = wtf * pricePerHour.getPricePerHour();
        System.out.println("wtf: " + wtf);
        System.out.println("termin price: " + terminPrice);
        if (uporabnik.getFunds() < terminPrice) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Not enough funds.").build();
        }

        /** Call polnilnica MS and book the termin **/
        // Create json body for POST call
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("userId", r.getCustomerId());
        jsonBody.put("dateFrom", r.getTerminDateFrom());
        jsonBody.put("dateTo", r.getTerminDateTo());
        // POST the termin to polnilnica api
        String polnilnicaPostResponse = myHttpPost(polnilnice_host.get() + "/v1/polnilnice/" + r.getPolnilnicaId() + "/termini", jsonBody.toString());
        // If everything went ok, we get the termin as response
        Termin termin = null;
        try {
            termin =  mapper.readValue(polnilnicaPostResponse, Termin.class);
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong while booking.").build();
        }
        /** Call uporabniki MS and substract funds **/
        uporabnik.setFunds(uporabnik.getFunds() - terminPrice);
        String putBodyString = null;
        try {
            putBodyString = mapper.writeValueAsString(uporabnik);
        } catch (JsonProcessingException e) {
            myHttpsDelete(polnilnice_host.get() + "/v1/termini/" + termin.getId());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong while updating user value.").build();
        }
        String uporabnikPutResponse = myHttpPut(uporabniki_host.get() + "/v1/uporabniki/" + r.getCustomerId(), putBodyString);
        try {
            uporabnikPutResponse = mapper.writeValueAsString(uporabnik);
        } catch (JsonProcessingException e) {
            myHttpsDelete(polnilnice_host.get() + "/v1/termini/" + termin.getId());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong while updating user funds.").build();
        }

        r.setStatus("payment");
        r.setPrice(terminPrice);
        r.setTerminId(termin.getId());
        r.setTimestamp(System.currentTimeMillis() / 1000L);
        Racun racun = racunBean.createRacun(r);

        return Response.status(Response.Status.CREATED).entity(racun).build();

    }
    @Operation(description = "Book a timeslot using provided service IP-s in POST body. Checks funds by calling user microservice and availability on charging stations microservice.", summary = "Book a timeslot.")
    @APIResponses({
            @APIResponse(responseCode = "400",
                    description = "Incorrect POST body."
            ),
            @APIResponse(responseCode = "404",
                    description = "Charging station with this ID does not exist."
            ),
            @APIResponse(responseCode = "500",
                    description = "Unknown error while booking."
            ),
            @APIResponse(responseCode = "200",
                    description = "Booking succesful, returns a transaction confirmation.",
                    content = @Content(
                            schema = @Schema(implementation = Racun.class))),
    })
    /** Create new racun **/
    @POST
    @Path("/rezerviraj")
    @Produces("application/json")
    public Response createRacun_(@RequestBody(
            description = "Body containing IP-s to users and charging stations microservices as u_host, p_host and the transaction as racun",
            required = true) String body) {
        System.out.println(body);

        if (body.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("No POST body found.").build();
        }
        JSONObject bodyObject = new JSONObject(body);

        String u_host = null;
        String p_host = null;
        Racun r = new Racun();
        try {
            u_host = bodyObject.getString("u_host");
            p_host = bodyObject.getString("p_host");
            if (u_host.isEmpty() || p_host.isEmpty()) {
                throw new JsonProcessingException("Error while aprsing u_host or p_host"){};
            }
            JSONObject racunObject = bodyObject.getJSONObject("racun");

            r.setCustomerId(racunObject.getInt("customerId"));
            r.setCustomerUsername(racunObject.getString("customerUsername"));
            r.setCustomerEmail(racunObject.getString("customerEmail"));
            r.setCustomerFirstName(racunObject.getString("customerFirstName"));
            r.setCustomerLastName(racunObject.getString("customerLastName"));
            r.setPolnilnicaId(racunObject.getInt("polnilnicaId"));
            r.setPrice(racunObject.getFloat("price"));
            r.setTerminDateTo(racunObject.getLong("terminDateTo"));
            r.setTerminDateFrom(racunObject.getLong("terminDateFrom"));
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Wrong structure for POST body.").build();
        }
        // check body
        if (r.getCustomerId() == null || r.getCustomerUsername().isEmpty() || r.getCustomerEmail().isEmpty()
                || r.getCustomerFirstName().isEmpty() || r.getCustomerLastName().isEmpty()
                || r.getPolnilnicaId() == null || r.getTerminDateFrom() == null || r.getTerminDateTo() == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("customerId, customerUsername, customerEmail, customerFirstName, customerLastName, terminId, terminDateFrom, terminDateTo are necessary parameters").build();
        }

        if (r.getTerminDateTo() - r.getTerminDateFrom() <= 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Impossible time bracket provided.").build();
        }

        uporabniki_host =  Optional.of(u_host);
        polnilnice_host =  Optional.of(p_host);

        /** Call polnilnica MS and check if termin is actually available  **/
        // Get list of termini for polnilnica with provided id
        String polnilnicaTerminiString = myHttpGet(polnilnice_host.get() + "/v1/polnilnice/" + r.getPolnilnicaId() + "/termini");
        // Map json string into Termin list
        List<Termin> terminList = null;
        try {
            terminList =  mapper.readValue(polnilnicaTerminiString, new TypeReference<List<Termin>>() {});
        } catch (JsonProcessingException e) {
            // Didn't receive list of termini, check if polnilnica exists
            if (polnilnicaTerminiString.startsWith("Polnilnica with id")) {
                return Response.status(Response.Status.NOT_FOUND).entity(polnilnicaTerminiString).build();
            }
            // If polnilnica doesn't exist and the service didn't say the termini list is empty, something went wrong there
            if (!polnilnicaTerminiString.startsWith("No termini found")) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(polnilnicaTerminiString).build();
            }
        }
        // Check for overlapping if we received a list of termini
        if (terminList != null && !terminList.isEmpty()) {
            for (int i = 0; i < terminList.size(); i++) {
                Long listTerminFrom = terminList.get(i).getDateFrom();
                Long listTerminTo = terminList.get(i).getDateTo();

                if (r.getTerminDateFrom() <= listTerminTo && listTerminFrom <= r.getTerminDateTo()) {

                    return Response.status(Response.Status.BAD_REQUEST).entity("Your timeframe overlaps with termin with id " + terminList.get(i).getId() + ".").build();
                }
            }
        }
        // No overlapping found

        /** Call user MS and check if user has adequate funds **/
        String uporabnikiString = myHttpGet(uporabniki_host.get() + "/v1/uporabniki/" + r.getCustomerId());
        Uporabnik uporabnik = null;
        try {
            uporabnik =  mapper.readValue(uporabnikiString, Uporabnik.class);
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Uporabnik with id " + r.getCustomerId() + " does not exist.").build();
        }
        // Check his balance
        if (pricePerHour.getPricePerHour() == null ) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Price per hour is not available and we cannot book. Sorry.").build();
        }
        Long diff = r.getTerminDateTo() - r.getTerminDateFrom();
        Float wtf = (diff.floatValue() / 3600f);
        Float terminPrice = wtf * pricePerHour.getPricePerHour();
        System.out.println("wtf: " + wtf);
        System.out.println("termin price: " + terminPrice);
        if (uporabnik.getFunds() < terminPrice) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Not enough funds.").build();
        }

        /** Call polnilnica MS and book the termin **/
        // Create json body for POST call
        JSONObject jsonBody = new JSONObject();
        jsonBody.put("userId", r.getCustomerId());
        jsonBody.put("dateFrom", r.getTerminDateFrom());
        jsonBody.put("dateTo", r.getTerminDateTo());
        // POST the termin to polnilnica api
        String polnilnicaPostResponse = myHttpPost(polnilnice_host.get() + "/v1/polnilnice/" + r.getPolnilnicaId() + "/termini", jsonBody.toString());
        // If everything went ok, we get the termin as response
        Termin termin = null;
        try {
            termin =  mapper.readValue(polnilnicaPostResponse, Termin.class);
        } catch (JsonProcessingException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong while booking.").build();
        }
        /** Call uporabniki MS and substract funds **/
        uporabnik.setFunds(uporabnik.getFunds() - terminPrice);
        String putBodyString = null;
        try {
            putBodyString = mapper.writeValueAsString(uporabnik);
        } catch (JsonProcessingException e) {
            myHttpsDelete(polnilnice_host.get() + "/v1/termini/" + termin.getId());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong while updating user value.").build();
        }
        String uporabnikPutResponse = myHttpPut(uporabniki_host.get() + "/v1/uporabniki/" + r.getCustomerId(), putBodyString);
        try {
            uporabnikPutResponse = mapper.writeValueAsString(uporabnik);
        } catch (JsonProcessingException e) {
            myHttpsDelete(polnilnice_host.get() + "/v1/termini/" + termin.getId());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Something went wrong while updating user funds.").build();
        }

        r.setStatus("payment");
        r.setPrice(terminPrice);
        r.setTerminId(termin.getId());
        r.setTimestamp(System.currentTimeMillis() / 1000L);
        Racun racun = racunBean.createRacun(r);

        return Response.status(Response.Status.CREATED).entity(racun).build();

    }



    @GET
    @Path("/test")
    @Produces("application/json")
    public Response getRacunById() {

        return Response.status(Response.Status.OK).entity(polnilnice_host).build();
    }


    /** Odpovej termin **/
    @Operation(description = "Cancel booking and refund (Using service discovery to user and charging stations microservice).", summary = "Cancel booking")
    @APIResponses({
            @APIResponse(responseCode = "404",
                    description = "Transaction for this booking not found. Contact support."
            ),
            @APIResponse(responseCode = "500",
                    description = "Error while parsing user received from user microservice."),
            @APIResponse(responseCode = "200",
                    description = "Cancelation succesful. Returns confirmation transaction.",
                    content = @Content(
                        schema = @Schema(implementation = Racun.class))),
    })

    @POST
    @Path("/odpovej/{terminId}")
    @Produces("application/json")
    public Response odpovejTermin(@PathParam("terminId") Integer terminId) {

        Racun er = racunBean.getRacunByTerminId(terminId);
        if (er == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Racun for this termin does not exist. Please contact support").build();
        }


        String uporabnikiString = myHttpGet(uporabniki_host.get() + "/v1/uporabniki/" + er.getCustomerId());
        Uporabnik uporabnik = null;
        try {
            uporabnik =  mapper.readValue(uporabnikiString, Uporabnik.class);
        } catch (JsonProcessingException e) {}

        if (uporabnik != null) {
            uporabnik.setFunds(uporabnik.getFunds() + er.getPrice());
            String putBodyString = null;
            try {
                putBodyString = mapper.writeValueAsString(uporabnik);
            } catch (JsonProcessingException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Problem while parsing user.").build();
            }
            myHttpPut(uporabniki_host.get() + "/v1/uporabniki/" + er.getCustomerId(), putBodyString);
        }

        myHttpsDelete(polnilnice_host.get() + "/v1/termini/" + er.getTerminId());

        System.out.println(er.getCustomerEmail());
        System.out.println(er.getCustomerFirstName());

        RacunEntity newRacun = RacunConverter.toEntity(er);
        Racun actuallyRacun = RacunConverter.toDto(newRacun);

        System.out.println(actuallyRacun.getCustomerEmail());
        System.out.println(actuallyRacun.getCustomerFirstName());
        actuallyRacun.setTimestamp(System.currentTimeMillis() / 1000L);
        actuallyRacun.setStatus("cancelled");



        Racun racun = racunBean.createRacun(actuallyRacun);
        return Response.status(Response.Status.CREATED).entity(racun).build();

    }

    @Operation(description = "Cancel booking and refund (Using service discovery to user and charging stations microservice).", summary = "Cancel booking")
    @APIResponses({
            @APIResponse(responseCode = "500",
                    description = "Error while parsing user received from user microservice."),
            @APIResponse(responseCode = "404",
                    description = "User with provided id not found"),
            @APIResponse(responseCode = "200",
                    description = "Transaction succesful, funds added. Returns confirmation transaction.",
                    content = @Content(
                            schema = @Schema(implementation = Racun.class))),
    })
    @POST
    @Path("/nakazi_/{uporabnik_id}")
    @Produces("application/json")
    public Response nakazi_(@PathParam("uporabnik_id") Integer uporabnikId,@RequestBody(
            description = "Body containing added funs as 'nakazilo' and IP of users microservice as 'u_host'",
            required = true) String body) {
        if (body.isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("No POST body found.").build();
        }
        JSONObject bodyObject = new JSONObject(body);

        Float nakazilo = bodyObject.getFloat("nakazilo");
        String u_host = bodyObject.getString("u_host");


        if (nakazilo == null || nakazilo.isNaN()) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Problem while parsing body.").build();
        }

        uporabniki_host = Optional.of(u_host);

        String uporabnikiString = myHttpGet(uporabniki_host.get() + "/v1/uporabniki/" + uporabnikId);
        System.out.println(uporabnikiString);
        Uporabnik uporabnik = null;
        try {
            uporabnik =  mapper.readValue(uporabnikiString, Uporabnik.class);
        } catch (JsonProcessingException e) {
            System.out.println(e.getMessage());
        }

        if (uporabnik != null) {
            uporabnik.setFunds(uporabnik.getFunds() + nakazilo);
            String putBodyString = null;
            try {
                putBodyString = mapper.writeValueAsString(uporabnik);
            } catch (JsonProcessingException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Problem while parsing user.").build();
            }
            myHttpPut(uporabniki_host.get() + "/v1/uporabniki/" + uporabnikId, putBodyString);
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("User with id " + uporabnikId + " was not found.").build();
        }
        Racun r = new Racun();
        r.setStatus("transfer");
        r.setCustomerFirstName(uporabnik.getFirstName());
        r.setCustomerLastName(uporabnik.getLastName());
        r.setTimestamp(System.currentTimeMillis() / 1000L);
        r.setPrice(nakazilo);
        r.setPolnilnicaId(-1);
        r.setTerminId(-1);
        r.setTerminDateTo((long) -1);
        r.setTerminDateFrom((long) -1);
        r.setCustomerEmail(uporabnik.getEmail());
        r.setCustomerId(uporabnik.getId());
        r.setCustomerUsername(uporabnik.getUsername());

        Racun racun = racunBean.createRacun(r);
        return Response.status(Response.Status.CREATED).entity(racun).build();

    }



    /** Create new racun **/
    @POST
    @Path("/odpovej_/{terminId}/uporabniki_host={u_host},polnilnice_host={p_host}")
    @Produces("application/json")
    public Response odpovejTermin_(@PathParam("terminId") Integer terminId, @PathParam("u_host") String u_host, @PathParam("p_host") String p_host) {
        uporabniki_host = Optional.of(u_host);
        polnilnice_host = Optional.of(p_host);

        Racun er = racunBean.getRacunByTerminId(terminId);
        if (er == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Racun for this termin does not exist. Please contact support").build();
        }


        String uporabnikiString = myHttpGet(uporabniki_host.get() + "/v1/uporabniki/" + er.getCustomerId());
        Uporabnik uporabnik = null;
        try {
            uporabnik =  mapper.readValue(uporabnikiString, Uporabnik.class);
        } catch (JsonProcessingException e) {}

        if (uporabnik != null) {
            uporabnik.setFunds(uporabnik.getFunds() + er.getPrice());
            String putBodyString = null;
            try {
                putBodyString = mapper.writeValueAsString(uporabnik);
            } catch (JsonProcessingException e) {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Problem while parsing user.").build();
            }
            myHttpPut(uporabniki_host.get() + "/v1/uporabniki/" + er.getCustomerId(), putBodyString);
        }

        myHttpsDelete(polnilnice_host.get() + "/v1/termini/" + er.getTerminId());

        System.out.println(er.getCustomerEmail());
        System.out.println(er.getCustomerFirstName());

        RacunEntity newRacun = RacunConverter.toEntity(er);
        Racun actuallyRacun = RacunConverter.toDto(newRacun);

        System.out.println(actuallyRacun.getCustomerEmail());
        System.out.println(actuallyRacun.getCustomerFirstName());
        actuallyRacun.setTimestamp(System.currentTimeMillis() / 1000L);
        actuallyRacun.setStatus("cancelled");



        Racun racun = racunBean.createRacun(actuallyRacun);
        return Response.status(Response.Status.CREATED).entity(racun).build();

    }

    private String myHttpsDelete(String url) {
        HttpDelete req = new HttpDelete(url);
        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(req);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException | IllegalArgumentException e) {
            return  e.getMessage();
        }
    }

    private String myHttpPut(String url, String jsonbody) {
        HttpPut request = new HttpPut(url);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        CloseableHttpResponse response = null;
        System.out.println(jsonbody);
        try {
            request.setEntity(new StringEntity(jsonbody));
            System.out.println(request.getEntity().toString());
        } catch (UnsupportedEncodingException e) {
            return e.getMessage();
        }
        try {
            response = httpClient.execute(request);
            //HttpEntity response_entity = response.getEntity();
            return EntityUtils.toString(response.getEntity());
        } catch (IOException | IllegalArgumentException e) {
            return  e.getMessage();
        }
    }

    private String myHttpPost(String url, String jsonbody) {
        HttpPost request = new HttpPost(url);
        request.setHeader("Accept", "application/json");
        request.setHeader("Content-type", "application/json");
        CloseableHttpResponse response = null;
        try {
            request.setEntity(new StringEntity(jsonbody));
        } catch (UnsupportedEncodingException e) {
            return e.getMessage();
        }
        try {
            response = httpClient.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException | IllegalArgumentException e) {
            return  e.getMessage();
        }

    }

    private String myHttpGet(String url) {
        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = null;

        try {
            response = httpClient.execute(request);
            return EntityUtils.toString(response.getEntity());
        } catch (IOException e) {
            return  e.getMessage();
        }
    }




}

