package servlet;

import db.DBconnection;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Statement;

import static literals.Literals.dbcon;

public class Adservlet extends HttpServlet {
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("text/html;charset=UTF-8");
        try {
            String adTitle = request.getParameter("adTitle");
            String adUrl = request.getParameter("adUrl");
            String adText = request.getParameter("adText");
            String adTags = request.getParameter("adTags");
            String adBudget = request.getParameter("adBudget");
            String adImage = request.getParameter("adImage");
            String adName = request.getParameter("adName");
            String adEmail = request.getParameter("adEmail");
            boolean check = adParameters(adBudget, adImage, adTags, adText, adTitle, adUrl, adName, adEmail);
            PrintWriter writer = response.getWriter();
            writer.println("<html>");
            writer.println("<head><title>");
            writer.println("Ad Registeration</title>");
            writer.println("<link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/icon?family=Material+Icons\">\n" +
                    "        <link rel=\"stylesheet\" href=\"https://code.getmdl.io/1.3.0/material.indigo-blue.min.css\">\n" +
                    "        <script defer src=\"https://code.getmdl.io/1.3.0/material.min.js\"></script>\n");
            writer.println("</head>");
            writer.println("<body>");
            writer.println("<div style=\"padding-left: 50px; padding-top: 50px\">");
            if (check == true) {
                writer.println("<p>Thank you for using our Search Engine.\n Your advertisement has been succesffully registered.</p>\n");
                writer.println("<p>You can see your advertisement in the search engine by gving your tags as search items.</p>");
                writer.println(" <button class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                        "                        value=\"Register More\" onclick=\"window.location.href=\'advertisement.html\'\" id=\"register\">Register More\n" +
                        "                </button>");
                writer.println(" <button class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                        "                        value=\"OK\" onclick=\"window.location.href=\'index.html\'\" id=\"SearchEngine\">OK\n" +
                        "                </button>");
                //writer.println("<input type=\"submit\" align=\"center\" value=\"Register More\" onclick=\"window.location.href=\'advertisement.html\'\" id=\"register\">");
                //writer.println("<input type=\"submit\" align=\"center\" value=\"OK\" onclick=\"window.location.href=\'index.html\'\" id=\"SearchEngine\">");
            } else {
                writer.println("<p>Seems there is an error. Please try again.</p>\n");
                writer.println("<p>For further assistance or complaints contact group08@cs.uni-kl.de</p>");
                writer.println(" <button class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                        "                        value=\"Register More\" onclick=\"window.location.href=\'advertisement.html\'\" id=\"register\">Try Again\n" +
                        "                </button>");
                writer.println(" <button class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                        "                        value=\"OK\" onclick=\"window.location.href=\'index.html\'\" id=\"SearchEngine\">OK\n" +
                        "                </button>");
               // writer.println("<input type=\"submit\" align=\"center\" value=\"Register More\" onclick=\"window.location.href=\'advertisement.html\'\" id=\"register\">");
               // writer.println("<input type=\"submit\" align=\"center\" value=\"OK\" onclick=\"window.location.href=\'index.html\'\" id=\"SearchEngine\">");
            }
            writer.println("</div></body></html>");
        } catch (Exception ex) {
            System.out.println("Catch block: Servlet Exception");
            ex.printStackTrace();
        }
    }

    private boolean adParameters(String adBudget, String adImage, String adTags, String adText, String adTitle, String adUrl, String adName, String adEmail) {
        try {
            Connection connection = DBconnection.getCon(dbcon, true);
            Statement statement = connection.createStatement();
            /*String tagsplit[] = adTags.split(",");
            String tagsString = String.join(",",tagsplit).replaceAll(" ",",");
            tagsplit = tagsString.split(",");
            tagsString = String.join(",",tagsplit).replaceAll("[,]", " ");
            tagsString = tagsString.replaceAll("\\s{2,}", " ");*/
            if (adImage.isEmpty()) {
               String inst="INSERT INTO advertisement(advertitle, adverturl, textofad, tags, budget,cust_name, cust_mailid) VALUES ('" + adTitle + "','" + adUrl + "','" + adText + "','" + adTags + "'," + adBudget + ",'" + adName + "','" + adEmail + "')";
                statement.executeUpdate(inst);
            } else {
                statement.executeUpdate("INSERT INTO advertisement(advertitle, adverturl, textofad, tags, budget, advertimage, cust_name, cust_mailid) VALUES ('" + adTitle + "','" + adUrl + "','" + adText + "','" + adTags + "'," + adBudget + ",'" + adImage + "','" + adName + "','" + adEmail + "')");
            }
            statement.close();
            connection.close();
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
