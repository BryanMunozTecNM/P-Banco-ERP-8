/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/JSP_Servlet/Servlet.java to edit this template
 */

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
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

@WebServlet("/servlet1")
public class servlet1 extends HttpServlet {

    PrintWriter out = null;
    Connection con;
    PreparedStatement pst;
    ResultSet rs;
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            String result;
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection("jdbc:mysql://localhost/bar", "root", "");
            ServletContext context = getServletContext();
            context.setAttribute("accid", "");
            String accid = request.getParameter("accid");
            String accpass = request.getParameter("accpass");

            // Consulta para verificar el accid y accpass
            pst = con.prepareStatement("SELECT * FROM login WHERE accid = ? AND accpass = ?");
            pst.setString(1, accid);
            pst.setString(2, accpass); // La comparación será sensible a mayúsculas y minúsculas
            rs = pst.executeQuery();
            boolean row = rs.next();

            if (row) {
                result = rs.getString(2);
                context.setAttribute("accid", result);
                RequestDispatcher dispatcher = getServletContext().getRequestDispatcher("/secondservlet");
                if (dispatcher == null) {
                }
                dispatcher.forward(request, response);
                con.close();
        } else {
            out = response.getWriter();
            response.setContentType("text/html");
            out.println("<html>");
            out.println("<body bgcolor='#ECF0F1'>");
            out.println("ID de cuenta y/o clave no válido(s)");
            out.println("</body>");
            out.println("</html>");
            out.close();
        }

        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

}
