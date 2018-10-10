package indexer;

import algorithms.Shingle;
import db.DBconnection;
import literals.Literals;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


public class HTMLStrip extends Literals {
    //static Map<String, Long> collect = new HashMap<>();

    private String contents = "";
    private String title;
    private String docurl;
    private int docid;
    String language = "";
    NodeList imgtags;
    private int imgtag_count;
    String domainName;

    /**
     * Removes stopwords from the HTML content afte all tags have been stripped
     * @throws Exception
     */
    public void stopWordRemoval() throws Exception {
        List<String> stp_words_removed;
        removeSpecial();
        String img_content[] = contents.toLowerCase().replaceAll("\\s{2,}", " ").trim().split(" ");
        String cnt=contents.toLowerCase().replaceAll("\\s{2,}", " ").replaceAll("\\btagpictures.*?\\b", "");
        String plain_content[] = contents.toLowerCase().replaceAll("\\s{2,}", " ").replaceAll("'","''").trim().split(" ");
        insertIntoContent(plain_content);
        String stopWordsRemoved = String.join(",", plain_content);
        String imgContent = String.join(",", img_content);
        stopWordsRemoved = stopWordsRemoved.replaceAll("[^a-zA-Z0-9 ]"," ").replaceAll("\\s{2,}"," ");
        imgContent = imgContent.replaceAll("[^a-zA-Z0-9 ]"," ").replaceAll("\\s{2,}"," ");
        plain_content = stopWordsRemoved.split(" ");
        img_content = imgContent.split(" ");
        List<String> list_of_terms = Arrays.asList(plain_content);
        List<String> imglist_of_terms = Arrays.asList(img_content);
        imglist_of_terms = imglist_of_terms.stream().map(String::toLowerCase).collect(Collectors.toList());
        stp_words_removed = list_of_terms.stream().map(String::toLowerCase).collect(Collectors.toList());
        Shingle shingle = new Shingle();
        shingle.shingling(docid,stp_words_removed);

        List stopWordsList = stopWords();
        String lang=detectLanguage(stp_words_removed, stopWordsList);
        if (imgtag_count>0)
            processImage(imglist_of_terms,lang);
        insertIntoDoctb(cnt,lang);
        List stopWords = stopWordList();
        for (int i = 0; i < stp_words_removed.size(); i++) {
            for (int j = 0; j < stopWords.size(); j++) {
                if (stp_words_removed.contains(stopWords.get(j))) {
                    stp_words_removed.remove(stopWords.get(j));
                }
            }
        }
        if (language == "en")
            stemming(stp_words_removed);
        else
            ge_term_frequency(stp_words_removed);
    }

    /**
     * insert full content of document and its title into content table w.r.t document id
     * @throws Exception
     */
    private void insertIntoContent(String[] rawcontent) throws Exception{
        String str = String.join(" ", rawcontent);
        String contentInsert = "INSERT INTO content(docid,title,fullcontent) VALUES("+docid+",'"+title+"','"+str+"');";
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        stmt.executeUpdate(contentInsert);
        stmt.close();
        conn.close();
    }

    /**
     * method inserts the crawled URL's into the documents table along with the content
     */

    public void insertIntoDoctb(String cnt,String lang) {
//        System.out.println("inside insertIntoDoctb method");
//        System.out.println("Paramters : id ->" + id + "url->" +url + " date ->"+date);
        cnt = cnt.replaceAll("'","");
        try
        {
            Connection conn = DBconnection.getCon(dbcon, true);
            Statement stmt = conn.createStatement();
            stmt.executeUpdate("INSERT into documents(docid,url,crawled_on_date,lang,content) VALUES (" + docid + ",'" + docurl + "','" + new Date() + "','" + lang + "','" + cnt + "')");
//            System.out.println("Inserted record into documents table");
            conn.close();
            stmt.close();

        } catch (Exception e) {
//            System.out.println("Catch block: insertIntoDoctb");
//            e.printStackTrace();
        }
    }


