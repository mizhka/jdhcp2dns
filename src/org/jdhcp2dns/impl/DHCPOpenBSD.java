package org.jdhcp2dns.impl;

import java.io.File;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.jdhcp2dns.api.DHCP;
import org.jdhcp2dns.data.EnvironmentConfigs;
import org.jdhcp2dns.data.LeaseInfo;
import org.publicsource.LogFileTailer;

public class DHCPOpenBSD implements DHCP {
	
	private static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("e yyyy/MM/dd HH:mm:ss z").withLocale(Locale.UK);
	
	private static final String TOKEN_DELIMS = "{;}";
	
	private static final String TOKEN_LEASE = "lease";
	private static final String TOKEN_STARTS = "starts";
	private static final String TOKEN_ENDS = "ends";
	private static final String TOKEN_HWETH = "hardware ethernet";
	private static final String TOKEN_UID = "uid";
	private static final String TOKEN_HOST = "client-hostname";
		
	private ExecutorService executor = Executors.newFixedThreadPool(5);
	private LinkedBlockingQueue<LeaseInfo> queue = new LinkedBlockingQueue<LeaseInfo>(20);
	
	private class LeaseListener implements LogFileTailer.Listener{
		StringBuffer buf = new StringBuffer();
		@Override
		public void newLogFileLine(String line) {
			if (line.contains("{")) {
				buf = new StringBuffer();
			}
			buf.append(line);
			if (line.contains("}")) {
				try {
					LeaseInfo info = parse(buf.toString());
					if (info != null)
						queue.put(info);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		private LeaseInfo parse(String strLease) {
			LeaseInfo tmp = new LeaseInfo();
			StringTokenizer tokenizer = new StringTokenizer(strLease, TOKEN_DELIMS);
			
			while(tokenizer.hasMoreTokens()) {
				String token = tokenizer.nextToken().trim();
				if (token.startsWith(TOKEN_LEASE)) {
					tmp.ipv4 = token.substring(TOKEN_LEASE.length()+1, token.length());
					continue;
				}
				if (token.startsWith(TOKEN_STARTS)) {
					tmp.start = ZonedDateTime.parse(token.substring(TOKEN_STARTS.length()+1, token.length()),formatter);
					continue;
				}
				if (token.startsWith(TOKEN_ENDS)) {
					tmp.end = ZonedDateTime.parse(token.substring(TOKEN_ENDS.length()+1, token.length()),formatter);
					continue;
				}
				if (token.startsWith(TOKEN_HWETH)) {
					tmp.mac = token.substring(TOKEN_HWETH.length()+1, token.length());
					continue;
				}
				if (token.startsWith(TOKEN_UID)) {
					continue;
				}
				if (token.startsWith(TOKEN_HOST)) {
					tmp.hostname = token.substring(TOKEN_HOST.length()+1, token.length()).replace("\"","");
				}
			}
			
			/* lease info can be not full */
			return  (tmp.isValid()) ? tmp: null;
		}
	}

	public DHCPOpenBSD(EnvironmentConfigs env) {
		File file = new File(env.getLeaseFilePath());
		
		if (!(file.exists() && file.canRead()))
			throw new RuntimeException("Lease file " + file.getAbsolutePath() + " is unacceptable");
		
		LogFileTailer tailer = new LogFileTailer(file,10,true);
		tailer.addLogFileTailerListener(new LeaseListener());
		executor.submit(tailer);
	}

	public LeaseInfo nextLease() {
		try {
			return queue.take();
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
