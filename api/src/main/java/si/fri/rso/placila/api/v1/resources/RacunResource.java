package si.fri.rso.placila.api.v1.resources;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kumuluz.ee.discovery.annotations.DiscoverService;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
@Path("/placila")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
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
    @GET
    @Path("/cenik")
    @Produces("application/json")
    public Response getRacuniByUserId() {
        if (pricePerHour.getPricePerHour() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        return Response.status(Response.Status.OK).entity(pricePerHour.getPricePerHour()).build();
    }

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

    /** Create new racun **/
    @POST
    @Path("/rezerviraj")
    @Produces("application/json")
    public Response createRacun(Racun r) {
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

        //TODO: zakomentiraj ti 2 vrstici in bi moglo uporabit to kar consul najde
        uporabniki_host =  Optional.of("http://192.168.99.100:8080");
        polnilnice_host =  Optional.of("http://192.168.99.100:8081");

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

    /** Create new racun **/
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

