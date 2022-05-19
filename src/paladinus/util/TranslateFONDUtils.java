package paladinus.util;

import java.io.File;
import java.io.IOException;

public class TranslateFONDUtils {

	public static String toAbsolute(String resFile) {
		File file = new File(resFile);
		return file.getAbsolutePath();
	}

	public static void translateFOND(String domainFile, String instanceFile) throws IOException, InterruptedException {
		String cmd = "translator-fond/translate.py " + domainFile + " " + instanceFile;
		Process p = Runtime.getRuntime().exec(cmd);
		p.waitFor();
	}
	
	public static void removeSASFile() throws IOException, InterruptedException {
		 Runtime run = Runtime.getRuntime();
		 Process pr;
		 if(!OsUtils.isWindows()) {
			 pr = run.exec("rm -rf output.sas");
			 pr.waitFor();
		 } else {
			 pr = run.exec("del output.sas");
			 pr.waitFor();			 
		 }
	}
}
