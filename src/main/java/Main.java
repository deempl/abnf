import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Properties;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class Main {

	public static void main(String[] arg) {

		checkPassedArgs(arg);

		String EWID = arg[0];
		String user = arg[1];
		String host = arg[2];
		String confDir = "/app/ipp01/jbossesb-server-4.11/server/ports-07/deploy/properties-service.xml";
		if (arg.length >= 6) {
			confDir = arg[4];
		}
		String runDir = "/app/ipp/ipp_klient/bin";
		if (arg.length >= 7) {
			runDir = arg[5];
		}
		int delay = 4000;

		String camp_flag = null;
		if (arg.length == 8) {
			camp_flag = arg[6];
		}

		String billPatterns = "";
		try {
			BufferedReader br = new BufferedReader(new FileReader("bill_no"));
			StringBuilder sb = new StringBuilder();
			String line = br.readLine();

			while (line != null) {
				sb.append(" -e " + line);
				line = br.readLine();
			}
			billPatterns = sb.toString();
			br.close();
		} catch (Exception e) {
			System.err.println("Error reading bill numbers");
		}
		try {
			JSch jsch = new JSch();
			Session session = jsch.getSession(user, host, 22);
			session.setPassword(arg[3]);
			Properties config = new Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.connect();
			if (camp_flag == null) {
				System.err.println("Bad current version number!");
				return;
			}
			String cmd = "cd " + runDir + "\n./ipp.sh -mode test -acc_range " + EWID + "_" + EWID + " -camp " + camp_flag;
			System.out.println("Executing: " + cmd);
			System.out.println("Creating ABNF...");
			conn(session, cmd);
			try {
				Thread.sleep(delay);
			} catch (Exception localException1) {
			}
			cmd = "grep ipp_adf_out " + confDir;
			System.out.println("Checkin ipp out directory...");
			String line = conn(session, cmd);
			String outDir = line.replaceFirst("ipp_adf_out=", "").replaceAll("\n", "").replaceAll(" ", "").replaceAll("\t", "");
			System.out.println("IPP out directory: " + outDir);
			cmd = "cd " + outDir + " \ngrep -r -l " + billPatterns + " *";
			System.out.println("Executing grep " + cmd);
			System.out.println("Downloading ABNF file...");
			line = conn(session, cmd);
			System.out.println("Generated files: \n" + line);
			ArrayList fnames = new ArrayList();
			String fname = "";
			for (int i = 0; i < line.length(); ++i) {
				fname = fname + line.charAt(i);
				if (line.charAt(i) != '\n')
					continue;
				fnames.add(fname);
				fname = "";
			}

			if (fnames.size() == 0) {
				System.out.println("No ABNF files for given account ID!");
			} else {
				for (int i = 0; i < fnames.size(); ++i) {
					if (((String) fnames.get(i)).replaceAll("\n", "").replaceAll(" ", "").equals(""))
						continue;
					ChannelSftp channelStftp = (ChannelSftp) session.openChannel("sftp");
					channelStftp.connect();
					channelStftp.cd(outDir);
					System.out.println("Copying file: " + ((String) fnames.get(i)).replaceAll("\n", "").replaceAll(" ", "").replaceAll("\t", ""));
					InputStream str = channelStftp.get(((String) fnames.get(i)).replaceAll("\n", "").replaceAll(" ", ""));
					OutputStream out = new FileOutputStream(new File(name + "/" + ((String) fnames.get(i)).replaceAll("\n", "").replaceAll(" ", "")));
					int read = 0;
					byte[] bytes = new byte[1024];
					while ((read = str.read(bytes)) != -1)
						out.write(bytes, 0, read);
					str.close();
					out.flush();
					out.close();
					channelStftp.disconnect();
				}
			}

			session.disconnect();
		} catch (JSchException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SftpException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void checkPassedArgs(String[] args) {
		if (args.length < 5) {
			System.out.println(describeRequiredParameters());
			System.err.println("Missing required parameters!");
			System.exit(0);
		}

	}

	private static String describeRequiredParameters() {
		String description = "-ewid -login_to_host -pass_to_host -config_dir";

		return description;
	}

	private static String conn(Session session, String cmd) throws JSchException, IOException {
		Channel channel = session.openChannel("exec");
		((ChannelExec) channel).setCommand(cmd);
		channel.setInputStream(null);
		((ChannelExec) channel).setErrStream(System.err);
		InputStream in = channel.getInputStream();
		channel.connect();
		System.out.println("Connected to IPP.");
		String line = "";
		byte[] tmp = new byte[1024];

		int i = in.read(tmp, 0, 1024);
		if (i >= 0) {
			line = line + new String(tmp, 0, i);
		}
		while (true) {
			label115: if (in.available() <= 0)
				;
			if (!(channel.isClosed())) {
				try {
					Thread.sleep(1000L);
				} catch (Exception localException) {
				}
			} else {
				in.close();
				channel.disconnect();
				return line;
			}
		}
	}
}
