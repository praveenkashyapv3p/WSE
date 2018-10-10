package crawler;

import algorithms.*;
import db.DBTables;
import db.DBconnection;
import indexer.HTMLStrip;
import literals.Literals;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.tidy.Tidy;

import javax.xml.xpath.*;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Crawler.java - a java class that  crawls the urls, calls the indexer and store the outgoing urls in a multithreaded fashion making use of the CrawlerQueue methods.
 * @author  Vishnu Narayanan Surendran
 * @version 1.0
 */
public class Crawler implements Runnable {

    public static int depthToCrawl = 1;
    public static int docsToCrawl = 2;
    public static int docCount=0;
    public static boolean chgDomain = true;
    public static int threadCount=5;
    public static List<Thread> threadPool = new ArrayList<Thread>();


    /**
     * beginCrawler() : creates a new thread and sets the below parameters
     * @param depthLim : set the limit for the number of depths to crawl
     * @param docLim : set the limit for the number of documents to crawl
     * @param flag : set flag to true to crawl pages from different domains
     */

    public Crawler(int depthLim, int docLim, boolean flag)
    {
        depthToCrawl = depthLim;
        docsToCrawl = docLim;
        chgDomain = flag;
    }

    /**
     * fetchHTML() : fetches the raw html content from the url given as input
     * @param urlname : conatins the url fetched from the Crawl Queue
     * @return : returns the raw html content of the given url
     */

    private String fetchHTML(String urlname) {
//        System.out.println("Inside fetchHTML method" + "\n");
        try {
            URL url = new URL(urlname);
            // open the stream and put it into BufferedReader
            BufferedReader breader = new BufferedReader(
                    new InputStreamReader(url.openStream()));
            StringBuffer urlcnt = new StringBuffer();
            String htmlCnt;
            while ((htmlCnt =
                    breader.readLine()) != null) {
                urlcnt.append(htmlCnt);
            }
            breader.close();
//            System.out.println("getting html content Done");
            return urlcnt.toString();

        } catch (MalformedURLException e) {
//            e.printStackTrace();
            System.out.println("Handled exception");
            return null;
        } catch (IOException e) {
//            e.printStackTrace();
            System.out.println("Handled exception");
            return null;
        }

    }

    /**
     * getTidyObject(): creates a new Tidy object and sets the various parameters
     * @return: returns the tidy object
     */

    private static Tidy getTidyObject()
    {
        Tidy tidyObj= new Tidy();
        tidyObj.setXHTML(true);
        tidyObj.setQuiet(true);
        tidyObj.setDropEmptyParas(true);
        tidyObj.setShowWarnings(false); //to hide errors
        tidyObj.setQuiet(true); //to hide warning
        tidyObj.setInputEncoding("UTF-8");
        tidyObj.setOutputEncoding("UTF-8");
        tidyObj.setMakeClean(true);
        tidyObj.setSmartIndent(true);
        tidyObj.setShowErrors(0);
        return tidyObj;
    }

    /**
     * htmlToXhtml(): coverts the raw html to xhtml
     * @param rawHtml: contains the raw html content that needs to be converted to xhtml
     * @return : returns the xhtml document
     */

    private Document htmlToXhtml(String rawHtml)
    {
        Tidy tidy= getTidyObject();
        Document xhtmlDoc=null;
        ByteArrayInputStream data = new ByteArrayInputStream(rawHtml.getBytes());
        xhtmlDoc = tidy.parseDOM(data, null);
        return xhtmlDoc;
    }

    /**
     * getOutgoingURLs(): fetches the outgoing url's from a given xhtml document using XPath expressions
     * @param docXHtml : contains the xhtml document from which the outgoing url's needs to be fetched
     * @return : returns a list of the outgoing url's
     */

    private NodeList getOutgoingURLs(Document docXHtml)
    {
        XPathFactory xpathFact = XPathFactory.newInstance();
        XPath xpath = xpathFact.newXPath();
        NodeList urlList = null;
        try {
            XPathExpression expression = xpath.compile("//a/@href");
            urlList = (NodeList)expression.evaluate(docXHtml, XPathConstants.NODESET);
            return urlList;
        }
        catch (XPathExpressionException e) {
//            e.printStackTrace();
            return urlList;
        }

    }

