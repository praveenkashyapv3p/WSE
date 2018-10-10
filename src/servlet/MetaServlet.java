package servlet;

import db.DBconnection;
import queryprocessing.MetaSearchQP;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static literals.Literals.dbcon;
import static literals.Literals.searcheng_lmt;

public class MetaServlet extends HttpServlet{
    public static String[] metaURLs ;
    public String[] query_terms;
    List<String> urlList;
    public static int threadCount = 3;
    public static List<Thread> threadPool = new ArrayList<>();
    List<Integer> srceng_ids=null;
    String queryrequest=null;
    String input_k=null;
    String model=null;

    public void doGet(HttpServletRequest request, HttpServletResponse response) {

        try {
            String stat = request.getParameter("status");
            String remo = request.getParameter("remove");
            queryrequest = request.getParameter("query");
            query_terms=queryrequest.split("\\s+");
            input_k = request.getParameter("k");
            model = request.getParameter("score");
            System.out.println("calling fetchdocs");
            Servletpojo finaljson = fetchdocs();
            System.out.println("Dropped Indexes from try");
            DisplayServlet displayServlet = new DisplayServlet();
            displayServlet.printMetaResults(finaljson,response);
        } catch (Exception e) {

        }
        System.out.println("execution complete");

    }

    private void clearDocTb() throws Exception
    {
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        String querystr="delete from metasearchdocs";
        stmt.executeUpdate(querystr);
        conn.close();
        stmt.close();
    }

    public int checkForStatistics() throws Exception
    {
        System.out.println("inside checkForStatistics");
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        int count=0;
        String qrstr = "SELECT count(*) as count FROM metasearchterms where ";
        for (int i=0;i<query_terms.length;i++) {
            if (i != (query_terms.length - 1))
                qrstr = qrstr.concat("term like '"+query_terms[i]+ "' OR ");
            else
                qrstr = qrstr.concat("term like '"+query_terms[i] + "'");
        }
        System.out.println("query:"+qrstr);
        ResultSet resultSet = stmt.executeQuery(qrstr);
        while (resultSet.next()){
            count=resultSet.getInt(1);
        }
        conn.close();
        stmt.close();
        if(count>0)
            return 0;
        else
            return 1;
    }

    public long getTotCW() throws Exception{
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        int sumVal=0;
        String count = "SELECT sum(cwval) as count FROM metaurls where state=1";
        ResultSet resultSet = stmt.executeQuery(count);
        while (resultSet.next()){
            sumVal=resultSet.getInt(1);
        }
        conn.close();
        stmt.close();
        return sumVal;
    }

