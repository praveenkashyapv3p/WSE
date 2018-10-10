package queryprocessing;

import db.DBconnection;
import userinterface.SearchReq;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;

import static literals.Literals.dbcon;

public class Advertisement {
    public Map<String, Integer> advertisement(String userInput) throws Exception {
        String siteValue;
        SearchReq searchReq = new SearchReq();
        String removeSite;
        String[] siteRemoved;
        if (userInput.toLowerCase().contains("site:".toLowerCase())) {
            siteValue = searchReq.processSearchString(userInput);
            removeSite = userInput.replace("site:", "").replace(siteValue, "");
            siteRemoved = removeSite.split(" ");
        } else {
            siteRemoved = userInput.split(" ");
        }
        userInput = String.join(" ", siteRemoved);
        userInput = userInput.replaceAll("~", "").replaceAll("\"", "");
        List<String> myList = new ArrayList<>(Arrays.asList(userInput.split(" ")));
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        List<Integer> adIds = new ArrayList<>();
        for (String ad : myList) {
            String ads = "SELECT * FROM advertisement WHERE tags LIKE '%" + ad + "%';";
            ResultSet resultSet = stmt.executeQuery(ads);
            while (resultSet.next()) {
                if (!adIds.contains(resultSet.getInt(1)))
                    adIds.add(resultSet.getInt(1));
            }
        }
        Map<String, String> tags = taglist(adIds);
        Map<String, Integer> test = scoringModel(tags, userInput);
        Map<String, Integer> sorted = sortByValue(test);
        conn.close();
        stmt.close();
        return sorted;
    }

    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> unsortMap) {

        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(unsortMap.entrySet());

        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        Collections.reverse(list);
        Map<K, V> result = new LinkedHashMap<K, V>();
        for (Map.Entry<K, V> entry : list) {
            if (result.size()<4)
                result.put(entry.getKey(), entry.getValue());
        }

        return result;

    }

    private Map<String, String> taglist(List<Integer> adIds) throws Exception {
        Connection conn = DBconnection.getCon(dbcon, true);
        Statement stmt = conn.createStatement();
        Map<String, String> test = new HashMap<>();
        for (Integer ids : adIds) {
            String tags = "select tags,id from advertisement where id=" + ids + "";
            ResultSet resultSet = stmt.executeQuery(tags);
            while (resultSet.next()) {
                test.put(resultSet.getString(2), resultSet.getString(1));
            }
        }
        conn.close();
        stmt.close();
        return test;
    }

    private Map<String, Integer> scoringModel(Map<String, String> tags, String userInput) {
        List<String> myList = new ArrayList<>(Arrays.asList(userInput.split(" ")));
        Set keys = new HashSet();
        int score = 0;
        Map<String, Integer> test = new HashMap<>();
        for (String values : tags.values()) {
            for (Map.Entry entry : tags.entrySet()) {
                if (values.equals(entry.getValue())) {
                    keys.add(entry.getKey());
                }
            }
            for (String testing : myList) {
                test.put(keys.toString(), score);
                if (values.contains(testing)) {
                    score++;
                    test.put(keys.toString(), score);
                }
            }
            score = 0;
            keys.clear();
        }
        return test;
    }


    public static void main(String[] args) {
        Advertisement advertisement = new Advertisement();
        try {
            Map<String, Integer> test = advertisement.advertisement("google");
            System.out.println(test);
        } catch (Exception e) {
        }
    }
}
