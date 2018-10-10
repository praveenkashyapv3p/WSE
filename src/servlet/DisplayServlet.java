package servlet;

import db.DBconnection;
import userinterface.Snippets;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static literals.Literals.dbcon;

public class DisplayServlet {

    /**
     * Creates HTML and prints result URLs on browser
     *
     * @param rs_json : contains rank,score,url
     * @param writer
     * @throws Exception
     */
    public void printresults(Servletpojo rs_json, Map<String, Integer> adIds, String queryrequest, List<String> missing, String userInput, PrintWriter writer, String alternatives, boolean typo, HttpServletRequest request, HttpServletResponse response) throws Exception {
        List<FetchedResultData> hasResult = rs_json.getFetchedResultData();
        int docid;
        String urls;
        String snippet;
        String title = "";
        String missingTerms = missing.stream().map(n -> String.valueOf(n)).collect(Collectors.joining(" "));
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        Snippets snippets = new Snippets();
        response.setContentType("text/html;charset=UTF-8");
        writer.println("<html class=\"mdl-js\" xmlns:padding-left=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "    <title>Results</title>\n" +
                "    <style>\n" +
                "        #SugSearch {\n" +
                "            background: none !important;\n" +
                "            border: none;\n" +
                "            padding: 0 !important;\n" +
                "            color: blue;\n" +
                "            text-decoration: underline;\n" +
                "            cursor: pointer;\n" +
                "        }\n" +
                "        .form {\n" +
                "            padding-left: 5px;\n" +
                "        }\n" +
                "        .column {\n" +
                "            float: left;\n" +
                "            width: 70%;\n" +
                "            padding-left: 15px;\n" +
                "            height: 100%;\n" +
                "        }\n" +
                "        .row{\n" +
                "            float: right;\n" +
                "            width: 25%;\n" +
                "            padding-top: 12%;\n" +
                "            padding-left: 15px;\n" +
                "            padding-right: 10px;\n" +
                "            height: 50%\n" +
                "        }\n" +
                ".topright {\n" +
                "        position: absolute;\n" +
                "        top: 10px;\n" +
                "        right: 10px;\n" +
                "    }" +
                "    </style>\n" +
                "    <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/icon?family=Material+Icons\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://code.getmdl.io/1.3.0/material.indigo-blue.min.css\">\n" +
                "    <script defer src=\"https://code.getmdl.io/1.3.0/material.min.js\"></script>\n" +
                "</head>\n" +
                "<body>\n" +
                "<a href=\"advertisement.html\" class=\"topright\">Register Ads</a>" +
                "\n" +
                "<div class=\"form\">\n" +
                "    <form action=\"result\" method=\"get\" id=\"servletclass\">\n" +
                "        <div >\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label \">\n" +
                "                <input class=\"mdl-textfield__input\" type=\"text\" name=\"query\" id=\"query\">\n" +
                "                <label class=\"mdl-textfield__label\" for=\"query\">Search Query...</label>\n" +
                "            </div>\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label \">\n" +
                "                <input class=\"mdl-textfield__input\" min=\"1\" max=\"20\" pattern=\"-?[0-9]*(\\.[0-9]+)?\" type=\"text\" id=\"k\"\n" +
                "                       name=\"k\">\n" +
                "                <label class=\"mdl-textfield__label\" for=\"k\">Number of Documents to Display</label>\n" +
                "                <span class=\"mdl-textfield__error\">Input should be a number between 1 and 20!</span>\n" +
                "            </div>\n" +
                "            <label class=\"mdl-radio mdl-js-radio mdl-js-ripple-effect\" for=\"english\">\n" +
                "                <input type=\"radio\" id=\"english\" class=\"mdl-radio__button\" name=\"lang\" value=\"en\" checked>\n" +
                "                <span class=\"mdl-radio__label\">English</span>\n" +
                "            </label>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n" +
                "            <label class=\"mdl-radio mdl-js-radio mdl-js-ripple-effect\" for=\"german\">\n" +
                "                <input type=\"radio\" id=\"german\" class=\"mdl-radio__button\" name=\"lang\" value=\"de\">\n" +
                "                <span class=\"mdl-radio__label\">German</span>\n" +
                "            </label>\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label\">\n" +
                "                <select class=\"mdl-textfield__input\" id=\"score\" name=\"score\">\n" +
                "                    <option value=\"1\">TF-IDF</option>\n" +
                "                    <option value=\"2\">OKAPI BM25</option>\n" +
                "                    <option value=\"3\">Page Rank & BM 25</option>\n" +
                "                </select>\n" +
                "                <label class=\"mdl-textfield__label\" for=\"score\">score</label>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div >\n" +
                "            <input type=\"submit\"\n" +
                "                   class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                "                   value=\"Search\" id=\"Search\" name=\"query\">\n" +
                "            </input>\n" +
                "            <input type=\"submit\"\n" +
                "                   class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                "                   value=\"Image Search\" name=\"image\" id=\"ImageSearch\">\n" +
                "            </input>\n" +
                "        </div>\n" +
                "    </form>\n" +
                "</div>" +
                "\n" +
                "<div  class=\"column\">\n" +
                "<div>\n" +
                "    <form action=\"result\" method=\"get\" id=\"servletclass\">\n" +
                "        <br>\n" +
                "        <table>\n" +
                "            <thead>\n" +
                "            <tr>\n" +
                "                <th align=\"left\">");
        if (typo == false) {
            writer.println("<h6><label>Search Results for:<b>" + queryrequest + "</b></label></h6>");
        } else {
            writer.println("<h6><label>Search Results for:<b>" + userInput + " </b>" +
                    "<br>instead of: <b>" + queryrequest + " </b></label></h6>");
        }
        writer.println(" </th></tr></thead><br>");
        if (hasResult.size() > 0) {
            writer.println("<div>");
            writer.println("<tbody>");
            for (FetchedResultData url : hasResult) {
                docid = url.getId();
                urls = url.getUrl();
                ResultSet resultSet = stmt.executeQuery("SELECT title FROM content WHERE docid=" + docid + "");
                while (resultSet.next())
                    title = resultSet.getString(1);
                snippet = snippets.snippet(docid, urls, userInput);
                writer.println(" <tr>\n");
                writer.println("<td>\n");

                writer.println("<a href=" + "\"" + url.getUrl() + "\">" + title + "</a><br>");
                writer.println("<a href=" + "\"" + url.getUrl() + "\" style=\"text-decoration:none\">");
                writer.println("<i style=\"color: #2e7d32; font-size: x-small\">" + url.getUrl() + "</i></a><br>");
                writer.println("<div>" + snippet + "</div>");
                if (!missingTerms.isEmpty())
                    writer.println("Missing : <del>" + missingTerms + "</del><br>");
                writer.println("</td>\n");
                writer.println(" </tr>\n");
            }
            writer.println("</tbody>");
            writer.println("</div>");
        } else {
            writer.println("<p>Sorry!!! No results to display<p>");
        }
        writer.println("</table><br><br><br><br>");
        writer.println("<div>");
        if (alternatives.length() > 0) {
            writer.println("Do you want to search for?<br>");
            writer.println("<input type=\"hidden\" value=" + rs_json.getK_val() + " name=\"k\">");
            writer.println("<input type=\"hidden\" value=" + rs_json.getLanguage() + " name=\"lang\">");
            writer.println("<input type=\"hidden\" value=" + rs_json.getModel() + " name=\"score\">");
            String str = String.join(" ", alternatives);
            writer.println("&emsp;&emsp;<input type=\"submit\" value='" + str + "' id=\"SugSearch\" >");
            writer.println("<input type=\"hidden\" value=" + str + " id=\"Query\" name=\"query\">");
        }
        writer.println("</div>");
        writer.println("</form>\n" + "</div></div>");
        writer.println("\n");
        writer.println("<div class=\"row\">\n" +
                "<form action=\"adBudget\" method=\"get\" id=\"adservletclass\">");

        writer.println("<div style=\"border:0.5px solid black; border-radius:5px;  padding-left: 5px\">");
        writer.println("<p style=\"font-size: small\"><u>Advertisements</u></p>");
        String adImage;
        double adBudget;
        String adUrl;
        for (String i : adIds.keySet()) {
            i = i.replaceAll("\\[|\\]", "");
            String ads = "SELECT advertitle,adverturl,textofad,advertimage,budget FROM advertisement WHERE id=" + Integer.parseInt(i) + "";
            ResultSet resultSet = stmt.executeQuery(ads);
            while (resultSet.next()) {
                adImage = resultSet.getString(4);
                adBudget = resultSet.getDouble(5);
                adUrl = resultSet.getString(2);
                if (adImage == null && adBudget >= 0.01) {
                    writer.println("<a href=\"/adBudget?input=" + adUrl + "\"\"><i  style=\"font-size: medium\">" + resultSet.getString(1) + "</i><br>");
                    writer.println("<a href=\"/adBudget?input=" + adUrl + "\" style=\"text-decoration:none\">");
                    //writer.println("<a href=" + "\"" + resultSet.getString(2) + "\" style=\"text-decoration:none\">");
                    writer.println("<i style=\"color: #2e7d32; font-size: x-small\">" + resultSet.getString(2) + "</i></a><br>");
                    writer.println("<i style=\"font-size: small\">" + resultSet.getString(3) + "</i><br><br>");
                } else if (adBudget > 0.01) {
                    writer.println("<a href=\"/adBudget?input=" + adUrl + "\"\">");
                    writer.println(" <img src=" + resultSet.getString(4) + " width=\"100\" height=\"100\"></a><br>");
                    writer.println("<i style=\"font-size: smaller\"><b>" + resultSet.getString(1) + "</b></i><br>");
                    writer.println("<i style=\"font-size: smaller\">" + resultSet.getString(3) + "</i><br>");
                }
            }
        }
        writer.println("</div></form></div>");
        writer.println("</body>");
        writer.println("</html>");
        stmt.close();
        conn.close();
    }

