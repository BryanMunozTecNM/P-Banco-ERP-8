/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */

import jakarta.servlet.ServletContext;
import java.io.IOException;
import java.io.PrintWriter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/secondservlet")
public class secondservlet extends HttpServlet {

Connection con; // Variable de instancia
    
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        // Llama a doPost para manejar la lógica de mostrar la pantalla de transacciones
        doPost(request, response);
    }


    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        PrintWriter out = response.getWriter();
        response.setContentType("text/html");

        ServletContext context = getServletContext();
        Object obj = context.getAttribute("accid");
        String value = obj.toString();

        double totalBalance = 0.0;

        try {
            con = DriverManager.getConnection("jdbc:mysql://localhost/bar", "root", ""); // Usa la variable de instancia
            PreparedStatement pst = con.prepareStatement("SELECT balance FROM account_balance WHERE accid = ?");
            pst.setString(1, value);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                totalBalance = rs.getDouble("balance");
            } else {
                out.println("Error: No se encontró la cuenta para el balance");
                return;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        out.println("<form action='faces/index.xhtml' method='get'>");
        out.println("<input type='submit' value='Volver a Inicio'>");
        out.println("</form>");

        out.println("<html>");
        out.println("<body bgcolor='#ECF0F1'>");
        out.println("<center>");
        out.println("<h2>Banco - ERP</h2>");
        out.println("<form method='post' action='servlet3' onsubmit='return validateForm()'>");
        out.println("<b>Transacciones Bancarias</b>");
        out.println("<table>");
        out.println("<tr>");
        out.println("<td>ID de Cuenta: " + value + "</td>");
        out.println("</tr>");
        out.println("<tr>");
        out.println("<td>Saldo Total: " + totalBalance + "</td>");
        out.println("</tr>");
        out.println("</table>");

        // Mostrar el mensaje de transacción si existe
        String transactionMessage = (String) request.getSession().getAttribute("transactionMessage");
        if (transactionMessage != null) {
            // Cambia esto para imprimir el mensaje de éxito en verde
            if (transactionMessage.contains("La transaccion fue realizada con exito")) {
                out.println("<p style='color: green;'>" + transactionMessage + "</p>");
            } else {
                out.println("<p>" + transactionMessage + "</p>");
            }
            request.getSession().removeAttribute("transactionMessage"); // Eliminar el mensaje de la sesión
        }

        out.println("<script>");
        out.println("function validateForm() {");
        out.println("    var mount = document.forms[0]['mount'].value;");
        out.println("    if (mount < 0) {");
        out.println("        alert('Por favor, ingrese un monto positivo.');");
        out.println("        return false;");
        out.println("    }");
        out.println("    return true;");
        out.println("}");
        out.println("</script>");

        // Agregar opción para realizar un depósito o un cargo
        out.println("<input type='number' name='mount' placeholder='Monto' required min='0' step='0.01'>");
        out.println("<select name='transactionType'>");
        out.println("<option value='deposit'>Depósito</option>");
        out.println("<option value='charge'>Cargo</option>");
        out.println("</select>");
        out.println("<input type='submit' value='Realizar Transacción'>");
        out.println("</form>");

        // Mostrar las transacciones del usuario
        out.println("<h3>Historial de Transacciones</h3>");
        out.println("<table border='1'>");
        out.println("<tr><th>ID de Cuenta</th><th>Fecha</th><th>Hora</th><th>Monto</th><th>Tipo </th></tr>");

        try {
            PreparedStatement transactionPst = con.prepareStatement("SELECT * FROM transactions WHERE accid = ?");
            transactionPst.setString(1, value);
            ResultSet transactionRs = transactionPst.executeQuery();

            while (transactionRs.next()) {
                out.println("<tr>");
                out.println("<td>" + transactionRs.getString("accid") + "</td>");
                out.println("<td>" + transactionRs.getDate("date") + "</td>");
                out.println("<td>" + transactionRs.getTime("hour") + "</td>"); // Hora de la transacción
                out.println("<td>" + transactionRs.getDouble("mount") + "</td>"); // Monto de la transacción

                // Obtener el tipo de transacción y traducirlo
                String transactionType = transactionRs.getString("type");
                String translatedType;

                if ("register".equals(transactionType)) {
                    translatedType = "registro";
                } else if ("deposit".equals(transactionType)) {
                    translatedType = "depósito";
                } else if ("charge".equals(transactionType)) {
                    translatedType = "cargo";
                } else {
                    translatedType = transactionType; // En caso de que sea un tipo no esperado
                }

                out.println("<td>" + translatedType + "</td>"); // Mostrar el tipo traducido
                out.println("</tr>");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
            out.println("<tr><td colspan='5'>Error al cargar las transacciones: " + ex.getMessage() + "</td></tr>");
        } finally {
            out.println("</table>");
            out.println("</center>");
            out.println("</body>");
            out.println("</html>");
        }
    }
}
