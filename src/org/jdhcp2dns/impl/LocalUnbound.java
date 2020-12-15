package org.jdhcp2dns.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.channels.FileChannel;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.jdhcp2dns.api.DNS;
import org.jdhcp2dns.data.EnvironmentConfigs;
import org.jdhcp2dns.data.LeaseInfo;

public class LocalUnbound implements DNS {
	
	private ScheduledExecutorService executor = Executors.newScheduledThreadPool(5);
	
	private final HashMap<String, LeaseInfo> actualLeases;
	private volatile boolean updated;
	private File localDataFile;
	private String domainName;
	private String unboundCtl;
	
	private Callable<Void> purge;
	
	public LocalUnbound(EnvironmentConfigs env) {
		actualLeases = new HashMap<>();
		updated = false;
		localDataFile = new File(env.getUnboundHome() + "/conf.d/local-dhcpd.conf");
		domainName = env.getDomainName();
		unboundCtl = env.getUnboundControlPath();
		purge = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				long seconds;
				synchronized (actualLeases) {
					seconds = purgeExpiredLeases();
				}
				
				if (seconds == 0)
					seconds = 321;

				executor.schedule(purge, seconds, TimeUnit.SECONDS);
				generateFile();
				
				System.out.println("Next purge in " + seconds + " seconds");
				return null;
			}
		};
		executor.schedule(purge, 5, TimeUnit.SECONDS);
	}
	
	private String getRecordA(LeaseInfo item) {
		return "        " + "local-data: \"" + item.hostname + "." + domainName + " A " + item.ipv4 + "\"";
	}
	
	private String getRecordPTR(LeaseInfo item) {
		return "        " + "local-data-ptr: \"" + item.ipv4 + " " + item.hostname + "." + domainName + "\"";
	}
	
	private void generateFile() {
		if (!updated)
			return;
		
		StringBuffer buf = new StringBuffer("server:\n");
		synchronized (actualLeases) {
			updated = false;
			for (LeaseInfo item : actualLeases.values()) {
				buf.append(getRecordA(item));
				buf.append("\n");
				buf.append(getRecordPTR(item));
				buf.append("\n");
			}
		}
		
		try {
			if (!localDataFile.exists())
				localDataFile.createNewFile();
			try (FileOutputStream stream = new FileOutputStream(localDataFile, true);
				FileChannel outChan = stream.getChannel()) {
				  outChan.truncate(0);
			}
			
			try (BufferedWriter bwr = new BufferedWriter(new FileWriter(localDataFile));)
			{
				bwr.write(buf.toString());
				bwr.flush();
			}
			
			new ProcessBuilder(unboundCtl, "reload").start().waitFor();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		
		System.out.println("File updated");
	}
	
	private long purgeExpiredLeases() {
		ZonedDateTime now = ZonedDateTime.now();
		ZonedDateTime eldest = null;
		ArrayList<String> todelete = new ArrayList<>();
		
		for (LeaseInfo item : actualLeases.values()) {
			if (item.end.isBefore(now)) {
				todelete.add(item.hostname);
				continue;
			}
			
			if (eldest == null || eldest.isAfter(item.end))
				eldest = item.end;
		}
		
		for (String host: todelete) {
			actualLeases.remove(host);
			System.out.println("Remove " + host + " from DNS service");
			updated = true;
		}
	
		return (eldest == null) ? 0 : Duration.between(Instant.now(),eldest.toInstant()).getSeconds();
	}
	
	@Override
	public synchronized void newLease(LeaseInfo lease) {
		ZonedDateTime now = ZonedDateTime.now();
		if (lease.end.isBefore(now))
			return;

		synchronized (actualLeases) {
			purgeExpiredLeases();
			
			LeaseInfo old = actualLeases.put(lease.hostname, lease);
			
			if (old != null && old.end.isAfter(lease.end))
				throw new RuntimeException("Unexpected lease purged newer one");
			
			if (old == null || !(old.ipv4.equals(lease.ipv4))) {
				updated = true;
				System.out.println("New/updated IPv4 address " + lease.ipv4 + " for " + lease.hostname);
			}
		}
		
		generateFile();
	}
}
