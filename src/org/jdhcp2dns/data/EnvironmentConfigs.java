package org.jdhcp2dns.data;

public class EnvironmentConfigs {

	public String domainName;
	
	public String getDomainName () {
		return domainName;
	}
	
	/* Constant parameters for FreeBSD +  */

	public String getLeaseFilePath () {
		return "/var/db/dhcpd.leases";
	}
	
	public String getUnboundHome () {
		return "/var/unbound/";
	}
	
	public String getUnboundControlPath () {
		return "/usr/sbin/local-unbound-control";
	}
}
