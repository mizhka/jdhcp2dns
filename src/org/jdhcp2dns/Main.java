package org.jdhcp2dns;

import org.jdhcp2dns.api.DHCP;
import org.jdhcp2dns.api.DNS;
import org.jdhcp2dns.data.EnvironmentConfigs;
import org.jdhcp2dns.data.LeaseInfo;
import org.jdhcp2dns.impl.DHCPOpenBSD;
import org.jdhcp2dns.impl.LocalUnbound;

public class Main {

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: java -Xmx32m -jar jdhcp2dns.jar <domain_name>");
			return;
		}
		
		EnvironmentConfigs env = new EnvironmentConfigs();
		env.domainName = args[0];
		
		DHCP producer = new DHCPOpenBSD(env);
		DNS consumer = new LocalUnbound(env);
		
		LeaseInfo info;
		while((info = producer.nextLease()) != null) {
			consumer.newLease(info);
		}
	}
}
