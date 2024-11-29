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

//TODO SALDOS
    
    @GET
    @Path("/balances")  
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBalances() {
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

//TODO Transacciones
    
@GET
@Path("/transactions")  
@Produces(MediaType.APPLICATION_JSON)
public Response getTransactions() {
    List<Transaction> transactions = new ArrayList<>();
    String sql = "SELECT * FROM transactions"; 

    try (Connection conn = connect();
         PreparedStatement stmt = conn.prepareStatement(sql);
         ResultSet rs = stmt.executeQuery()) {

        while (rs.next()) {
            Transaction transaction = new Transaction();
            transaction.setAccid(rs.getString("accid")); 
            transaction.setDate(rs.getString("date")); 
            transaction.setHour(rs.getString("hour")); 
            transaction.setMount(rs.getDouble("mount")); 
            transaction.setType(rs.getString("type")); 
            transactions.add(transaction);
        }
    } catch (SQLException e) {
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
    }

    return Response.ok(transactions).build();
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
            insertBalanceStmt.setDouble(2, 0);
            insertBalanceStmt.executeUpdate();

            // Registrar la transacción en la tabla de historial
            PreparedStatement insertTransactionStmt = conn.prepareStatement("INSERT INTO transactions (accid, date, hour, mount, type) VALUES (?, ?, ?, ?, ?)");
            insertTransactionStmt.setString(1, account.getAccid());
            insertTransactionStmt.setString(2, LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            insertTransactionStmt.setString(3, LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            insertTransactionStmt.setDouble(4, 0);
            insertTransactionStmt.setString(5, "register");
            insertTransactionStmt.executeUpdate();

            return Response.status(Response.Status.CREATED).entity("Registro realizado exitosamente.").build();
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    // Endpoint para obtener la información de la cuenta (saldo y transacciones)
    @GET
    @Path("/{accid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccidSaldo(@PathParam("accid") String accid) {
        AccidSaldo accidSaldo = new AccidSaldo();
        try (Connection conn = connect()) {
            // Obtener el saldo total
            PreparedStatement balanceStmt = conn.prepareStatement("SELECT balance FROM account_balance WHERE accid = ?");
            balanceStmt.setString(1, accid);
            ResultSet balanceRs = balanceStmt.executeQuery();

            if (balanceRs.next()) {
                accidSaldo.setAccid(accid);
                accidSaldo.setBalance(balanceRs.getDouble("balance"));
            } else {
                return Response.status(Response.Status.NOT_FOUND).entity("Cuenta no encontrada.").build();
            }

            // Obtener el historial de transacciones
            List<Transaction> transactions = new ArrayList<>();
            PreparedStatement transactionStmt = conn.prepareStatement("SELECT * FROM transactions WHERE accid = ?");
            transactionStmt.setString(1, accid);
            ResultSet transactionRs = transactionStmt.executeQuery();

            while (transactionRs.next()) {
                Transaction transaction = new Transaction();
                transaction.setDate(transactionRs.getString("date"));
                transaction.setHour(transactionRs.getString("hour"));
                transaction.setMount(transactionRs.getDouble("mount"));
                transaction.setType(transactionRs.getString("type"));
                transactions.add(transaction);
            }
            accidSaldo.setTransactions(transactions);

            return Response.ok(accidSaldo).build();
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    // Clase para representar la información de la cuenta
    public static class AccidSaldo {
        private String accid;
        private double balance;
        private List<Transaction> transactions;

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

        public List<Transaction> getTransactions() {
            return transactions;
        }

        public void setTransactions(List<Transaction> transactions) {
            this.transactions = transactions;
        }
    }

    // Clase para representar una transacción
    public static class Transaction {
        private String accid;
        private String date;
        private String hour;
        private double mount;
        private String type;

        public String getAccid() {
            return accid;
        }
    
        public void setAccid(String accid) {
            this.accid = accid;
        }        

        public String getDate() {
            return date;
        }

        public void setDate(String date) {
            this.date = date;
        }

        public String getHour() {
            return hour;
        }

        public void setHour(String hour) {
            this.hour = hour;
        }

        public double getMount() {
            return mount;
        }

        public void setMount(double mount) {
            this.mount = mount;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }

    // Clase para representar una cuenta
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

    // Endpoint para realizar un depósito
    @POST
    @Path("deposit")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deposit(DepositRequest depositRequest) {
        // Validar que el monto sea un número mayor o igual a 0
        if (depositRequest.getMount() < 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("El monto debe ser mayor o igual a 0.").build();
        }

        try (Connection conn = connect()) {
            // Verificar que el accid y accpass sean correctos
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM login WHERE accid = ? AND accpass = ?");
            checkStmt.setString(1, depositRequest.getAccid());
            checkStmt.setString(2, depositRequest.getAccpass());
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("ID de cuenta o clave incorrectos.").build();
            }

            // Actualizar el saldo en account_balance
            String updateBalanceSql = "UPDATE account_balance SET balance = balance + ? WHERE accid = ?";
            try (PreparedStatement updateBalanceStmt = conn.prepareStatement(updateBalanceSql)) {
                updateBalanceStmt.setDouble(1, depositRequest.getMount());
                updateBalanceStmt.setString(2, depositRequest.getAccid());
                updateBalanceStmt.executeUpdate();
            }

            // Registrar la transacción en transactions
            String insertTransactionSql = "INSERT INTO transactions (accid, date, hour, mount, type) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertTransactionStmt = conn.prepareStatement(insertTransactionSql)) {
                LocalDateTime now = LocalDateTime.now();
                insertTransactionStmt.setString(1, depositRequest.getAccid());
                insertTransactionStmt.setString(2, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                insertTransactionStmt.setString(3, now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                insertTransactionStmt.setDouble(4, depositRequest.getMount());
                insertTransactionStmt.setString(5, "deposit");
                insertTransactionStmt.executeUpdate();
            }

            return Response.ok("Depósito realizado con éxito.").build();
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    // Clase para representar la solicitud de depósito
    public static class DepositRequest {
        private String accid;
        private String accpass;
        private double mount;

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

        public double getMount() {
            return mount;
        }

        public void setMount(double mount) {
            this.mount = mount;
        }
    }

    // Endpoint para realizar un cargo
    @POST
    @Path("charge")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response charge(ChargeRequest chargeRequest) {
        // Validar que el monto sea un número mayor o igual a 0
        if (chargeRequest.getMount() < 0) {
            return Response.status(Response.Status.BAD_REQUEST).entity("El monto debe ser mayor o igual a 0.").build();
        }

        try (Connection conn = connect()) {
            // Verificar que el accid y accpass sean correctos
            PreparedStatement checkStmt = conn.prepareStatement("SELECT * FROM login WHERE accid = ? AND accpass = ?");
            checkStmt.setString(1, chargeRequest.getAccid());
            checkStmt.setString(2, chargeRequest.getAccpass());
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                return Response.status(Response.Status.UNAUTHORIZED).entity("ID de cuenta o clave incorrectos.").build();
            }

            // Verificar el saldo disponible
            PreparedStatement balanceStmt = conn.prepareStatement("SELECT balance FROM account_balance WHERE accid = ?");
            balanceStmt.setString(1, chargeRequest.getAccid());
            ResultSet balanceRs = balanceStmt.executeQuery();

            if (balanceRs.next()) {
                double currentBalance = balanceRs.getDouble("balance");
                if (currentBalance < chargeRequest.getMount()) {
                    return Response.status(Response.Status.BAD_REQUEST).entity("Saldo insuficiente para realizar el cargo.").build();
                }
            }

            // Actualizar el saldo en account_balance
            String updateBalanceSql = "UPDATE account_balance SET balance = balance - ? WHERE accid = ?";
            try (PreparedStatement updateBalanceStmt = conn.prepareStatement(updateBalanceSql)) {
                updateBalanceStmt.setDouble(1, chargeRequest.getMount());
                updateBalanceStmt.setString(2, chargeRequest.getAccid());
                updateBalanceStmt.executeUpdate();
            }

            // Registrar la transacción en transactions
            String insertTransactionSql = "INSERT INTO transactions (accid, date, hour, mount, type) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement insertTransactionStmt = conn.prepareStatement(insertTransactionSql)) {
                LocalDateTime now = LocalDateTime.now();
                insertTransactionStmt.setString(1, chargeRequest.getAccid());
                insertTransactionStmt.setString(2, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                insertTransactionStmt.setString(3, now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
                insertTransactionStmt.setDouble(4, chargeRequest.getMount());
                insertTransactionStmt.setString(5, "charge");
                insertTransactionStmt.executeUpdate();
            }

            return Response.ok("Cargo realizado con éxito.").build();
        } catch (SQLException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    // Clase para representar la solicitud de cargo
    public static class ChargeRequest {
        private String accid;
        private String accpass;
        private double mount;

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

        public double getMount() {
            return mount;
        }

        public void setMount(double mount) {
            this.mount = mount;
        }
    }

// Endpoint para realizar un depósito y cargo simultáneamente
@POST
@Path("pay")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public Response pay(PayRequest payRequest) {
    // Validar que el monto sea un número mayor o igual a 0
    if (payRequest.getMount() < 0) {
        return Response.status(Response.Status.BAD_REQUEST).entity("El monto debe ser mayor o igual a 0.").build();
    }

    try (Connection conn = connect()) {
        // Verificar que el primer accid y accpass sean correctos
        PreparedStatement checkStmt1 = conn.prepareStatement("SELECT * FROM login WHERE accid = ? AND accpass = ?");
        checkStmt1.setString(1, payRequest.getAccid1());
        checkStmt1.setString(2, payRequest.getAccpass1());
        ResultSet rs1 = checkStmt1.executeQuery();

        if (!rs1.next()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("ID de cuenta o clave incorrectos de ACCID1.").build();
        }

        // Verificar que el segundo accid y accpass sean correctos
        PreparedStatement checkStmt2 = conn.prepareStatement("SELECT * FROM login WHERE accid = ?");
        checkStmt2.setString(1, payRequest.getAccid2());
        ResultSet rs2 = checkStmt2.executeQuery();

        if (!rs2.next()) {
            return Response.status(Response.Status.UNAUTHORIZED).entity("ID de cuenta incorrecto para la segunda cuenta.").build();
        }

        // Verificar el saldo disponible en la cuenta del primer accid
        PreparedStatement balanceStmt = conn.prepareStatement("SELECT balance FROM account_balance WHERE accid = ?");
        balanceStmt.setString(1, payRequest.getAccid1());
        ResultSet balanceRs = balanceStmt.executeQuery();

        if (balanceRs.next()) {
            double currentBalance = balanceRs.getDouble("balance");
            if (currentBalance < payRequest.getMount()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Saldo insuficiente en ACCID1 para realizar el pago hacia ACCID2.").build();
            }
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Cuenta de destino no encontrada.").build();
        }

        // Actualizar el saldo en account_balance para el primer accid
        String updateBalanceSql1 = "UPDATE account_balance SET balance = balance - ? WHERE accid = ?";
        try (PreparedStatement updateBalanceStmt1 = conn.prepareStatement(updateBalanceSql1)) {
            updateBalanceStmt1.setDouble(1, payRequest.getMount());
            updateBalanceStmt1.setString(2, payRequest.getAccid1());
            updateBalanceStmt1.executeUpdate();
        }

        // Registrar la transacción en transactions para el primer accid
        String insertTransactionSql1 = "INSERT INTO transactions (accid, date, hour, mount, type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insertTransactionStmt1 = conn.prepareStatement(insertTransactionSql1)) {
            LocalDateTime now = LocalDateTime.now();
            insertTransactionStmt1.setString(1, payRequest.getAccid1());
            insertTransactionStmt1.setString(2, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            insertTransactionStmt1.setString(3, now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            insertTransactionStmt1.setDouble(4, payRequest.getMount());
            insertTransactionStmt1.setString(5, "charge");
            insertTransactionStmt1.executeUpdate();
        }

        // Actualizar el saldo en account_balance para el segundo accid
        String updateBalanceSql2 = "UPDATE account_balance SET balance = balance + ? WHERE accid = ?";
        try (PreparedStatement updateBalanceStmt2 = conn.prepareStatement(updateBalanceSql2)) {
            updateBalanceStmt2.setDouble(1, payRequest.getMount());
            updateBalanceStmt2.setString(2, payRequest.getAccid2());
            updateBalanceStmt2.executeUpdate();
        }

        // Registrar la transacción en transactions para el segundo accid
        String insertTransactionSql2 = "INSERT INTO transactions (accid, date, hour, mount, type) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement insertTransactionStmt2 = conn.prepareStatement(insertTransactionSql2)) {
            LocalDateTime now = LocalDateTime .now();
            insertTransactionStmt2.setString(1, payRequest.getAccid2());
            insertTransactionStmt2.setString(2, now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
            insertTransactionStmt2.setString(3, now.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            insertTransactionStmt2.setDouble(4, payRequest.getMount());
            insertTransactionStmt2.setString(5, "deposit");
            insertTransactionStmt2.executeUpdate();
        }

        // Confirmar el pago
        return Response.ok("Pago de ACCID1 hacia ACCID2 realizado con éxito.").build();

    } catch (SQLException e) {
        e.printStackTrace();
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error en la base de datos. " + e.getMessage()).build();
    }
}

    // Clase para representar la solicitud de transferencia
    public static class PayRequest {
        private String accid1;
        private String accpass1;
        private String accid2;
        private double mount;

        public String getAccid1() {
            return accid1;
        }

        public void setAccid1(String accid1) {
            this.accid1 = accid1;
        }

        public String getAccpass1() {
            return accpass1;
        }

        public void setAccpass1(String accpass1) {
            this.accpass1 = accpass1;
        }

        public String getAccid2() {
            return accid2;
        }

        public void setAccid2(String accid2) {
            this.accid2 = accid2;
        }

        public double getMount() {
            return mount;
        }

        public void setMount(double mount) {
            this.mount = mount;
        }
    }

}