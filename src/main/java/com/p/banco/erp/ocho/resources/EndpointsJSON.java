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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
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
    @Path("get/saldo/todo")  
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSaldoTodo() {
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
    @Path("get/saldo/{accid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSaldoAccid(@PathParam("accid") String accid) {
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

    // Endpoint para registrar una nueva cuenta
    @POST
    @Path("register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerAccount(Account account) {
        try (Connection conn = connect()) {
            // Verificar si el accid ya existe
            PreparedStatement checkStmt = conn.prepareStatement("SELECT COUNT(*) FROM login WHERE accid = ?");
            checkStmt.setString(1, account.getAccid());
            ResultSet rs = checkStmt.executeQuery();
            rs.next();
            int count = rs.getInt(1);

            if (count > 0) {
                // Si el accid ya existe, devolver un error
                return Response.status(Response.Status.CONFLICT).entity("El ID de cuenta ya existe.").build();
            }

            // Si no existe, proceder con la inserción
            // Insertar en la tabla login
            PreparedStatement insertLoginStmt = conn.prepareStatement("INSERT INTO login (accid, accpass) VALUES (?, ?)");
            insertLoginStmt.setString(1, account.getAccid());
            insertLoginStmt.setString(2, account.getAccpass());
            insertLoginStmt.executeUpdate();

            // Insertar en la tabla account_balance con saldo inicial de 0
            PreparedStatement insertBalanceStmt = conn.prepareStatement("INSERT INTO account_balance (accid, balance) VALUES (?, ?)");
            insertBalanceStmt.setString(1, account.getAccid());
            insertBalanceStmt.setDouble(2, 0.00);
            insertBalanceStmt.executeUpdate();

            // Registrar la transacción en la tabla transactions
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String date = dateFormatter.format(now);
            String hour = timeFormatter.format(now);

            PreparedStatement insertTransactionStmt = conn.prepareStatement(
                    "INSERT INTO transactions (accid, date, hour, mount, type) VALUES (?, ?, ?, ?, ?)");
            insertTransactionStmt.setString(1, account.getAccid());
            insertTransactionStmt.setString(2, date);
            insertTransactionStmt.setString(3, hour);
            insertTransactionStmt.setDouble(4, 0.00); // Monto de 0.00
            insertTransactionStmt.setString(5, "register");
            insertTransactionStmt.executeUpdate();

            // Devolver una respuesta exitosa
            return Response.ok("Registro realizado exitosamente.").build();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en la base de datos: " + ex.getMessage()).build();
        }
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

        public String getAccpass() {
            return accpass;
        }

        public void setAccpass(String accpass) {
            this.accpass = accpass;
        }

        public double getBalance() {
            return balance;
        }

        public void setBalance(double balance) {
            this.balance = balance;
        }
    }

    // Método para iniciar sesión
    @POST
    @Path("login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response loginUser (UserCredentials credentials) {
        try (Connection conn = connect()) {
            PreparedStatement pst = conn.prepareStatement("SELECT * FROM login WHERE accid = ? AND accpass = ?");
            pst.setString(1, credentials.getAccid());
            pst.setString(2, credentials.getAccpass());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                // Si las credenciales son válidas, devolver el accid
                String accid = rs.getString("accid");
                return Response.ok().entity("{\"message\": \"Inicio de sesión exitoso.\", \"accid\": \"" + accid + "\"}").build();
            } else {
                return Response.status(Response.Status.UNAUTHORIZED).entity("ID de cuenta y/o clave no válidos.").build();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en la base de datos: " + ex.getMessage()).build();
        }
    }

    // Obtener información de la cuenta
    @GET
    @Path("account/{accid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccountInfo(@PathParam("accid") String accid) {
        double totalBalance = 0.0;

        try (Connection conn = connect()) {
            PreparedStatement pst = conn.prepareStatement("SELECT balance FROM account_balance WHERE accid = ?");
            pst.setString(1, accid);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                totalBalance = rs.getDouble("balance");
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("No se encontró información de saldo para el ID de cuenta proporcionado.").build();
            }

            return Response.ok("{\"accid\": \"" + accid + "\", \"totalBalance\": " + totalBalance + "}").build();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en la base de datos: " + ex.getMessage()).build();
        }
    }

    // Realizar un depósito
    @POST
    @Path("deposit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deposit(DepositRequest depositRequest) {
        double monto = depositRequest.getMonto();
        String accid = depositRequest.getAccid();

        if (monto < 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("El valor debe ser mayor o igual que 0.").build();
        }

        try (Connection conn = connect()) {
            // Actualizar el saldo
            PreparedStatement pst = conn.prepareStatement("UPDATE account_balance SET balance = balance + ? WHERE accid = ?");
            pst.setDouble(1, monto);
            pst.setString(2, accid);
            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                // Registrar la transacción
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                PreparedStatement logPst = conn.prepareStatement("INSERT INTO transaction_history (accid, amount, transaction_type, date, time) VALUES (?, ?, ?, ?, ?)");
                logPst.setString(1, accid);
                logPst.setDouble(2, monto);
                logPst.setString(3, "Depósito");
                logPst.setString(4, now.format(dateFormatter));
                logPst.setString(5, now.format(timeFormatter));
                logPst.executeUpdate();

                return Response.ok("{\"message\": \"Depósito realizado con éxito.\"}").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("No se encontró la cuenta para el ID proporcionado.").build();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en la base de datos: " + ex.getMessage()).build();
        }
    }

    // Realizar un cargo
    @POST
    @Path("charge")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response charge(ChargeRequest chargeRequest) {
        double monto = chargeRequest.getMonto();
        String accid = chargeRequest.getAccid();

        if (monto < 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("El valor debe ser mayor o igual que 0.").build();
        }

        try (Connection conn = connect()) {
            // Verificar el saldo antes de realizar el cargo
            PreparedStatement balancePst = conn.prepareStatement("SELECT balance FROM account_balance WHERE accid = ?");
            balancePst.setString(1, accid);
            ResultSet rs = balancePst.executeQuery();

            if (rs.next()) {
                double currentBalance = rs.getDouble("balance");
                if (currentBalance < monto) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("Saldo insuficiente para realizar el cargo.").build();
                }

                // Actualizar el saldo
                PreparedStatement pst = conn.prepareStatement("UPDATE account_balance SET balance = balance - ? WHERE accid = ?");
                pst.setDouble(1, monto);
                pst.setString(2, accid);
                pst.executeUpdate();

                // Registrar la transacción
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                PreparedStatement logPst = conn.prepareStatement("INSERT INTO transaction_history (accid, amount, transaction_type, date, time) VALUES (?, ?, ?, ?, ?)");
                logPst.setString(1, accid);
                logPst.setDouble(2, monto);
                logPst.setString(3, "Cargo");
                logPst.setString(4, now.format(dateFormatter));
                logPst.setString(5, now.format(timeFormatter));
                logPst.executeUpdate();

                return Response.ok("{\"message\": \"Cargo realizado con éxito.\"}").build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("No se encontró la cuenta para el ID proporcionado.").build();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en la base de datos: " + ex.getMessage()).build();
        }
    }

    // Obtener historial de transacciones
    @GET
    @Path("transactions/{accid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTransactionHistory(@PathParam("accid") String accid) {
        List<Transaction> transactions = new ArrayList<>();

        try (Connection conn = connect()) {
            PreparedStatement pst = conn.prepareStatement("SELECT * FROM transaction_history WHERE accid = ? ORDER BY date DESC, time DESC");
            pst.setString(1, accid);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                Transaction transaction = new Transaction();
                transaction.setAccid(rs.getString("accid"));
                transaction.setAmount(rs.getDouble("amount"));
                transaction.setTransactionType(rs.getString("transaction_type"));
                transaction.setDate(rs.getString("date"));
                transaction.setTime(rs.getString("time"));
                transactions.add(transaction);
            }

            return Response.ok(transactions).build();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en la base de datos: " + ex.getMessage()).build();
        }
    }

    // Clases internas para representar las solicitudes y transacciones
    public static class UserCredentials {
        private String accid;
 private String accpass;

        public String getAccid() {
            return accid;
        }

        public void setAccid(String accid) {
            this.accid = accid;
        }

        public String getAccpass() {
            return accpass;
        }

        public void setAccpass(String accpass) {
            this.accpass = accpass;
        }
    }

    public static class DepositRequest {
        private String accid;
        private double monto;

        public String getAccid() {
            return accid;
        }

        public void setAccid(String accid) {
            this.accid = accid;
        }

        public double getMonto() {
            return monto;
        }

        public void setMonto(double monto) {
            this.monto = monto;
        }
    }

    public static class ChargeRequest {
        private String accid;
        private double monto;

        public String getAccid() {
            return accid;
        }

        public void setAccid(String accid) {
            this.accid = accid;
        }

        public double getMonto() {
            return monto;
        }

        public void setMonto(double monto) {
            this.monto = monto;
        }
    }

    public static class Transaction {
        private String accid;
        private double amount;
        private String transactionType;
        private String date;
        private String time;

        public String getAccid() {
            return accid;
        }

        public void setAccid(String accid) {
            this.accid = accid;
        }

        public double getAmount() {
            return amount;
        }

        public void setAmount(double amount) {
            this.amount = amount;
        }

        public String getTransactionType() {
            return transactionType;
        }

        public void setTransactionType(String transactionType) {
            this.transactionType = transactionType;
        }

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}