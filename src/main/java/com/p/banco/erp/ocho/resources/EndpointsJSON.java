/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.p.banco.erp.ocho.resources;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Path("json")

public class EndpointsJSON {
    private Connection connect() throws SQLException {
        return DriverManager.getConnection("jdbc:mysql://localhost/bar", "root", "");
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getJsonInfo() {
        String message = "Consumo y Produccion de JSON";
        return Response.ok(message).build();
    }

    @GET
    @Path("all_cuentasysaldo")  
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllAccounts() {
        List<Account> accounts = new ArrayList<>();
        String sql = "SELECT * FROM account_balance"; 

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Account account = new Account();
                account.setAccid(rs.getString("accid"));
                account.setBalance(rs.getDouble("balance"));
                accounts.add(account);
            }
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.ok(accounts).build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccount(Account account) {
        String sql = "INSERT INTO account_balance (accid, balance) VALUES (?, ?)";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, account.getAccid());
            stmt.setDouble(2, account.getBalance());
            stmt.executeUpdate();
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.status(Response.Status.CREATED).entity(account).build();
    }

    @GET
    @Path("consultar/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccount(@PathParam("id") String id) {
        Account account = new Account();
        String sql = "SELECT * FROM account_balance WHERE accid = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                account = new Account();
                account.setAccid(rs.getString("accid"));
                account.setBalance(rs.getDouble("balance"));
            } /*else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }*/
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.ok(account).build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateAccount(@PathParam("id") String id, Account account) {
        String sql = "UPDATE account_balance SET balance = ? WHERE accid = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setDouble(1, account.getBalance());
            stmt.setString(2, id);
            int rowsUpdated = stmt.executeUpdate();

            if (rowsUpdated == 0) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.noContent().build();
    }

    @DELETE
    @Path("{id}")
    public Response deleteAccount(@PathParam("id") String id) {
        String sql = "DELETE FROM account_balance WHERE accid = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted == 0) {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }

        return Response.noContent().build();
    }

    // Clase interna para representar la entidad Account
    public static class Account {
        private String accid;
        private double balance;

        public String getAccid() {
            return accid;
        }

 public void setAccid(String accid) {
            this.accid = accid;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }
    }
} 
