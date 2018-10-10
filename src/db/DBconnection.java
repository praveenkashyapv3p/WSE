package db;

import literals.Literals;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBconnection extends Literals {

    /**
     *
     * @param connectionString : connection url for DB
     * @param autocommit
     * @return ; connection object
     * @throws Exception
     */
    public static Connection getCon(String connectionString, boolean autocommit) throws Exception {
        Connection conn ;
        Class.forName(driver_name);
        conn = DriverManager.getConnection(connectionString, u_name, pwd);
        conn.setAutoCommit(autocommit);
        return conn;
    }

    /*public static void main(String[] args) throws Exception {
        getCon(dbcon, true);
    }*/
}
