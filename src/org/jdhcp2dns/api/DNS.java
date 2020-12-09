package org.jdhcp2dns.api;

import org.jdhcp2dns.data.LeaseInfo;

public interface DNS {

	public void newLease(LeaseInfo lease);
}