    /**
     * this mehtod fetched the image tags from the web page
     * @param docXHtml
     * @return
     */
    private NodeList getImages(Document docXHtml)
    {
        XPathFactory xpathFact = XPathFactory.newInstance();
        XPath xpath = xpathFact.newXPath();
        NodeList imgList = null;
        try {
            XPathExpression expression = xpath.compile("//img");
            imgList = (NodeList)expression.evaluate(docXHtml, XPathConstants.NODESET);
            return imgList;
        }
        catch (XPathExpressionException e) {
//            e.printStackTrace();
            return imgList;
        }

    }

    /**
     * getDomain(): fetches the domain name from a given url
     * @param url: contains the url from which the domain has to be fetched
     * @return: returns the domain name
     */
    private String getDomain(String url){
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            return domain.startsWith("www.") ? domain.substring(4) : domain;
        }catch (URISyntaxException e)
        {
//            e.printStackTrace();
            return null;
        }
    }

    /**
     * Each thread picks up a url from the crawl Queue satisfying all the conditions (depth limit, document count, domain change allowed?).
     * fetches the html content from the url, calls the indexer and extracts the outgoing urls from the page
     */

    public void run()
    {
        CrawlerQueue crawlQueue = new CrawlerQueue();
        String url;
        int level;
        int parent;
        int id;
        Connection conn = null;
        try {
            conn = DBconnection.getCon(Literals.dbcon, true);
        }catch(Exception e){
//            e.printStackTrace();
        }
        try {
            while (checkdocLimit()) {
                docCount += 1;
                int currLevel = crawlQueue.currLevel(conn);
//                System.out.println("minLevel" + currLevel);
                String[] urlDet = crawlQueue.getURLFromQueue(conn, currLevel);
                if (urlDet[1] != null) {
                    url = urlDet[1];
                    id = Integer.parseInt(urlDet[0]);
                    parent = Integer.parseInt(urlDet[3]);
                    level = Integer.parseInt(urlDet[2]);
                    System.out.println("Thread named: " + Thread.currentThread().getName() + "has picked up URL: " + url);
//                    System.out.println("docCount is" + docCount);
//                    System.out.println("**[*******domainname******** for :" + url);
                    String domainName = getDomain(url);
//                    System.out.println(domainName);
//                    System.out.println("*********domainname********");
//                    System.out.println("*********level for******** for :" + url + "is :" + level);
//                    System.out.println("*********id for******** for :" + url + "is :" + id);
//                    System.out.println("*********parent for******** for :" + url + "is :" + parent);
                    if (level > depthToCrawl) {
                        docCount -= 1;
//                        System.out.println(" url level : " + level + "depth to crawl limit" + depthToCrawl);
                        crawlQueue.rollbackQueueStatus(conn, id);
                        break;
                    } else {
                        if(isValidHTML(url)) {
                            String webContent = fetchHTML(url);
                            if (webContent != null) {
                                int imgcount=0;
                                System.out.println(webContent);
                                Document xhtmlDoc = htmlToXhtml(webContent);
                                String title=xhtmlDoc.getElementsByTagName("title").item(0).getFirstChild().getNodeValue();
                                System.out.println("title: "+title);
                                NodeList atags = getOutgoingURLs(xhtmlDoc);
                                NodeList imgtags = getImages(xhtmlDoc);
                                String imgTaggedCont=webContent;
                                Pattern pt = Pattern.compile("<img([\\w\\W]+?)>");
                                while(pt.matcher(imgTaggedCont).find())
                                {
                                    imgTaggedCont=imgTaggedCont.replaceFirst("<img([\\w\\W]+?)>"," tagpictures-"+imgcount+" ");
                                    imgcount++;
                                }
                                HTMLStrip hreader = new HTMLStrip();
                                try {
                                    hreader.getSource(imgTaggedCont,id,imgtags,imgcount,domainName,title,url);
                                } catch (Exception e) {
                                    System.out.println("Catch block: calling hreader.getSource()");
//                                    e.printStackTrace();
                                }
//                                String lang = hreader.getLang();
//                              System.out.println(webContent);
                                crawlQueue.insertIntoQueue(conn, atags, domainName, id, level, url, chgDomain);
//                                crawlQueue.insertIntoDoctb(conn, id, url, new Date(),lang);
                                crawlQueue.updateQueue(conn, url);
                                if (parent != 0)
                                    crawlQueue.insertIntoLinkstb(conn, id, parent);
                                System.out.println("URL: " + url + " crawled successfully by thread [" + Thread.currentThread().getName() + "]");
                                Thread.sleep(1000);
                            } else {
                                System.out.println("Invalid Url caught");
                                crawlQueue.updateQueue(conn, url);
                                docCount -= 1;
                            }
                        }else{
                            System.out.println("Not a html document");
                            crawlQueue.updateQueue(conn, url);
                            docCount -= 1;
                        }
                    }
                } else {
                    System.out.println("empty url");
                    docCount -= 1;
                    boolean isComplete = crawlQueue.isQueueProcessed(conn,depthToCrawl);
                    if (isComplete)
                        break;
                    Thread.sleep(1000);
                }
                System.out.println("doc_count"+docCount);
            }

        }
        catch(InterruptedException e)
        {
            System.out.println("Thread named : " + Thread.currentThread().getName() +" interrupted");
        }
        System.out.println("Thread named: " + Thread.currentThread().getName() + "run is over" );
        try{
            conn.close();
            System.out.println("Thread named: " + Thread.currentThread().getName() + "run is over" );
        }catch(SQLException e){
//            e.printStackTrace();
        }
    }

    /**
     * method computes the tdidf score
     */
    private void computeTFIDF() {
        try {
            System.out.println("tfidf calcualtion is starting");
            Tfidf tfidf = new Tfidf();
            tfidf.tfidf();
            System.out.println("tfidf calcualtion is over");
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    /**
     * this method computes the Page Rank scores
     */

    private void computePageRank() {
        try {
            System.out.println("Page Rank calculation is starting");
            PageRank pr= new PageRank();
            pr.calculatePageRank();
            System.out.println("Page Rank calcualtion is over");
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    /**
     * this method computes the Okapi scores
     */

    private void computeOkapiScore() {
        try {
            System.out.println("Okapi BM25 calculation is starting");
            Okapi_BM25 okapi= new Okapi_BM25();
            okapi.computeOkapi_BM25();
            System.out.println("Okapi BM25  calcualtion is over");
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    /**
     *this method computes the combined page rank and okapi scores
     */

    private void computePgrnk_BM25() {
        try {
            System.out.println("Page Rank and Okapi BM25 combined score calculation is starting");
            Pgrnk_BM25 cmbrscr= new Pgrnk_BM25();
            cmbrscr.computePgrn_BM25();
            System.out.println("Page Rank and Okapi BM25 combined score  calcualtion is over");
        } catch (Exception e) {
//            e.printStackTrace();
        }
    }

    /**
     * method validates is a given url contains a html document by checking the header of the page
     * @param urlname: gets the urlname as String
     * @return : returns a boolean status
     */
    private boolean isValidHTML(String urlname){
        try{
            URL url = new URL(urlname);
            HttpURLConnection verifyhtml = (HttpURLConnection)url.openConnection();
            verifyhtml.setAllowUserInteraction( false );
            verifyhtml.setDoInput( true );
            verifyhtml.setDoOutput( false );
            verifyhtml.setUseCaches( true );
            verifyhtml.setRequestMethod("HEAD");
            verifyhtml.connect();
            String mime = verifyhtml.getContentType();
            if(mime.toLowerCase().contains("text/html")){
                System.out.println("Value:True, mime value : " + mime);
                return true;
            }
            else{
                System.out.println("Value:False, mime value : " + mime);
                return false;
            }
        } catch (MalformedURLException e) {
//            e.printStackTrace();
            return false;
        } catch (IOException e) {
//            e.printStackTrace();
            return false;
        }
    }

    /**
     *method checks if the crawled documents are with in the limit specified
     * @return boolean status
     */

    private static synchronized boolean checkdocLimit(){

        if(docCount < docsToCrawl)
            return true;
        else
            return false;

    }

    /**
     * initialise the tables, drop indexes,
     * call the threads in a loop, call the computeTFIDF method once all threads complete
     * and finally create indexes for the tables
     * @param args : takes no input
     * @throws Exception
     */
    public static void main(String args[]) throws Exception {
        Crawler crl= new Crawler(5, 100,false);
        CrawlerQueue crawlQueue = new CrawlerQueue();
        DBTables table = new DBTables();
        String[] startURL= {"https://www.cricbuzz.com/"};
        table.createTables();
        table.dropIndexes();
        if(crawlQueue.isQueueEmpty())
            table.initializeQueue(startURL);
        System.out.println("Dropped Indexes");
        for(int i=1;i<=threadCount;i++)
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
        crl.computeTFIDF();
        crl.computePageRank();
        crl.computeOkapiScore();
        crl.computePgrnk_BM25();
        Shingle shingle = new Shingle();
        shingle.updateShingle();
        shingle.createShingleSimilarity();

        System.out.println("Crawling complete");
        table.createIndexes();
        System.out.println("Created Indexes");
    }

}


