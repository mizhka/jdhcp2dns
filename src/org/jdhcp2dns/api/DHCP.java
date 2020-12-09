package org.jdhcp2dns.api;

import org.jdhcp2dns.data.LeaseInfo;

public interface DHCP {

	public LeaseInfo nextLease();
}
