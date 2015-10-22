import java.sql.ResultSet;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;

import start.OracleJDBC;

public class ConnectionTest {

	@Parameter
	public static String query = "select igc.IPP_CAMP_VER as wersja, igc.IPP_CAMP_NAME as nazwa, igp.IPP_CAMP_PKG_NUM as \"NUMER / 99\", igc.IPP_CAMP_PKG_COUNT from IPP_GEN_CAMP_T igc join IPP_GEN_PROC_T igp on igc.IPP_CAMP_ID = igp.IPP_CAMP_ID order by igc.IPP_CAMP_VER desc, igp.IPP_CAMP_PKG_NUM desc";

	@Test
	public void connectTest() throws Exception {

		OracleJDBC oracleJDBC = new OracleJDBC();

		oracleJDBC.connect();
		ResultSet executeQuery = oracleJDBC.executeQuery(query);
		executeQuery.next();
		System.out.println(executeQuery.getString(1));
		Assert.assertNotNull(executeQuery);

	}

}