    public void processImage(List<String> imgterms,String lang) throws Exception
    {
        int[] img_ids=storeImageData(lang);
        String[][] before_image_terms= new String[imgtag_count][window];
        String[][] after_image_terms= new String[imgtag_count][window];
        Double[][] afimage_term_scores= new Double[imgtag_count][window];
        Double[][] bfimage_term_scores= new Double[imgtag_count][window];
        int size=imgterms.size();
        for(int i=0;i<imgtag_count;i++)
        {
            int index=imgterms.indexOf("tagpictures"+i);
            if(index != -1)
            {
                int befpos=index-1;
                int aftpos=index+1;
                for(int j=0;j<window;j++)
                {
                    if(!(befpos < 0))
                    {
                        String val= imgterms.get(befpos);
                        while((val.contains("tagpictures")))
                        {
                            befpos--;
                            val= imgterms.get(befpos);
                        }
                        before_image_terms[i][j] = val;
                        bfimage_term_scores[i][j]= lambda * Math.pow(2.718281828459045,-(lambda * (j+1)));
                        befpos--;
                    }
                    if(!(aftpos >= size))
                    {
                        String aftval= imgterms.get(aftpos);
                        while((aftval.contains("tagpictures")))
                        {
                            aftpos++;
                            aftval= imgterms.get(aftpos);
                        }
                        after_image_terms[i][j] = aftval;
                        afimage_term_scores[i][j]= lambda * Math.pow(2.718281828459045,-(lambda * (j+1)));
                        aftpos++;
                    }
                }
            }
        }
        storeImageFeatures(before_image_terms,after_image_terms,afimage_term_scores,bfimage_term_scores,img_ids);

    }

    public void storeImageFeatures(String[][] bfi_terms, String[][] afi_terms, Double[][] afi_ts, Double[][] bfi_ts,int[] imgids) throws Exception
    {
        Connection conn = DBconnection.getCon(dbcon, true);
        String querystr = "INSERT INTO img_features(imgid,terms,imgscore) VALUES (?,?,?)";
        PreparedStatement pstmt = conn.prepareStatement(querystr);
        for(int i=0;i<imgids.length;i++)
        {
            for(int j=0;j<window;j++)
            {
                Stemmer stemmer = new Stemmer();
                char[] splitChar = bfi_terms[i][j].trim().toCharArray();
                stemmer.add(splitChar, splitChar.length);
                stemmer.stem();
                String bfterm=stemmer.toString();
                splitChar = afi_terms[i][j].trim().toCharArray();
                stemmer.add(splitChar, splitChar.length);
                stemmer.stem();
                String afterm=stemmer.toString();
                pstmt.setInt(1,imgids[i]);
                pstmt.setString(2,bfterm);
                pstmt.setDouble(3, bfi_ts[i][j]);
                pstmt.addBatch();
                pstmt.setInt(1,imgids[i]);
                pstmt.setString(2,afterm);
                pstmt.setDouble(3, afi_ts[i][j]);
                pstmt.addBatch();
            }
        }
        pstmt.executeBatch();
        conn.close();
    }

    public int[] storeImageData(String lang) throws Exception{
        String url=null;
        String alt=null;
        int[] imgids= new int[imgtags.getLength()];
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        for (int i = 0; i < imgtags.getLength(); i++) {
            Node n = imgtags.item(i);
            Element e = (Element)n;
            if (!(e.getAttribute("src").startsWith("http") ||e.getAttribute("src").toLowerCase().contains("www.")||
                    e.getAttribute("src").startsWith("www") || e.getAttribute("src") == null || e.getAttribute("src").isEmpty())) {
                if(e.getAttribute("src").startsWith("/"))
                    url="http://www." + domainName + e.getAttribute("src");
                else
                    url="http://www." + domainName + "/"+ e.getAttribute("src");
            }
            if(!(e.getAttribute("alt") == null || e.getAttribute("alt").isEmpty()))
                alt=e.getAttribute("alt").replaceAll("'","");
            stmt.executeUpdate("INSERT into images(docid,imgurl,alt_tag,lang) VALUES (" + docid + ",'" + url + "','" + alt + "','" + lang + "')");
            ResultSet rs = stmt.executeQuery("SELECT imgid FROM images where imgurl like '"+ url + "'");
            while (rs.next()) {
                imgids[i] = rs.getInt("imgid");
            }
        }
        conn.close();
        stmt.close();
        return imgids;
    }

    /**
     *
     * @param termsList : list of words after stop words removal
     * @param bagowords : bag of words to detect language
     */
    public String detectLanguage(List<String> termsList, List bagowords) {
        List<String> temp = new ArrayList<>();
        for (int i = 0; i < termsList.size(); i++) {
            for (int j = 0; j < bagowords.size(); j++) {
                if (termsList.get(i).equalsIgnoreCase(bagowords.get(j).toString())) {
                    temp.add(termsList.get(i));
                }
            }
        }
        float threshold = (float) temp.size() / (float) termsList.size();
        if (threshold >= 0.1)
            language = "en";
        else
            language = "de";
        return language;
    }

    public String getLang() {
        return language;
    }

    /**
     * Does stemming for words in the English Documents after stop word removal
     * @throws Exception
     */
    public void stemming(List<String> stp_words_removed) throws Exception {
        List<String> stemmed_list = new ArrayList<>();
        for (String temp : stp_words_removed) {
            Stemmer stemmer = new Stemmer();
            char[] splitChar = temp.trim().toCharArray();
            stemmer.add(splitChar, splitChar.length);
            stemmer.stem();
            stemmed_list.add(stemmer.toString());
        }
        en_term_frequency(stp_words_removed,stemmed_list);
    }

