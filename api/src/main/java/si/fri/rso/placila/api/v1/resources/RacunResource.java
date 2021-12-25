package si.fri.rso.placila.api.v1.resources;

import org.json.JSONObject;
import si.fri.rso.placila.lib.Racun;
import si.fri.rso.placila.lib.Termin;
import si.fri.rso.placila.services.beans.RacunBean;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@ApplicationScoped
@Path("/placila")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RacunResource {

    @Inject
    private RacunBean racunBean;

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
        //TODO
        /** Call polnilnica MS and check if termin is actually available  **/
        /** Call user MS and check if user has   **/

        r.setStatus("completed");
        Racun racun = racunBean.createRacun(r);
        return Response.status(Response.Status.CREATED).entity(racun).build();

    }

    /** Update racun with id**/
    @PUT
    @Path("/{id}")
    @Produces("application/json")
    public Response updateRacunStatus(@PathParam("id") Integer id, Racun r) {
        String status = r.getStatus();
        if (racunBean.getRacunById(id) == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if(!status.equals("completed") && !status.equals("deleted")) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Only 'completed' and 'deleted' status exist.").build();
        }

        Racun racun = racunBean.changeRacunStatus(id, status);
        return Response.status(Response.Status.OK).entity(racun).build();

    }




}

