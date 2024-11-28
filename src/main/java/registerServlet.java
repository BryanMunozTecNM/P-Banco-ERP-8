/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */

import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@WebServlet("/registerServlet")
public class registerServlet extends HttpServlet {

    PrintWriter out = null;
    Connection con;
    PreparedStatement pst;    
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            // Obtener los parámetros necesarios para la inserción
            String accid = request.getParameter("accid");
            String accpass = request.getParameter("accpass");

            // Establecer la conexión con la base de datos
            con = DriverManager.getConnection("jdbc:mysql://localhost/bar", "root", "");

            // Verificar si el accid ya existe en la tabla login
            pst = con.prepareStatement("SELECT COUNT(*) FROM login WHERE accid = ?");
            pst.setString(1, accid);
            ResultSet rs = pst.executeQuery();
            rs.next();
            int count = rs.getInt(1);

             if (count > 0) {
                 // Si el accid ya existe, mostrar mensaje de error
                 out = response.getWriter();
                 response.setContentType("text/html");
                 out.println("<html>");
                 out.println("<body bgcolor='#ECF0F1'>");
                 out.println("Error: El ID de cuenta que ingresaste ya está dentro del sistema, ingresa un ID de cuenta diferente");
                 out.println("</body>");
                 out.println("</html>");
            } else {
                // Si no existe, proceder con la inserción
                pst = con.prepareStatement("INSERT INTO login (accid, accpass) VALUES (?, ?)");
                pst.setString(1, accid);
                pst.setString(2, accpass);

                // Ejecutar la inserción
                int rowsAffected = pst.executeUpdate();
                if (rowsAffected > 0) {
                    // Si la inserción fue exitosa, insertar un saldo inicial de 0 en
                    // account_balance
                    pst = con.prepareStatement("INSERT INTO account_balance (accid, balance) VALUES (?, ?)");
                    pst.setString(1, accid);
                    pst.setDouble(2, 0.00); // Saldo inicial de 0
                    int balanceRowsAffected = pst.executeUpdate();

                    if (balanceRowsAffected > 0) {
                        // Registrar la transacción en la tabla de historial
                        pst = con.prepareStatement(
                                "INSERT INTO transactions (accid, date, hour, mount, type) VALUES (?, ?, ?, ?, ?)");
                        LocalDateTime now = LocalDateTime.now();
                        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                        String date = dateFormatter.format(now);
                        String hour = timeFormatter.format(now);
                        pst.setString(1, accid);
                        pst.setString(2, date);
                        pst.setString(3, hour);
                        pst.setDouble(4, 0.00); // Monto de 0.00
                        pst.setString(5, "register"); // Tipo de transacción
                        int transactionRowsAffected = pst.executeUpdate();

                        // Mensaje de éxito
                         if (transactionRowsAffected > 0) {
                             out = response.getWriter();
                             response.setContentType("text/html");
                             out.println("<html>");
                             out.println("<body bgcolor='#ECF0F1'>");
                             out.println("Registro realizado exitosamente.");
                             out.println("</body>");
                             out.println("</html>");
                        } else {
                            // Error al insertar en transactions
                            out = response.getWriter();
                            response.setContentType("text/html");
                            out.println("<html>");
                            out.println("<body bgcolor='#ECF0F1'>");
                            out.println("Error al registrar la transacción.");
                            out.println("</body>");
                            out.println("</html>");
                        }
                    } else {
                        // Error al insertar en account_balance
                        out = response.getWriter();
                        response.setContentType("text/html");
                        out.println("<html>");
                        out.println("<body bgcolor='#ECF0F1'>");
                        out.println("Error al registrar el saldo inicial.");
                        out.println("</body>");
                        out.println("</html>");
                    }
                 } else {
                     // Si no se afectaron filas
                     out = response.getWriter();
                     response.setContentType("text/html");
                     out.println("<html>");
                     out.println("<body bgcolor='#ECF0 F1'>");
                     out.println("Error al registrarse.");
                     out.println("</body>");
                     out.println("</html>");
                 }
            }

            // Cerrar la conexión
            con.close();
        } catch (SQLException ex) {
            ex.printStackTrace();
            out = response.getWriter();
            response.setContentType("text/html");
            out.println("<html>");
            out.println("<body bgcolor='#ECF0F1'>");
            out.println("Error en la base de datos: " + ex.getMessage());
            out.println("</body>");
            out.println("</html>");
        }
    }
}