    public void printImageResults(Servletpojo img_json, String queryrequest, String userInput, PrintWriter writer, String alternatives, boolean typo, HttpServletRequest request, HttpServletResponse response) throws Exception {
        response.setContentType("text/html;charset=UTF-8");
        writer.println("<!DOCTYPE html>");
        writer.println("<html>");
        writer.println("<head>");
        writer.println("<title>Image Results</title>");
        writer.println("<style>\n" +
                        "* {\n" +
                        "padding-bottom: 3px;" +
                        "padding-top: 3px;" +
                        "}\n" +
                        ".grid{\n" +
                        "display: grid;\n" +
                        "grid-template-columns: auto auto auto auto auto;\n" +
                        "background-color: white;\n" +
                        "padding: 5px;\n" +
                        "}\n" +
                        " .topright {\n" +
                        "        position: absolute;\n" +
                        "        top: 10px;\n" +
                        "        right: 10px;\n" +
                        "    }"+
                "</style>");
        writer.println("<link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/icon?family=Material+Icons\">\n" +
                "        <link rel=\"stylesheet\" href=\"https://code.getmdl.io/1.3.0/material.indigo-blue.min.css\">\n" +
                "        <script defer src=\"https://code.getmdl.io/1.3.0/material.min.js\"></script>\n");
        writer.println("</head>");
        writer.println("<body>");
        writer.println("<a href=\"advertisement.html\" class=\"topright\">Register Ads</a>" +
                "\n" +
                "<div class=\"form\">\n" +
                "    <form action=\"result\" method=\"get\" id=\"servletclass\">\n" +
                "        <div >\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label \">\n" +
                "                <input class=\"mdl-textfield__input\" type=\"text\" name=\"query\" id=\"query\">\n" +
                "                <label class=\"mdl-textfield__label\" for=\"query\">Search Query...</label>\n" +
                "            </div>\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label \">\n" +
                "                <input class=\"mdl-textfield__input\" min=\"1\" max=\"20\" pattern=\"-?[0-9]*(\\.[0-9]+)?\" type=\"text\" id=\"k\"\n" +
                "                       name=\"k\">\n" +
                "                <label class=\"mdl-textfield__label\" for=\"k\">Number of Documents to Display</label>\n" +
                "                <span class=\"mdl-textfield__error\">Input should be a number between 1 and 20!</span>\n" +
                "            </div>\n" +
                "            <label class=\"mdl-radio mdl-js-radio mdl-js-ripple-effect\" for=\"english\">\n" +
                "                <input type=\"radio\" id=\"english\" class=\"mdl-radio__button\" name=\"lang\" value=\"en\" checked>\n" +
                "                <span class=\"mdl-radio__label\">English</span>\n" +
                "            </label>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;\n" +
                "            <label class=\"mdl-radio mdl-js-radio mdl-js-ripple-effect\" for=\"german\">\n" +
                "                <input type=\"radio\" id=\"german\" class=\"mdl-radio__button\" name=\"lang\" value=\"de\">\n" +
                "                <span class=\"mdl-radio__label\">German</span>\n" +
                "            </label>\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label\">\n" +
                "                <select class=\"mdl-textfield__input\" id=\"score\" name=\"score\">\n" +
                "                    <option value=\"1\">TF-IDF</option>\n" +
                "                    <option value=\"2\">OKAPI BM25</option>\n" +
                "                    <option value=\"3\">Page Rank & BM 25</option>\n" +
                "                </select>\n" +
                "                <label class=\"mdl-textfield__label\" for=\"score\">score</label>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div >\n" +
                "            <input type=\"submit\"\n" +
                "                   class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                "                   value=\"Search\" id=\"Search\" name=\"query\">\n" +
                "            </input>\n" +
                "            <input type=\"submit\"\n" +
                "                   class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                "                   value=\"Image Search\" name=\"image\" id=\"ImageSearch\">\n" +
                "            </input>\n" +
                "        </div>\n" +
                "    </form>\n" +
                "</div>" +
                "\n");
        writer.println("<div class=\"form\">\n");
        writer.println("<form action=\"result\" method=\"get\" id=\"servletclass\">");
        writer.println("<div>");
        if (typo == false) {
            writer.println("<label>&emsp;&emsp;Search Results for:<b>" + queryrequest + "</b></label>");
        } else {
            writer.println("<label >&emsp;&emsp;Search Results for :<b>" + userInput + " </b>" +
                    "<br>&emsp;&emsp; instead of: <b>" + queryrequest + " </b></label>");
        }
        writer.println("</div><br>");
        List<ImagePOJO> hasResult = img_json.getImageResultData();
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        if (hasResult.size() > 0) {
            writer.println("<div class=\"grid\">");
            for (ImagePOJO url : hasResult) {
                writer.println("<div class=\"column\">");
                writer.println("&emsp;&emsp;<img src=" + "\"" + url.getImgUrl() + "\"" + " alt=\"Cannot load image\" width=\"200\" height=\"200\"></img><br>" + "\n");
                writer.println("</div>");
            }
            writer.println("</div>");
        } else {
            writer.println("<p> &emsp;&emsp;Sorry!!! No results to display<p>");
        }
        writer.println("</form>\n");
        writer.println("</div>");
        writer.println("</body>");
        writer.println("</html>");
        stmt.close();
        conn.close();
    }

