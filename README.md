# jdhcp2dns
Integrate OpenBSD dhcpd leases DB with Unbound DNS forwarder. 

OpenBSD dhcpd is simple and powerful daemon providing DHCP server, but there is no integration with DNS.
jdhcp2dns pulls data from dhcpd lease DB and pushes it into configuration of Unbound forwarder. It's written in Java (requires 11+). 

### Unbound DNS forwarder (FreeBSD)
FreeBSD provides Unbound DNS forwarder out of the box. As well, OpenBSD dhcpd is ported into FreeBSD and available as port: https://www.freshports.org/net/dhcpd/, https://github.com/koue/dhcpd

### How to use
Trigger it as:
java -Xmx32m -jar jdhcp2dns.jar <domain_name>

### History
Inspired by https://gist.github.com/akpoff/1c1b994a7b51dab34d1413ca7eab0628
