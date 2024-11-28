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
        try {
            // Cargar el controlador JDBC para MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se pudo encontrar el controlador JDBC para MySQL.", e);
        }
    
        // Establecer la conexión con la base de datos
        return DriverManager.getConnection("jdbc:mysql://localhost/bar", "root", "");
    }

// Texto Plano    
    
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response json() {
        String message = "Consumo y Produccion de JSON";
        return Response.ok(message).build();
    }

//TODO GET   
    
    @GET
    @Path("get/todo/saldos")  
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTodoSaldos() {
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

//INDIVIDUAL GET

    @GET
    @Path("get/individual/{accid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getIndividualAccid(@PathParam("accid") String accid) {
        Account account = new Account();
        String sql = "SELECT * FROM account_balance WHERE accid = ?";

        try (Connection conn = connect();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, accid);
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

    // Clase interna para representar la entidad Account
    public static class Account {
        private String accid;
        private String accpass;
        private double balance;

        public String getAccid() {
            return accid;
        }

 public void setAccid(String accid) {
            this.accid = accid;
        }

  public void setAccpass(String accid) {
            this.accpass = accpass;
        }
 
        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }
    }
} 
