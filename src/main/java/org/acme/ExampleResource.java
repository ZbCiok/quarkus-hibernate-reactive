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

@Path("customers")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
public class ExampleResource {

    private static final Logger LOGGER = Logger.getLogger(ExampleResource.class);

    @Inject
    Mutiny.SessionFactory sf;


    @GET
    @Path("/hello")
    @Produces(MediaType.TEXT_PLAIN)
    public String hello() {
        return "Hello from RESTEasy Reactive";
    }


    @GET
    public Uni<List<Customer>> get() {
        return sf.withTransaction((s, t) -> s
                .createNamedQuery("Customers.findAll", Customer.class)
                .getResultList()
        );
    }

    @GET
    @Path("{id}")
    public Uni<Customer> getSingle(@RestPath Integer id) {
        return sf.withTransaction((s, t) -> s.find(Customer.class, id));
    }

    @POST
    public Uni<Response> create(Customer customer) {
        if (customer == null || customer.getId() != null) {
            throw new WebApplicationException("Id was invalidly set on request.", 422);
        }

        return sf.withTransaction((s, t) -> s.persist(customer))
                .replaceWith(() -> Response.ok(customer).status(CREATED).build());
    }

    @PUT
    @Path("{id}")
    public Uni<Response> update(@RestPath Integer id, Customer customer) {
        if (customer == null || customer.getName() == null) {
            throw new WebApplicationException("Fruit name was not set on request.", 422);
        }

        return sf.withTransaction((s, t) -> s.find(Customer.class, id)
                // If entity exists then update it
                .onItem().ifNotNull().invoke(entity -> entity.setName(customer.getName()))
                .onItem().ifNotNull().transform(entity -> Response.ok(entity).build())
                // If entity not found return the appropriate response
                .onItem().ifNull()
                .continueWith(() -> Response.ok().status(NOT_FOUND).build())
        );
    }

    @DELETE
    @Path("{id}")
    public Uni<Response> delete(@RestPath Integer id) {
        return sf.withTransaction((s, t) ->
                s.find(Customer.class, id)
                        // If entity exists then delete it
                        .onItem().ifNotNull()
                        .transformToUni(entity -> s.remove(entity)
                                .replaceWith(() -> Response.ok().status(NO_CONTENT).build()))
                        // If entity not found return the appropriate response
                        .onItem().ifNull().continueWith(() -> Response.ok().status(NOT_FOUND).build()));
    }
}