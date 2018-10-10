package servlet;

import db.DBconnection;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Enumeration;

import static literals.Literals.dbcon;

public class configServlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        try {
            String url="";
            String removeurl = "url";
            String updateurl = "status";
            String removeString = "";
            String updateString = "";
            int statVal;
            Connection conn = DBconnection.getCon(dbcon,true);
            Statement stmt = conn.createStatement();
            Enumeration paramNames = request.getParameterNames();
            while(paramNames.hasMoreElements()) {
                String paramName = (String) paramNames.nextElement();
                String paramValue = request.getParameter(paramName);
                if(paramName.contains(removeurl)){
                    url = paramValue;
                    removeString = "DELETE FROM metaurls WHERE metaurl='"+paramValue+"'";
                }
                if(paramName.contains(updateurl)){
                    if (paramValue.equalsIgnoreCase("activate"))
                        statVal = 1;
                    else
                        statVal = 0;
                   updateString = "UPDATE metaurls SET state='"+statVal+"' WHERE metaurl='"+url+"'";

                }
                if (paramValue.contains("remove")) {
                    stmt.executeUpdate(removeString);
                }
                if (paramValue.contains("activate") || paramValue.contains("deactivate")) {
                    stmt.executeUpdate(updateString);
                }

            }

            String meta[] = request.getParameterValues("field");

                String metUrls = "INSERT INTO metaurls(metaurl,state) VALUES (?,?)";
                PreparedStatement pstmt = conn.prepareStatement(metUrls);
                for (String c : meta) {
                    System.out.println(c);
                    if (c!="" || !c.equalsIgnoreCase("")){
                        pstmt.setString(1, c);
                        pstmt.setInt(2, 0);
                        pstmt.addBatch();
                    }
                }
                pstmt.executeBatch();

            conn.close();
            pstmt.close();
            response.sendRedirect("/configuration.jsp");
        } catch (Exception ex) {
            System.out.println("Catch block: Servlet Exception");
            ex.printStackTrace();
        }
    }
}