    /**
     * Calculates f(t,d)
     * @throws Exception
     */
    private void ge_term_frequency(List<String> stp_words_removed) throws Exception {
        Map<String, Integer> collect = new HashMap<>();
            Set<String> unique = new HashSet<>(stp_words_removed);
            for (String key : unique) {
                collect.put(key,Collections.frequency(stp_words_removed, key));
            }
        insertIntoFeatures(collect);
        insertDictionary(stp_words_removed,stp_words_removed);
    }

    private void en_term_frequency(List<String> stp_words_removed,List<String> stemmedList) throws Exception {
        Map<String, Integer> collect = new HashMap<>();
        Set<String> unique = new HashSet<>(stp_words_removed);
        for (String key : unique) {
            collect.put(key,Collections.frequency(stp_words_removed, key));
        }
        insertIntoFeatures(collect);
        insertDictionary(stp_words_removed,stemmedList);
    }

    /**
     * insert stemmed words and their unstemmed pair into database
     * @throws Exception
     */
    public void insertDictionary(List<String> stp_words_removed,List<String> stemmedList) throws Exception{
        Object plainword;
        Object stemmedword;
        Iterator unstemlist = stp_words_removed.iterator();
        Iterator stemlist = stemmedList.iterator();
        Connection conn = DBconnection.getCon(dbcon, true);
        String ins_query = "INSERT INTO t_dictionary(plainword,stemmedword)" + " VALUES ( ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(ins_query);
        for (int i=0;i<stp_words_removed.size();i++) {
            try {
                plainword = unstemlist.next();
                stemmedword = stemlist.next();
                pstmt.setString(1, plainword.toString());
                pstmt.setString(2, stemmedword.toString());
                pstmt.addBatch();
            }catch(Exception e){
            }
        }
        pstmt.executeBatch();
        conn.close();
        pstmt.close();
    }

    /**
     * insert document id, terms and their frequency into DB(tabel name: features)
     * @throws Exception
     */
    private void insertIntoFeatures(Map<String, Integer> collect) throws Exception {
        Connection conn = DBconnection.getCon(dbcon, true);
        String ins_query = "INSERT INTO features(docid,term,term_frequency)" + " VALUES (?, ?, ?)";
        PreparedStatement pstmt = conn.prepareStatement(ins_query);
        for (Map.Entry<String, Integer> entry1 : collect.entrySet()) {
            pstmt.setInt(1, docid);
            pstmt.setString(2, entry1.getKey());
            pstmt.setInt(3, entry1.getValue());
            pstmt.addBatch();
        }
        pstmt.executeBatch();
        conn.close();
    }

    /**
     *
     * @return : stop word list according to language - English or German accordingly
     * @throws Exception
     */
    private List stopWordList() throws Exception {
        ArrayList<String> list = new ArrayList<>();
        if (language == "en") {
            Scanner s = new Scanner(new File(stop_word_path));
            while (s.hasNext()) {
                list.add(s.next());
            }
            s.close();
        } else if (language == "de") {
            Scanner s = new Scanner(new File(g_stop_word_path));

            while (s.hasNext()) {
                list.add(s.next());
            }
            s.close();
        }
        return list;
    }

    /**
     *
     * @return stop words list
     * @throws Exception
     */
    private List stopWords() throws Exception {
        ArrayList<String> list = new ArrayList<>();
        Scanner s = new Scanner(new File(stop_word_path));
        while (s.hasNext()) {
            list.add(s.next());
        }
        s.close();
        return list;
    }

