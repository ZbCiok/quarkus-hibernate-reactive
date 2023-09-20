package org.acme;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;

import org.hibernate.reactive.mutiny.Mutiny;
import org.jboss.resteasy.reactive.RestPath;

import java.util.List;

import static jakarta.ws.rs.core.Response.Status.*;
import org.jboss.logging.Logger;

@Path("organizations")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class OrganizationResource {

    private static final Logger LOGGER = Logger.getLogger(OrganizationResource.class);

    @Inject
    Mutiny.SessionFactory sf;


    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from RESTEasy Reactive";
    }

    @GET
    @Path("/getAll")
    public Uni<List<Organization>> get() {
        return sf.withTransaction((s, t) -> s
                .createNamedQuery("Organizations.findAll", Organization.class)
                .getResultList()
        );
    }

    @GET
    @Path("/get/{id}")
    public Uni<Organization> getSingle(@RestPath Integer id) {
        if ( id == null ) throw new IllegalArgumentException();
        return sf.withTransaction((s, t) -> s.find(Organization.class, id));
    }

    @POST
    @Path("/save")
    public Uni<Response> create(Organization organization) {
        if (organization == null || organization.getId() != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

        return sf.withTransaction((s, t) -> s.persist(organization))
                .replaceWith(() -> Response.ok(organization).status(CREATED).build());
    }

    @PUT
    @Path("/update/{id}")
    public Uni<Response> update(@RestPath Integer id, Organization organization) {
        if (organization == null || organization.getName() == null) {
            throw new WebApplicationException("Fruit name was not set on request.", 422);
        }

        return sf.withTransaction((s, t) -> s.find(Organization.class, id)
                // If entity exists then update it
                .onItem().ifNotNull().invoke(entity -> entity.setName(organization.getName()))
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                // If entity not found return the appropriate response
                .onItem().ifNull()
                .continueWith(() -> Response.ok().status(NOT_FOUND).build())
        );
    }

    @DELETE
    @Path("/delete/{id}")
    public Uni<Response> delete(@RestPath Integer id) {
        return sf.withTransaction((s, t) ->
                s.find(Organization.class, id)
                        // If entity exists then delete it
                        .onItem().ifNotNull()
                        .transformToUni(entity -> s.remove(entity)
                                .replaceWith(() -> Response.ok().status(NO_CONTENT).build()))
                        // If entity not found return the appropriate response
                        .onItem().ifNull().continueWith(() -> Response.ok().status(NOT_FOUND).build()));
    }
}