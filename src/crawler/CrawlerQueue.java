package crawler;


import db.DBconnection;
import literals.Literals;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.*;


/**
 * CrawlerQueue.java - a java class that maintains the queue for crawling operation.
 * @author  Vishnu Narayanan Surendran
 * @version 1.0
 */

public class CrawlerQueue {

    //function gets the first record from the queue and updates its state to processing

    /**
     * getURLFromQueue(): gets the first record from the queue that is yet to be processed
     * and is of the current depth of url's that are being processed and updates its state to 'processing'
     * @param conn : connection object
     * @param currLevel: the current level that is being processed
     * @return: returns the url
     */
    public static synchronized String[] getURLFromQueue(Connection conn,int currLevel)
    {
        try{
//            System.out.println("Inside getURLFromQueue method" + "\n");
            Statement stmt = conn.createStatement();
            String[] urlDet = new String[4];
            int id=0;
            ResultSet rs = stmt.executeQuery("SELECT id,url,level,parent_doc FROM crawlqueue where crawl_status  like 'initial' and level <=" + currLevel + "order by id,level LIMIT 1 ");
            while (rs.next()) {
                id=rs.getInt("id");
                urlDet[0] =  Integer.toString(id);
                urlDet[1] =  rs.getString("url");
                urlDet[2] =  Integer.toString(rs.getInt("level"));
                urlDet[3] =  Integer.toString(rs.getInt("parent_doc"));
            }
            if(id != 0) {
                stmt.executeUpdate("UPDATE crawlqueue set crawl_status = 'processing' where url ='" + urlDet[1]+"'");
//                System.out.println("Fetched record and status updated. Exiting getURLFromQueue method" + "\n");
            }
            else
                System.out.println("No records with level less than "+ currLevel + " and status as initial" + "\n");
//            System.out.println("getURLFromQueue return value:"+ urlDet[1]);
            return urlDet;
        }
        catch (SQLException e) {
            System.out.println("Catch block: getURLFromQueue");
//            e.printStackTrace();
//            System.out.println("getURLFromQueue return value:"+ null);
            return null;
        }

    }

    private int checkAlreadyProcessed(Connection conn,String url)
    {
        try{
            int id=0;
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id FROM crawlqueue where ( crawl_status  like 'crawled' OR crawl_status like 'processing') and url = '"+ url +"'");
            while (rs.next()) {
                id=rs.getInt("id");
            }
            return id;

        }catch (SQLException e) {
            System.out.println("Catch block: getURLFromQueue");
//            e.printStackTrace();
//            System.out.println("getURLFromQueue return value:"+ null);
            return 0;
        }

    }

    /**
     *method computes the current level that is being processed or yet to be processed
     * @param conn : takes the connection object
     * @return: return the level value
     */
    public  int currLevel(Connection conn) {
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT min(level) as level FROM crawlqueue where crawl_status = 'initial' OR crawl_status='processing'");
            int minLevel=0;
            while (rs.next()) {
                minLevel = rs.getInt("level");
                System.out.println("Minimum level in queue:" + minLevel);
            }
            return minLevel;
        } catch (SQLException e) {
            System.out.println("Catch block: minLevel");
//            e.printStackTrace();
            return 0;
        }
    }

    /**
     * method rollsback the status of a URL back to initial
     * @param conn : connection object
     * @param id : id of the entry that needs to be rolled back
     */
    public  void rollbackQueueStatus(Connection conn, int id) {
//        System.out.println("inside rollbackQueueStatus method");
        try
        {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE crawlqueue set crawl_status = 'initial' where id = " + id);
//            System.out.println("Rollbacked status in queue");

        } catch (SQLException e) {
            System.out.println("Catch block: rollbackQueueStatus");
//            e.printStackTrace();
        }
    }

    /**
     * method checks if the all records within the specified depth has been crawled
     * @param conn
     * @param depth
     * @return
     */

    public  boolean isQueueProcessed(Connection conn, int depth) {
//        System.out.println("inside isQueueProcessed method");
        try
        {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) as count FROM crawlqueue " +
                    "where crawl_status  like 'initial' OR (crawl_status  like 'processing' and level < "+ depth+")");
            int count=0;
            while (rs.next()) {
                count=rs.getInt("count");
            }
