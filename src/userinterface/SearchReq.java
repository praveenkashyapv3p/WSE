package userinterface;

import db.DBTables;
import queryprocessing.ProcessQuery;
import servlet.Servletpojo;

import java.util.Scanner;


/**
 * SearchReq.java - a java class that gets the input from the user identifies the various operations and calls the ProcessQuery methods.
 *
 * @author Vishnu Narayanan Surendran
 * @version 1.0
 */
public class SearchReq {

    /**
     * method gets the input parameters from the user in the command line and returns the documents satisfying the search parameters
     *
     * @param args: no input
     */
    public static void main(String args[]) {
        SearchReq srObj = new SearchReq();
        try {
            DBTables dbTables = new DBTables();
            dbTables.createViews();
        }catch (Exception e){

        }

        Scanner reader = new Scanner(System.in);  // Reading from System.in
        System.out.println("Enter the search query: ");
        String searchQuery = reader.nextLine();
        System.out.println("value of searchQuery :" + searchQuery);
        String siteOp = srObj.processSearchString(searchQuery);
        System.out.println("Enter the scoring model\n Enter 1 for tfidf, 2 for bm25 and 3 for page rank and bm25 combined scoring model");
        int model = reader.nextInt();
        System.out.println("value of Score model : "+ model);
        System.out.println("Enter the total result documents limit: ");
        int kVal = reader.nextInt();
        System.out.println("value of kVal :" + kVal);
        System.out.println("Enter preferred language(English/German");
        String lang = reader.next();
        System.out.println("value of Language : "+ lang);
        ProcessQuery pqObj = new ProcessQuery();
//        Servletpojo finaljson = pqObj.fetchDocs(searchQuery, siteOp, kVal,lang,model);
        Servletpojo imgjson = pqObj.fetchImgDocs(searchQuery, siteOp, kVal,lang);
        reader.close();
    }

    /**
     * method gets the user input query and extracts the value of the site operator
     *
     * @param qry : the user search query
     * @return: returns the site operator value
     */
    public String processSearchString(String qry) {
        System.out.println("qry before replace :" + qry);
        String siteValue = null;
        int siteValueLmt;
        if (qry.toLowerCase().contains("site:".toLowerCase())) {
            System.out.println("siteValueLmt before :" + qry.substring(qry.indexOf("site:")).indexOf(' '));
            siteValueLmt = qry.substring(qry.indexOf("site:")).indexOf(' ');
            if (siteValueLmt != -1)
                siteValueLmt = qry.indexOf("site:") + qry.substring(qry.indexOf("site:")).indexOf(' ') + 1;
            else
                siteValueLmt = qry.length();
            System.out.println("siteValueLmt :" + siteValueLmt);
            siteValue = qry.substring(qry.indexOf("site:") + 5, siteValueLmt);
        }
        System.out.println("siteValue: " + siteValue);
        System.out.println("site:" + siteValue);
        return siteValue;
    }

    /**
     * method process requests from the HTML interface and returns the documents matching the input search parameters
     *
     * @param searchqry : gets the search query
     * @param kVal      : gets the limit for the result set
     * @return
     */
    public Servletpojo userSearch(String searchqry, int kVal, String language, int model) throws Exception {
        System.out.println("userSearch");
        DBTables dbTables = new DBTables();
        dbTables.createViews();
        String siteOp = processSearchString(searchqry);
        ProcessQuery pqObj = new ProcessQuery();
        Servletpojo finaljson = pqObj.fetchDocs(searchqry, siteOp, kVal,language,model);
        return finaljson;

    }

    /**
     * method process requests from the HTML interface and returns the documents matching the input search parameters
     *
     * @param searchqry : gets the search query
     * @param kVal      : gets the limit for the result set
     * @return
     */
    public Servletpojo imgSearch(String searchqry, int kVal, String language) throws Exception {
        System.out.println("imgSearch");
        String siteOp = processSearchString(searchqry);
        ProcessQuery pqObj = new ProcessQuery();
        Servletpojo finaljson = pqObj.fetchImgDocs(searchqry, siteOp, kVal,language);
        return finaljson;

    }
}
