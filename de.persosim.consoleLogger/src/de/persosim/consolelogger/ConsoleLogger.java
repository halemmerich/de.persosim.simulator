package de.persosim.consolelogger;

import java.text.DateFormat;
import java.util.Date;

import org.osgi.service.log.LogEntry;
import org.osgi.service.log.LogListener;

public class ConsoleLogger implements LogListener {

	DateFormat format = DateFormat.getDateTimeInstance();
	
	@Override
	public void logged(LogEntry entry) {
		if (entry.getMessage() != null){
			System.out.println("[" + entry.getBundle().getSymbolicName() + " - " + format.format(new Date(entry.getTime())) + "] "  + entry.getMessage());
		}
	}

}
