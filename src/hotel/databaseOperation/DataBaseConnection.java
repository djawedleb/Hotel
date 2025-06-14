package hotel.databaseOperation;

import java.sql.Connection;
import java.sql.DriverManager;




/**
 *
 * @author Faysal Ahmed
 */


public class DataBaseConnection {

	static String url = "jdbc:sqlite:hotel.sqlite";
    public static Connection connectTODB(){
        try {
            Class.forName("org.sqlite.JDBC");
            return DriverManager.getConnection(url);
        } catch (Exception e) {
        	
            System.err.println("Connection error");
            e.printStackTrace();
            return null;
        }
      
    }
    
}
