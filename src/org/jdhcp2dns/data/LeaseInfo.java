package org.jdhcp2dns.data;

public class LeaseInfo {

	public String hostname;
	public String ipv4;
	public java.time.ZonedDateTime start;
	public java.time.ZonedDateTime end;
	public String mac;
	
	public boolean isValid() {
		if (hostname == null)
			return false;
		
		if (ipv4 == null)
			return false;
		
		if (start == null)
			return false;
		
		if (end == null)
			return false;
		
		return true;
	}
	
	public String getReverseIP () {
		int dot1 = ipv4.indexOf('.');
		int dot2 = ipv4.indexOf('.', dot1 + 1);
		int dot3 = ipv4.indexOf('.', dot2 + 1);
		String o1 = ipv4.substring(0, dot1);
		String o2 = ipv4.substring(dot1 + 1, dot2);
		String o3 = ipv4.substring(dot2 + 1, dot3);
		String o4 = ipv4.substring(dot3 + 1, ipv4.length());
		return o4 + "." + o3 + "." + o2 + "." + o1;
	}
	
	@Override
	public String toString() {
		return "host " + hostname + ",ipv4 " + ipv4 + ",start " + start + ",end " + end + ",reverse " + getReverseIP();
	}
}
