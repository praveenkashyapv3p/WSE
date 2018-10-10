package servlet;

import db.DBconnection;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import queryprocessing.Advertisement;
import queryprocessing.LevenshteinCheck;
import userinterface.SearchReq;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static literals.Literals.dbcon;


public class Resultservlet extends HttpServlet {
    static HashMap<String, Long> ip_restrict = new HashMap<>();

   /* *//**
     * @return connection object
     *//*
    public Connection getCon() {
        Connection conn = null;
        try {
            Literals l = new Literals();
            conn = DBconnection.getCon(dbcon, true);
        } catch (Exception e) {
            System.out.println(" ");
        }
        return conn;
    }*/

    /**
     * @param queryrequest : user input query
     * @param input_k      : number of results to be displayed
     * @param languag      : user language preference
     * @param writer
     * @throws Exception
     */
    public void queryprocessing(String queryrequest, String input_k, String image, String languag, String model, PrintWriter writer, HttpServletRequest request, HttpServletResponse response, boolean dispjson) throws Exception {
        int no_of_docs = Integer.parseInt(input_k);
        int score_model = Integer.parseInt(model);
        String userInput;
        boolean typo = false;
        String alternatives;
        SearchReq f = new SearchReq();
        LevenshteinCheck levenshteinCheck = new LevenshteinCheck();
        if (dispjson!=true)
            userInput = levenshteinCheck.checkForTypo(queryrequest);
        else
            userInput = queryrequest;
        List<String> missing = levenshteinCheck.getMissingTerms();
        if (!queryrequest.equalsIgnoreCase(userInput))
            typo = true;
        alternatives = levenshteinCheck.alternativesSuggest(userInput);
        Servletpojo rs_json = f.userSearch(userInput, no_of_docs, languag, score_model);
        Servletpojo img_json = f.imgSearch(userInput, no_of_docs, languag);
        printJson(rs_json,img_json,image, queryrequest, missing, userInput, no_of_docs, writer, alternatives, typo, request, response, dispjson);
    }

    /**
     * Count total number of terms in features table
     *
     * @return
     * @throws Exception
     */
    public int getcw() throws Exception {
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        String queryToSearch = "SELECT count(term) FROM features";
        ResultSet rs = stmt.executeQuery(queryToSearch);
        int n = 0;
        while (rs.next()) {
            n = (rs.getInt(1));
        }
        conn.close();
        stmt.close();
        return n;
    }

    /**
     * Prints JSON
     *
     * @param rs_json      : contains rank, score , url, stemmed terms
     * @param queryrequest
     * @param no_of_docs
     * @param writer
     * @throws Exception
     */
    public void printJson(Servletpojo rs_json,Servletpojo img_json, String image, String queryrequest, List<String> missing, String userInput, int no_of_docs, PrintWriter writer, String alternatives, boolean typo, HttpServletRequest request, HttpServletResponse response, boolean dispjson) throws Exception {
        JSONObject final_json = new JSONObject();
        ScriptEngineManager manager = new ScriptEngineManager();
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        JSONArray df_array = new JSONArray();
        String query_to_sql = "";
        List<String> queries = new ArrayList<>();
        for (String statadd : rs_json.getStemmedTerms()) {
            query_to_sql = (statadd).replaceAll("\"", "").replaceAll("\\[", "").replaceAll("\\]", "");
            queries.add(query_to_sql);
        }
        Iterator<String> iterator = queries.iterator();
        while (iterator.hasNext()) {
            String queryToSearch = " SELECT term,count(docid) FROM features WHERE term ='" + iterator.next() + "' GROUP BY term;";
            ResultSet rs = stmt.executeQuery(queryToSearch);
            while (rs.next()) {
                JSONObject j = new JSONObject();
                j.put("term", rs.getString(1));
                j.put("df", rs.getInt(2));
                df_array.add(j);
            }
        }
        JSONArray query_array = new JSONArray();
        JSONObject query_json = new JSONObject();
        query_json.put("query", userInput);
        query_json.put("k", no_of_docs);
        query_array.add(query_json);
        final_json.put("resultList", rs_json.getFetchedResultData());
        final_json.put("query", query_array);
        final_json.put("stat", df_array);
        final_json.put("cw", getcw());
        Advertisement advertisement = new Advertisement();
        Map<String, Integer> adIds = advertisement.advertisement(userInput);
        DisplayServlet displayServlet = new DisplayServlet();
        if (dispjson == false && image == null) {
            displayServlet.printresults(rs_json, adIds, queryrequest, missing, userInput, writer, alternatives, typo, request, response);
        }
        else if (dispjson == false && image != null)
            displayServlet.printImageResults(img_json, queryrequest, userInput, writer, alternatives, typo, request, response);
        if (dispjson == true){
            response.setContentType("text/json;charset=UTF-8");
            ScriptEngine scriptEngine = manager.getEngineByName("JavaScript");
            scriptEngine.put("jsonString", final_json);
            scriptEngine.eval("result = JSON.stringify(JSON.parse(jsonString), null, 2)");
            String prettyPrintedJson = (String) scriptEngine.get("result");
            writer.println(prettyPrintedJson);
        }
        conn.close();
        stmt.close();
    }



    /**
     * Gets IP addressed and restrict requests to Max 10 per second and only one request per second from single ip
     *
     * @param request
     * @throws Exception
     */
    public void getClientIp(HttpServletRequest request) throws Exception {
        /*Map<String, String> result = new HashMap<>();
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = (String) headerNames.nextElement();
            String value = request.getHeader(key);
            result.put(key, value);
        }*/
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null)
            ip = request.getRemoteAddr();
        int ip_size = ip_restrict.size();
        long time = Calendar.getInstance().getTimeInMillis();
        if (ip_size < 10) {
            ip_restrict.put(ip, time);
        } else if ((ip_restrict.containsKey(ip) && time - ip_restrict.get(ip) < 1000) || (ip_size >= 10)) {
            ip_restrict.remove(ip);
        }
    }


    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        boolean dispjson = false;
        response.setContentType("text/html;charset=UTF-8");
        try {
            String meta = request.getParameter("Search");
            String queryrequest = request.getParameter("query");
            String input_k = request.getParameter("k");
            String languag = request.getParameter("lang");
            String model = request.getParameter("score");
            String image = request.getParameter("image");
            if (input_k.isEmpty())
                input_k = "20";
            PrintWriter writer = response.getWriter();
            getClientIp(request);
            //if (meta.isEmpty() || meta==null)
                queryprocessing(queryrequest, input_k,image, languag, model, writer, request, response, dispjson);
            //else
                //metaSearch(queryrequest,input_k,model);
        } catch (Exception ex) {
            System.out.println("Catch block: Servlet Exception");
            ex.printStackTrace();
        }
    }
}
