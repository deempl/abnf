import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class OracleJDBC {

	private String executeQuery(String query) throws SQLException{
		
		
		
		String url;
		String user;
		String password;
		
		Connection connection = DriverManager.getConnection(url, user, password);
		
		Statement statement = connection.createStatement();
		
		ResultSet executeQuery = statement.executeQuery(query);
		
		while (executeQuery.next())
			
			
		}
}

}
