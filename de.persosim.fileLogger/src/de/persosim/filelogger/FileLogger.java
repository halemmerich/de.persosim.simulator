package de.persosim.filelogger;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

public class FileLogger implements LogListener {

	DateFormat format = DateFormat.getDateTimeInstance();
	String logFileName = "logs" + File.separator + "PersoSim_" + new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance().getTime()) + ".log";
	File file = new File(logFileName);
	PrintWriter writer;
	
	public FileLogger() {
		if (!file.exists()){
			try {
				file.createNewFile();
				writer = new PrintWriter(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void logged(LogEntry entry) {
		if (entry.getMessage() != null){
			writer.println("[" + entry.getBundle().getSymbolicName() + " - " + format.format(new Date(entry.getTime())) + "] "  + entry.getMessage());
		}
	}

}