    /**
     *
     * @param htmlContent : raw HTML content
     * @param id : document id
     * @throws Exception
     */
    public void getSource(String htmlContent, int id,NodeList imgtg,int count,String domain,String heading, String url) {
        try {
            docid = id;
            imgtag_count = count;
            title=heading.replaceAll("'","''");
            contents = htmlContent;
            imgtags = imgtg;
            domainName=domain;
            docurl=url;
            removeStyle();
        }catch(Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Removes style tags from HTML
     * @throws Exception
     */
    private void removeStyle() throws Exception {
        Pattern style = Pattern.compile("<style.*?>.*?</style>");
        Matcher matchStyle = style.matcher(contents);
        while (matchStyle.find()) contents = matchStyle.replaceAll("");
        removeScript();
    }

    /**
     * Removes script tags from HTML
     * @throws Exception
     */
    private void removeScript() throws Exception {
        Pattern script = Pattern.compile("<script.*?>.*?</script>");
        Matcher matchScript = script.matcher(contents);
        while (matchScript.find()) contents = matchScript.replaceAll("");
        getMetaData();
        removeComment();
    }

    /**
     * Removes special characters rom HTML
     * @throws Exception
     */
    private void removeSpecial() throws Exception {
        Pattern special = Pattern.compile("[^.,';a-zA-Z0-9 ]");
        Matcher matchSpecial = special.matcher(contents);
        while (matchSpecial.find()) contents = matchSpecial.replaceAll("");
//        removeNumber();
    }

    /**
     * Removes numbers from HTML
     * @throws Exception
     */
//    private void removeNumber() throws Exception {
//        Pattern number = Pattern.compile("[0-9]");
//        Matcher matchNumber = number.matcher(content);
//        while (matchNumber.find()) content = matchNumber.replaceAll("");
//        stopWordRemoval(content);
//    }

    /**
     * Removes primary tags like ... and others from HTML
     * @throws Exception
     */
    private void removeTag() throws Exception {
        Pattern tag = Pattern.compile("\\<.*?>");
        Matcher matchTag = tag.matcher(contents);
       /* String[] splited = content.split("\\s+");
        String str = String.join(" ", splited);*/

        while (matchTag.find()) contents = matchTag.replaceAll(" ");
        //content = str;
        stopWordRemoval();
    }

    /**
     * Retreive Meta Tag information like "author","description","keywords" and insert into database(table name : metadata)
     * @throws Exception
     */
    public void getMetaData() throws Exception {
        String author = "";
        String description = "";
        String keywords = "";
        Pattern metaAuthor = Pattern.compile("(?<=name=\"author\" content=\")[^\"]*(?=\")");
        Matcher matchmetaAuthor = metaAuthor.matcher(contents);
        while (matchmetaAuthor.find()) {
            author = matchmetaAuthor.group();
            matchmetaAuthor.group();
        }
        Pattern metaDescription = Pattern.compile("(?<=name=\"description\" content=\")[^\"]*(?=\")");
        Matcher matchmetaDescription = metaDescription.matcher(contents);
        while (matchmetaDescription.find()) {
            description = matchmetaDescription.group();
            matchmetaDescription.group();
        }
        Pattern metaKeywords = Pattern.compile("(?<=name=\"keywords\" content=\")[^\"]*(?=\")");
        Matcher matchmetaKeywords = metaKeywords.matcher(contents);
        while (matchmetaKeywords.find()) {
            keywords = matchmetaKeywords.group();
            matchmetaKeywords.group();
        }
        try {
            if (author.isEmpty() && description.isEmpty() && keywords.isEmpty()) {
                author = null;
                description = null;
                keywords = null;
            }
            Connection c = DBconnection.getCon(dbcon, true);
            Statement stmt = c.createStatement();
            String insert = "INSERT INTO metadata(docid,author, description, keyword,title)" + " VALUES ("+docid+",'" + author + "','" + description + "','" + keywords + "','" + title + "')";
            stmt.executeUpdate(insert);
            c.close();
            stmt.close();
        } catch (Exception e) {
            System.out.println("Catch block:getMetaData ");
            e.printStackTrace();
        }
    }

    /**
     * Removes general comments
     * @throws Exception
     */
    private void removeComment() throws Exception {
        Pattern comment = Pattern.compile("<!--.*?-->");
        Matcher matchComment = comment.matcher(contents);
        while (matchComment.find()) contents = matchComment.replaceAll("");
        removeTag();
    }

    /**
     * Does stemming for the words on the HTML content after stop word removal
     * @param arr
     * @return
     * @throws Exception
     */
    public String stemming(String arr) throws Exception {
        Stemmer stemmer = new Stemmer();
        char[] splitChar = arr.trim().toCharArray();
        stemmer.add(splitChar, splitChar.length);
        stemmer.stem();
        return stemmer.toString();
    }

   /* public String[] stemming(List<String> stp_words_removed) throws Exception {
        for (int i = 0; i < arr.length; i++) {
            String temp = arr[i];
            Stemmer stemmer = new Stemmer();
            char[] splitChar = temp.trim().toCharArray();
            stemmer.add(splitChar, splitChar.length);
            stemmer.stem();
            arr[i] = stemmer.toString();
        }
        return arr;
    }*/

    /**
     * Removes more than single space and replaces with single space
     * @throws Exception
     */
//    private void removeTab() throws Exception {
//        Pattern tab = Pattern.compile("\\s{2,}");
//        Matcher matchTab = tab.matcher(content);
//        while (matchTab.find()) content = matchTab.replaceAll(" ");
//        removeSpecial();
//    }

}