    public void printMetaResults(Servletpojo finaljson, HttpServletResponse response) throws Exception {
        List<MetaPOJO> hasResult = finaljson.getMetaResultData();
        int metaVmId;
        String metaUrl;
        Double metaScore;
        String displayUrl = "";
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        writer.println("<html class=\"mdl-js\" xmlns:padding-left=\"http://www.w3.org/1999/xhtml\">\n" +
                "<head>\n" +
                "    <title>Meta Search Results</title>\n" +
                "    <style>\n" +
                "*{\n" +
                "padding-left: 10px;\n" +
                "}\n" +
                " .topright {\n" +
                "        position: absolute;\n" +
                "        top: 10px;\n" +
                "        right: 10px;\n" +
                "    }" +
                "    </style>\n" +
                "    <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/icon?family=Material+Icons\">\n" +
                "    <link rel=\"stylesheet\" href=\"https://code.getmdl.io/1.3.0/material.indigo-blue.min.css\">\n" +
                "    <script defer src=\"https://code.getmdl.io/1.3.0/material.min.js\"></script>\n" +
                "</head>\n" +
                "<body>" +
                "\n" +
                "<div class=\"form\">\n" +
                "    <form action=\"MetaServlet\" method=\"get\" id=\"servletclass\">\n" +
                "        <a href=\"configuration.jsp\" class=\"topright\">Configuration</a>\n" +
                "        <div >\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label \">\n" +
                "                <input class=\"mdl-textfield__input\" type=\"text\" name=\"query\" id=\"query\">\n" +
                "                <label class=\"mdl-textfield__label\" for=\"query\">Search Query...</label>\n" +
                "            </div>\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label \">\n" +
                "                <input class=\"mdl-textfield__input\" min=\"1\" max=\"20\" pattern=\"-?[0-9]*(\\.[0-9]+)?\" type=\"text\" id=\"k\"\n" +
                "                       name=\"k\">\n" +
                "                <label class=\"mdl-textfield__label\" for=\"k\">Number of Documents to Display</label>\n" +
                "                <span class=\"mdl-textfield__error\">Input should be a number between 1 and 20!</span>\n" +
                "            </div>\n" +
                "            <div class=\"mdl-textfield mdl-js-textfield mdl-textfield--floating-label\">\n" +
                "                <select class=\"mdl-textfield__input\" id=\"score\" name=\"score\">\n" +
                "                    <option value=\"1\">TF-IDF</option>\n" +
                "                    <option value=\"2\">OKAPI BM25</option>\n" +
                "                    <option value=\"3\">Page Rank & BM 25</option>\n" +
                "                </select>\n" +
                "                <label class=\"mdl-textfield__label\" for=\"score\">score</label>\n" +
                "            </div>\n" +
                "        </div>\n" +
                "        <div >\n" +
                "            <input type=\"submit\"\n" +
                "                   class=\"mdl-button mdl-js-button mdl-button--raised mdl-js-ripple-effect mdl-button--accent\"\n" +
                "                   value=\"META SEARCH\" id=\"Search\" name=\"Search\">\n" +
                "            </input>\n" +
                "        </div>\n" +
                "    </form>\n" +
                "</div>" +
                "\n" +
                "<div>\n" +
                "<table>\n");
        if (hasResult.size() > 0) {
            writer.println("<tbody>");
            for (MetaPOJO url : hasResult) {
                metaVmId = url.getMetaVmId();
                metaUrl = url.getMetaUrl();
                metaScore = url.getMetaScore();
                ResultSet resultSet = stmt.executeQuery("SELECT metaurl FROM metaurls WHERE id=" + metaVmId + "");
                try {
                    while (resultSet.next()) {
                        displayUrl = resultSet.getString(1);
                        displayUrl = displayUrl.substring(0, displayUrl.lastIndexOf(":") + 5);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                writer.println(" <tr padding-left=\"10px\">\n");
                writer.println("<td>\n");

                writer.println("<a href=" + "\"" + metaUrl + "\">" + metaUrl + "</a><br>");
                writer.println("<a href=" + "\"" + displayUrl + "\" style=\"text-decoration:none\">");
                writer.println("<i style=\"color: #2e7d32; font-size: small\">From VM : " + displayUrl + "</i></a><br>");
                writer.println("<i style=\"color: black; font-size: small\">Score : " + metaScore + "</i><br>");
                writer.println("</td>\n");
                writer.println(" </tr>\n");
            }
            writer.println("</tbody>");
        } else {
            writer.println("<p>Sorry!!! No results to display<p>");
        }
        writer.println("</table>");
        writer.println("</div>");
        writer.println("</form>");
        writer.println("</body>");
        writer.println("</html>");
        stmt.close();
        conn.close();
    }

}
