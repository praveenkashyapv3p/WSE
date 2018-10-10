package queryprocessing;

import db.DBconnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static literals.Literals.dbcon;

public class MetaSearchQP  implements Runnable {
    public static List<String> meta_url ;
    public static Map<String, Integer> map = new HashMap<String, Integer>();

    public MetaSearchQP(List<String> urls)
    {
        meta_url=urls;
    }


    private String fetchJSON(String urlname) {
        try {
            URL url = new URL(urlname);
            BufferedReader breader = new BufferedReader(
                    new InputStreamReader(url.openStream()));
            //BufferedReader breader = new BufferedReader(new InputStreamReader(((HttpURLConnection) (new URL(url)).openConnection()).getInputStream(), Charset.forName("UTF-8")));
            StringBuffer urlcnt = new StringBuffer();
            String htmlCnt;
            while ((htmlCnt =
                    breader.readLine()) != null) {
                urlcnt.append(htmlCnt);
            }
            breader.close();
            return urlcnt.toString();

        } catch (MalformedURLException e) {
            e.printStackTrace();
            System.out.println("Handled exception");
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Handled exception");
            return null;
        }

    }

    private void updateCWScore(long cw,int id) throws Exception
    {
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        String query = "UPDATE metaurls SET cwval = "+cw+" WHERE id = "+id;
        stmt.executeUpdate(query);
        conn.close();
        stmt.close();
    }

    private int termExists(int id,String term) throws Exception
    {
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        int count=0;
        String check = "SELECT count(*) as count from metasearchterms where collectionid = "+ id +" and term like '"+ term + "'";
        ResultSet resultSet = stmt.executeQuery(check);
        while (resultSet.next()){
            count=resultSet.getInt(1);
        }
        conn.close();
        stmt.close();
        if(count >0)
            return 0;
        else
            return 1;
    }

    private void calculateCORIScore(String jsonstr,int id) throws Exception{
        JSONParser parser = new JSONParser();
        Connection conn = DBconnection.getCon(dbcon,true);
        String querystr = "INSERT INTO metasearchdocs(collectionid,docurl,score) VALUES (?,?,?)";
        String updatestr = "INSERT INTO metasearchterms(collectionid,term,dfval) VALUES (?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(querystr);
        PreparedStatement pstmt_terms = conn.prepareStatement(updatestr);
        try{
            Object obj = parser.parse(jsonstr);
            JSONObject jsonObject = (JSONObject) obj;
            System.out.println("JSONObject jsonObject:" + jsonObject);
            JSONArray rslt = (JSONArray) jsonObject.get("resultList");
            JSONArray stat = (JSONArray) jsonObject.get("stat");
            long cw = (long) jsonObject.get("cw");
            updateCWScore(cw,id);
            for (int i = 0; i < rslt.size(); i++) {
                JSONObject rsltjsonobject = (JSONObject) rslt.get(i);
                pstmt.setInt(1, id);
                pstmt.setString(2, (String) rsltjsonobject.get("url"));
                pstmt.setDouble(3, (double) rsltjsonobject.get("score"));
                pstmt.addBatch();
                System.out.println("collectionid  : " + id);
                System.out.println("docurl:  " + (String) rsltjsonobject.get("url"));
                System.out.println("score : " + (double) rsltjsonobject.get("score"));
            }
            pstmt.executeBatch();
            for (int i = 0; i < stat.size(); i++) {
                JSONObject statjsonobject = (JSONObject) stat.get(i);
                String term=(String) statjsonobject.get("term");
                int flag=termExists(id,term);
                if(flag!=0) {
                    pstmt_terms.setInt(1, id);
                    pstmt_terms.setString(2, term);
                    pstmt_terms.setLong(3, (long) statjsonobject.get("df"));
                    pstmt_terms.addBatch();
                    System.out.println("collectionid  : " + id);
                    System.out.println("term:  " + term);
                    System.out.println("dfval : " + (long) statjsonobject.get("df"));
                }
            }
            pstmt_terms.executeBatch();
        }catch(ParseException pe){
            System.out.println("position: " + pe.getPosition());
            System.out.println(pe);
        }
        conn.close();
        pstmt.close();
        pstmt_terms.close();
    }

    private int getID(String url) throws Exception
    {
        Connection conn = DBconnection.getCon(dbcon,true);
        url=url.substring(0, url.indexOf("="));
//        System.out.println("url sibstring" +url);
        Statement stmt = conn.createStatement();
        int id=0;
        String getid = "SELECT id FROM metaurls where metaurl like '%"+ url + "%' and state=1";
//        System.out.println("getid" +getid);
        ResultSet resultSet = stmt.executeQuery(getid);
        while (resultSet.next()){
            id=resultSet.getInt(1);
        }
        conn.close();
        stmt.close();
        return id;

    }

    public synchronized String getURLFromArray()
    {
        System.out.println("before length" + meta_url.size());
        System.out.println(Arrays.toString(meta_url.toArray()));
        String metUrl=null;
        if((meta_url != null) && !meta_url.isEmpty()) {
            metUrl = meta_url.get(0);
            meta_url.remove(metUrl);
        }
        System.out.println("after length" +meta_url.size());
        System.out.println(Arrays.toString(meta_url.toArray()));
        return metUrl;
    }

    public void run() {
        try {
            String url=getURLFromArray();
            int id=getID(url);
            System.out.println("url from array: "+ url);
            System.out.println("url from array has id: "+ id);
            System.out.println("Thread named: " + Thread.currentThread().getName() + "has picked up URL : " + url);
            String jsonContent = fetchJSON(url);
//            jsonContent=jsonContent.replaceAll("\\\\","");
            calculateCORIScore(jsonContent,id);
            System.out.println("jsoncontent :"+jsonContent);

        } catch(Exception e)
        {
            e.printStackTrace();
            System.out.println("Thread named : " + Thread.currentThread().getName() +" interrupted abruptly");
        }
        System.out.println("Thread named: " + Thread.currentThread().getName() + "run is over" );

    }


}
