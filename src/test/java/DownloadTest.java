import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import start.Main;

public class DownloadTest {

	@Parameter
	public String outDir = "/export_current01/ipp/out/ADF/";

	@Parameter
	public List<String> fileToDownload = Arrays.asList("test_E.O01.A.B000.G1.ADF.CG.M1215.C01A.P6200.test",
			"test_E.O01.A.FI00.G1.ADF.CG.M1215.C01A.P6200.test");

	@Parameters
	public static Session getSession() throws Exception {
		JSch jSch = new JSch();

		Session session = jSch.getSession("ipp01", "172.25.32.72", 22);
		session.setPassword("ipp01");
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect();
		return session;
	}

	@Test
	public void findABNF() throws IOException, Exception {

		String billPatterns = "F0030063426/001/15";
		String cmd = "cd " + outDir + " \ngrep -r -l " + billPatterns + " *";
		String conn = Main.conn(getSession(), cmd);

		System.out.println(conn);
		Assert.assertNotNull("Błąd pobierania abnf", conn);

	}

	@Test
	public void passedArgs() {
		final String[] args = { "a", "b", "c", "d", "e", "f", "g" };
		boolean checkPassedArgs = Main.checkPassedArgs(args);

		Assert.assertEquals(true, checkPassedArgs);
	}

	@Test
	public void passedBadArgs() {
		final String[] args = { "a", "b", "c", "d", "e", "f" };
		boolean checkPassedArgs = Main.checkPassedArgs(args);

		Assert.assertEquals(false, checkPassedArgs);
	}

	@Test
	public void downloadABNF() throws Exception, JSchException, SftpException, IOException, Throwable {
		Main.downloadFiles("TEST", getSession(), outDir, fileToDownload);
	}

	@Test
	public void preperCMD() throws Exception {
		System.out.println(Main.class.getPackage().getName());
		String preparIppCmd = Main.preparIppCmd("C:\\msm\\cdc\\ss", "4", "0700845646");
		Assert.assertNotNull(preparIppCmd);
		// System.out.println(preparIppCmd);

	}
}
