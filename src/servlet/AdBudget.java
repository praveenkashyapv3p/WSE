package servlet;

import db.DBconnection;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static literals.Literals.dbcon;

public class AdBudget extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        System.out.println("url");
        String url = request.getParameter("input");
        System.out.println(url);
        double newBudget;
        try {
            Connection conn = DBconnection.getCon(dbcon, true);
            Statement stmt = conn.createStatement();
            String budget = "SELECT budget FROM advertisement WHERE adverturl='"+url+"';";
            ResultSet resultSet = stmt.executeQuery(budget);
            while (resultSet.next()){
                newBudget = resultSet.getDouble(1)-0.01;
                stmt.executeUpdate("UPDATE advertisement SET budget="+newBudget+" WHERE adverturl='"+url+"'");
            }
            stmt.close();
            conn.close();
        }catch (Exception e){

        }
        try {
            response.sendRedirect(url);
        }catch (Exception e){

        }

    }
}
