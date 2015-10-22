package start;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OracleJDBC {

	private static String PROP_NAME = "DB.conf";

	private String url;
	private String user;
	private String password;
	private Connection connection;

	private static final Logger log = Logger.getLogger(OracleJDBC.class.getName());

	public ResultSet executeQuery(String query) {
		ResultSet executeQuery = null;

		try {
			PreparedStatement prepareStatement = connection.prepareStatement(query);
			executeQuery = prepareStatement.executeQuery();
		} catch (SQLException e) {
			log.log(Level.WARNING, "Błąd zapytania SQL. \nQuery = " + query, e);
			e.printStackTrace();
		}

		return executeQuery;
	}

	public void connect() throws Exception {
		loadProperties();
		try {
			connection = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			log.log(Level.WARNING, "Nie można nawiązać połącznie z DB", e);
		}
	}

	private void loadProperties() throws FileNotFoundException {
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(PROP_NAME));
			url = properties.getProperty("url");
			user = properties.getProperty("user");
			password = properties.getProperty("password");
		} catch (IOException e) {
			log.log(Level.WARNING, "Nie można załadować pliku: " + PROP_NAME);
			creatEmptyPropFile();
		}
	}

	private void creatEmptyPropFile() throws FileNotFoundException {

		log.log(Level.INFO, "Tworze czysty plik konfiguracyjny:" + PROP_NAME);
		log.info("Wypelnij dane polaczenia z baza");

		File file = new File(PROP_NAME);
		PrintWriter printWriter = new PrintWriter(file);
		printWriter.println("url=");
		printWriter.println("user=");
		printWriter.println("password=");
		printWriter.close();

		log.warning("Program zakonczony niepowodzeniem");
		System.exit(0);
	}

	public void disconect() {
		try {
			connection.close();
		} catch (SQLException e) {
			log.log(Level.WARNING, "Nie można zamknąć połaczenia z DB", e);
		}
	}
}