    public int getcf(String term) throws Exception{
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        int cfVal=0;
        String count = "SELECT count(collectionid) as count from metasearchterms where term like '"+ term + "'";
        ResultSet resultSet = stmt.executeQuery(count);
        while (resultSet.next()){
            cfVal=resultSet.getInt(1);
        }
        conn.close();
        stmt.close();
        return cfVal;

    }
    public void computeCORIScore() throws Exception{
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        String coriupdate="UPDATE metasearchterms SET coriscore = ?,rmin=?,rmax=? WHERE id = ?";
        PreparedStatement pstmt=conn.prepareStatement(coriupdate);;
        int colcount=getCollectionCount();
        long totalcw=getTotCW();
        long avg_cw=totalcw/colcount;
        String idval = "select a.id as colid,cwval,b.id,dfval,term from metaurls a, metasearchterms b where a.id=b.collectionid and a.state=1 and coriscore is null";
        ResultSet rs = stmt.executeQuery(idval);
        while (rs.next()) {
            int colid=rs.getInt("colid");
            int id=rs.getInt("id");
            long df=rs.getLong("dfval");
            long cw=rs.getLong("cwval");
            double rem=df+50+150*(cw/avg_cw);
            double T=df/rem;
            int cf=getcf(rs.getString("term"));
            double I=(Math.log((colcount+0.5)/cf))/(Math.log((colcount+1.0)));
            double coriscore=0.4+(1-0.4)*T*I;
            double rmax=0.4+(1-0.4)*1.0*I;
            double rmin=0.4+(1-0.4)*0.0*I;
            System.out.println("coriscore :"+ coriscore);
            System.out.println("id : " + id);
            pstmt.setDouble(1, coriscore);
            pstmt.setDouble(2, rmin);
            pstmt.setDouble(3, rmax);
            pstmt.setInt(4, id);
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        conn.close();
        pstmt.close();
        stmt.close();
    }

    private int getCollectionCount() throws Exception{
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        int countVal=0;
        String count = "SELECT count(*) as count FROM metaurls where state=1";
        ResultSet resultSet = stmt.executeQuery(count);
        while (resultSet.next()){
            countVal=resultSet.getInt(1);
        }
        conn.close();
        stmt.close();
        return countVal;
    }

    public Servletpojo getBestDocs(int flag) throws Exception{
        System.out.println("inside get best docs : flag value "+ flag);
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        List<MetaPOJO> listjson = new ArrayList<>();
        ResultSet rs;
        PreparedStatement pstmt;
        String querystr=null;
        Servletpojo servletpojo = new Servletpojo();
        if(flag!=0)
        {
            if(input_k !=null)
                querystr="select collectionid,docurl,score from metasearchdocs order by score desc limit " +input_k;
            else
                querystr="select collectionid,docurl,score from metasearchdocs order by score desc";
            System.out.println("flag not 0 query :"+querystr);
            rs = stmt.executeQuery(querystr);
            int i = 1;
            System.out.println("Search Results:  ");
            while (rs.next()) {
                System.out.println("*******************************");
                System.out.println("Results :  ");
                System.out.println("collection id : " + rs.getInt("collectionid"));
                System.out.println("url : " + rs.getString("docurl"));
                System.out.println("rank : " + i);
                System.out.println("score : " + rs.getDouble("score"));
                System.out.println("*******************************");
                MetaPOJO fetchedResultData = new MetaPOJO();
                fetchedResultData.setMetaVmId(rs.getInt("collectionid"));
                fetchedResultData.setMetaScore(rs.getDouble("score"));
                fetchedResultData.setMetaRank(i);
                fetchedResultData.setMetaUrl(rs.getString("docurl"));
                listjson.add(fetchedResultData);
                i++;
            }
            servletpojo.setMetaResultData(listjson);
            clearDocTb();
            return servletpojo;
        }
        else
        {
            String substr = "SELECT id  FROM metasearchterms where ";
            for (int i=0;i<query_terms.length;i++) {
                if (i != (query_terms.length - 1))
                    substr = substr.concat("term like '"+query_terms[i]+ "' OR ");
                else
                    substr = substr.concat("term like '"+query_terms[i] + "'");
            }
            if(input_k !=null)
                querystr=" select b.collectionid as collid,docurl, (b.score+0.4*b.score*((sum(coriscore)-sum(rmin))/(sum(rmax)-sum(rmin))))/1.4 as nrscore \n" +
                        " from metasearchterms a, metasearchdocs b where b.collectionid=a.collectionid\n" +
                        " and a.id in ( " +substr+ " )  group by collid,docurl,b.score order by nrscore desc limit " +input_k;
            else
                querystr=" select b.collectionid as collid,docurl, (b.score+0.4*b.score*((sum(coriscore)-sum(rmin))/(sum(rmax)-sum(rmin))))/1.4 as nrscore \n" +
                        " from metasearchterms a, metasearchdocs b where b.collectionid=a.collectionid\n" +
                        " and a.id in ( " +substr+ " )  group by collid,docurl,b.score order by nrscore desc";
            System.out.println("flag  0 query :"+querystr);
            rs = stmt.executeQuery(querystr);
            int i = 1;
            System.out.println("Search Results:  ");
            while (rs.next()) {
                System.out.println("*******************************");
                System.out.println("Results :  ");
                System.out.println("collection id : " + rs.getInt("collid"));
                System.out.println("url : " + rs.getString("docurl"));
                System.out.println("rank : " + i);
                System.out.println("score : " + rs.getDouble("nrscore"));
                System.out.println("*******************************");
                MetaPOJO fetchedResultData = new MetaPOJO();
                fetchedResultData.setMetaVmId(rs.getInt("collid"));
                fetchedResultData.setMetaScore(rs.getDouble("nrscore"));
                fetchedResultData.setMetaRank(i);
                fetchedResultData.setMetaUrl(rs.getString("docurl"));
                listjson.add(fetchedResultData);
                i++;
            }
            servletpojo.setMetaResultData(listjson);
            clearDocTb();
            return servletpojo;
        }

    }

    public void fetchStatistics(int flag) throws Exception{
        metaSearch(queryrequest,input_k,model,flag);
        urlList = new LinkedList<String>(Arrays.asList(metaURLs));
        MetaSearchQP crl = new MetaSearchQP(urlList);
        for(int i=1;i<=metaURLs.length;i++)
        {
            Thread threadObj = new Thread(crl,"thread_"+i);
            threadPool.add(threadObj);
            System.out.println("thread :"+ threadObj + " created");
            threadObj.start();
        }

        for(Thread t : threadPool) {
            // waits for this thread to die
            t.join();
        }
        if(flag == 0)
            computeCORIScore();
    }

    private List<Integer> getSearchEng() throws Exception {
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        String substr = "SELECT id  FROM metasearchterms where ";
        for (int i=0;i<query_terms.length;i++) {
            if (i != (query_terms.length - 1))
                substr = substr.concat("term like '"+query_terms[i]+ "' OR ");
            else
                substr = substr.concat("term like '"+query_terms[i] + "'");
        }
        List<Integer> idcol = new ArrayList<Integer>();
        String mainquery= "select a.id as collid, sum(coriscore) as sum from metaurls a, metasearchterms b where b.collectionid=a.id " +
                "and b.id in ( "+ substr + ") and a.state=1 group by collid order by sum desc limit "+ searcheng_lmt;
        ResultSet resultSet = stmt.executeQuery(mainquery);
        while (resultSet.next()){
            idcol.add(resultSet.getInt("collid"));
        }
        conn.close();
        stmt.close();
        return idcol;
    }

    public Servletpojo fetchdocs() throws Exception
    {
        System.out.println("inside fetchdocs");
        int flag=checkForStatistics();
        System.out.println("flag :"+ flag);
        if(flag!=0){
            fetchStatistics(flag);
            System.out.println("collected statistics");
        }
        else{
            System.out.println("statistics already found");
            srceng_ids=getSearchEng();
            fetchStatistics(flag);
            System.out.println("srceng_ids : "+ Arrays.toString(srceng_ids.toArray()));
        }
        Servletpojo finaljson = getBestDocs(flag);
        return finaljson;

    }

    public void metaSearch(String queryrequest, String input_k,String model,int flag)throws Exception{
        queryrequest=queryrequest.replaceAll(" ","%20");
        System.out.println(queryrequest+input_k+model);
        List<String> metsurlsList = new ArrayList<>();
        String query = queryrequest+"&"+"k="+input_k+"&score="+model;
        Connection conn = DBconnection.getCon(dbcon,true);
        Statement stmt = conn.createStatement();
        String urls=null;
        if(flag == 0)
        {
            String ids="";
            for(int i=0;i<srceng_ids.size();i++) {
                if (i != (srceng_ids.size() - 1))
                    ids= ids.concat(String.valueOf(srceng_ids.get(i))+",");
                else
                    ids= ids.concat(String.valueOf(srceng_ids.get(i)));
            }
            urls = "SELECT metaurl FROM metaurls where id in ( "+ ids + " )";
        }
        else
            urls = "SELECT metaurl FROM metaurls where state=1";
        ResultSet resultSet = stmt.executeQuery(urls);
        while (resultSet.next()){
            metsurlsList.add(resultSet.getString(1).concat(query));
        }
        // metaURLs = metsurlsList;
        conn.close();
        stmt.close();
//        System.out.println(metsurlsList);
        metaURLs = metsurlsList.toArray(new String[0]);
    }
}
