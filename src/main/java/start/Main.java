package start;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class Main {

	private static final String EFLAG = " -e ";
	private static final int STANDARD_PORT = 22;
	private static final int DELAY_BEFORE_GREP = 40;
	private static final Logger log = Logger.getLogger("ABNF_Download");

	public static void main(String[] arg) throws Exception {

		if (!checkPassedArgs(arg))
			return;

		final String EWID = arg[0];
		final String user = arg[1];
		final String host = arg[2];
		final String pass = arg[3];
		final String confDir = arg[4];
		final String runDir = arg[5];
		final String billDay = arg[6];

		List<String> billNoList = readBillNo();
		Session session = getConnectedSession(user, host, pass);

		String cmd = preparIppCmd(runDir, billDay, EWID);

		log.info("Executing: " + cmd);
		log.info("Creating ABNF...");

		conn(session, cmd);
		Thread.sleep(DELAY_BEFORE_GREP);

		cmd = "grep ipp_adf_out " + confDir;
		log.info("Checkin ipp out directory...");

		String line = conn(session, cmd);
		String outDir = line.replaceFirst("ipp_adf_out=", "").replaceAll("\n", "").replaceAll(" ", "").replaceAll("\t", "");

		log.info("IPP out directory: " + outDir);

		String billNo = preperCMDforSerchingBillFile(billNoList);
		cmd = "cd " + outDir + " \ngrep -r -l " + billNo + " *";

		log.info("Executing grep " + cmd);
		log.info("Downloading ABNF file...");
		line = conn(session, cmd);
		log.info("Generated files: \n" + line);

		List<String> fileToDownload = Arrays.asList(line.split("\n"));

		if (fileToDownload.size() == 0) {
			log.info("No ABNF files for given account ID!");
			return;
		}

		downloadFiles(EWID, session, outDir, fileToDownload);

		session.disconnect();
	}

	private static String preparIppCmd(String runDir, String billDay, String EWID) throws Exception {

		String query = "select igc.IPP_CAMP_VER, igc.IPP_CAMP_NAME, igp.IPP_CAMP_PKG_NUM from IPP_GEN_CAMP_T igc join IPP_GEN_PROC_T igp on igc.IPP_CAMP_ID = igp.IPP_CAMP_ID order by igc.IPP_CAMP_VER desc, igp.IPP_CAMP_PKG_NUM desc";

		OracleJDBC oracleJDBC = new OracleJDBC();
		oracleJDBC.connect();
		ResultSet executeQuery = oracleJDBC.executeQuery(query);
		executeQuery.next();

		String ver = executeQuery.getString(1);
		String date = executeQuery.getString(2);
		int campNumInt = executeQuery.getInt(3);

		if (campNumInt > 98) {
			campNumInt = 1;
			ver = String.valueOf(executeQuery.getInt(1) + 1);
		}

		String campNum = campNumInt < 10 ? "0" + String.valueOf(campNumInt) : String.valueOf(campNumInt);

		String campFlag = date + "_" + campNum + "/99";

		String cmd = "cd " + runDir + "\n./ipp.sh -mode test -acc_range " + EWID + "_" + EWID + " -schema pin01s4 -cycle 1M" + billDay + " -ver " + ver
				+ " -camp " + campFlag + " -disable_camp 1";

		return cmd;
	}

	private static void downloadFiles(String EWID, Session session, String outDir, List<String> fileToDownload)
			throws JSchException, SftpException, FileNotFoundException, IOException {

		for (String file : fileToDownload) {
			if (file.equals(""))
				continue;
			ChannelSftp channelStftp = (ChannelSftp) session.openChannel("sftp");
			channelStftp.connect();
			log.info("Copying file: " + file);
			String cmd = outDir + file;
			OutputStream out = new FileOutputStream(new File(EWID + "_" + file));
			channelStftp.get(cmd, out);

			out.flush();
			out.close();
			channelStftp.disconnect();
		}
	}

	private static String preperCMDforSerchingBillFile(List<String> billNoList) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String billNo : billNoList) {
			stringBuilder.append(EFLAG).append(billNo);
		}
		return stringBuilder.toString();
	}

	private static List<String> readBillNo() {
		List<String> billNoList = new ArrayList<>();
		try (BufferedReader br = new BufferedReader(new FileReader("bill_no"))) {

			String line = br.readLine();

			while (line != null) {
				billNoList.add(line);
				line = br.readLine();
			}

			if (billNoList.size() == 0) {
				log.warning("Empty bill_on file");
				System.exit(0);
			} else {
				log.info("Find " + billNoList.size() + " billing numbers");
			}

		} catch (Exception e) {
			log.warning("Error reading bill numbers");
			System.exit(0);
		}
		return billNoList;
	}

	private static Session getConnectedSession(String user, String host, String pass) throws JSchException {
		JSch jsch = new JSch();
		Session session = jsch.getSession(user, host, STANDARD_PORT);
		session.setPassword(pass);
		Properties config = new Properties();
		config.put("StrictHostKeyChecking", "no");
		session.setConfig(config);
		session.connect();
		return session;
	}

	private static boolean checkPassedArgs(String[] args) {
		if (args.length < 7) {
			System.out.println(describeRequiredParameters());
			System.err.println("Missing required parameters!");
			return false;
		}
		return true;
	}

	private static String describeRequiredParameters() {
		String description = "-ewid -user -login_to_host -pass_to_host -config_dir -runDir -billDay";
		return description;
	}

	private static String conn(Session session, String cmd) throws JSchException, IOException, Exception {
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(cmd);
		channel.setInputStream(null);
		((ChannelExec) channel).setErrStream(System.err);
		InputStream in = channel.getInputStream();
		Reader reader = new InputStreamReader(in);
		BufferedReader bufferedReader = new BufferedReader(reader);
		channel.connect();
		System.out.println("Connected to IPP.");

		StringBuilder output = new StringBuilder();

		String line = "";

		while (true) {
			line = bufferedReader.readLine();
			if (line == null)
				break;
			output.append("\n").append(line);
		}
		channel.disconnect();
		session.disconnect();
		return output.toString();
	}
}