//            System.out.println("isQueueProcessed processed. Count value :" + count);
            if(count!=0)
                return false;
            else
                return true;

        } catch (SQLException e) {
            System.out.println("Catch block: isQueueProcessed");
//            e.printStackTrace();
            return false;
        }
    }

    /**
     * check if the crawlqueue is empty
     * @param  : connection object
     * @return
     */
    public boolean isQueueEmpty(){
        try
        {
            Connection conn = DBconnection.getCon(Literals.dbcon, true);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT count(*) as count FROM crawlqueue");
            int count=0;
            while (rs.next()) {
                count=rs.getInt("count");
            }
//            System.out.println("isQueueProcessed processed. Count value :" + count);
            conn.close();
            stmt.close();
            if(count>0)
                return false;
            else
                return true;

        } catch (Exception e) {
            System.out.println("Catch block: isQueueProcessed");
//            e.printStackTrace();
            return false;
        }
    }

    /**
     * method inserts the valid outgoing URLs to the queue
     * @param conn : Connection objects
     * @param atags : node list of outgoing url's
     * @param domainName : domain name of the url
     * @param id : id of the url
     * @param level : level of the url
     * @param url : the url name
     * @param flag : flag which indicates whether the crawler is allowed to move out of the current domain
     */

    public void insertIntoQueue(Connection conn, NodeList atags,String domainName, int id,int level,String url,boolean flag) {
//        System.out.println("inside insertIntoQueue method");
        try {
            String querystr = "INSERT INTO crawlqueue(url,crawl_status,level,parent_doc) VALUES (?,?,?,?)";
            PreparedStatement pstmt = conn.prepareStatement(querystr);
            for (int i = 0; i < atags.getLength(); i++) {
                Node n = atags.item(i);
//                System.out.println("***Actual url****");
//                System.out.println(n.getNodeValue());
                if (!(n.getNodeValue().startsWith("http") ||n.getNodeValue().toLowerCase().contains("www.")|| n.getNodeValue().startsWith("www") || n.getNodeValue() == null || n.getNodeValue().isEmpty()) && n.getNodeValue().startsWith("/")) {
                    n.setNodeValue("http://www." + domainName + n.getNodeValue());
                }
                boolean isValid = isValidURL(n.getNodeValue());
//                System.out.println("***Validity_check**** : "+isValid);
                int newLevel = level + 1;
                if (isValid) {
                    if (!flag) {
                        boolean domainCheck = checkDomain(n.getNodeValue(), domainName);
                        if (!domainCheck)
                            continue;

                    }
//                    System.out.println("***modified url****");
//                    System.out.println(n.getNodeValue());
//                    System.out.println(n.getNodeValue());
                    if (!n.getNodeValue().equals(url))
                    {
                        int checkid=checkAlreadyProcessed(conn,n.getNodeValue());
                        if(checkid !=0 ) {
                            insertIntoLinkstb(conn,id,checkid);
                            continue;
                        }
                        pstmt.setString(1,n.getNodeValue());
                        pstmt.setString(2,"initial");
                        pstmt.setInt(3, newLevel);
                        pstmt.setInt(4, id);
                        pstmt.addBatch();
                    }
                }

            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            System.out.println("Catch block: insertIntoQueue");
//            e.printStackTrace();
        }
//        System.out.println("out of insertIntoQueue method");
    }

//    /**
//     * method inserts the crawled URL's into the documents table
//     * @param conn Connection Object
//     * @param id : id of the url
//     * @param url: url value
//     * @param date : current date value
//     */
//
//    public void insertIntoDoctb(Connection conn,int id, String url,  Date date, String lang,String content) {
////        System.out.println("inside insertIntoDoctb method");
////        System.out.println("Paramters : id ->" + id + "url->" +url + " date ->"+date);
//        try
//        {
//            Statement stmt = conn.createStatement();
//            stmt.executeUpdate("INSERT into documents(docid,url,crawled_on_date,lang) VALUES (" + id + ",'" + url + "','" + date + "','" + lang + "')");
////            System.out.println("Inserted record into documents table");
//
//        } catch (SQLException e) {
////            System.out.println("Catch block: insertIntoDoctb");
////            e.printStackTrace();
//        }
//    }

    /**
     * method updates the status of the url's that are crawled from initial to crawled
     * @param conn : Connection Object
     * @param url : url value
     */
    public void updateQueue(Connection conn, String url)
    {
//        System.out.println("inside updateQueue method");
        try
        {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("UPDATE crawlqueue set crawl_status = 'crawled' where url like '"+ url + "'");
//            System.out.println("Updated status in queue");

        } catch (SQLException e) {
            System.out.println("Catch block: updateQueue");
//            e.printStackTrace();
        }
    }

    /**
     * method inserts the link structure into the links table
     * @param conn : Connection Object
     * @param id : id of the url
     * @param parent : parent id of the url
     */

    public void insertIntoLinkstb(Connection conn, int id, int parent)
    {
//        System.out.println("inside insertIntoLinkstb method");
        try
        {
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("INSERT into links(from_docid,to_docid) VALUES (" + parent +","+ id+")");
//            System.out.println("Inserted record into links table");

        } catch (SQLException e) {
            System.out.println("Catch block: insertIntoLinkstb");
//            e.printStackTrace();
        }
    }

    /**
     * method checks if the domain of the outgoing URL matches with its parent URL
     * @param url : url value
     * @param parentDomain : domain of the parent
     * @return: returns a boolean output
     */
    public boolean checkDomain(String url,String parentDomain)
    {
//        System.out.println("inside checkDomain method");
        try {
            URI uri = new URI(url);
//            System.out.println("url : "+ url);
            String domain = uri.getHost();
            domain= domain.startsWith("www.") ? domain.substring(4) : domain;
//            System.out.println("domain : "+ domain);
//            System.out.println("parentDomain: "+ parentDomain);
            if(parentDomain.equals(domain)) {
//                System.out.println("output: "+ true);
                return true;
            }

            else {
//                System.out.println("output: "+ false);
                return false;
            }
        }catch (URISyntaxException e)
        {
//            e.printStackTrace();
            return false;
        }
    }

    /**
     * method checks if the url is valid or not
     * @param url : url value
     * @return: returns a boolean output
     */
    public boolean isValidURL(String url)
    {
//        System.out.println("inside isValidURL method");
        try {
            if(url.startsWith("mailto:"))
                return false;
            else{
                new URL(url).toURI();
                return true;
            }
        }catch (URISyntaxException exception) {
            return false;
        }

        catch (MalformedURLException exception) {
            return false;
        }
    }



}
