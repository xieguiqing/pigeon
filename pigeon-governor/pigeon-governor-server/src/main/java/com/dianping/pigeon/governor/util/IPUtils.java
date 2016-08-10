package com.dianping.pigeon.governor.util;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;
import java.util.*;

import javax.servlet.http.HttpServletRequest;

public class IPUtils {

    private static Logger logger = LogManager.getLogger();

	public static String getFirstNoLoopbackIP4Address() {
        Collection<String> allNoLoopbackIP4Addresses = getNoLoopbackIP4Addresses();
        if (allNoLoopbackIP4Addresses.isEmpty()) {
            return null;
        }
        return allNoLoopbackIP4Addresses.iterator().next();
    }


    public static boolean regionCheck(String regionNetPrefix,String ipAddress){
        if(ipAddress.indexOf(regionNetPrefix)==0)
            return true;
        else
            return false;
    }

    public static Collection<String> getNoLoopbackIP4Addresses() {
        Collection<String> noLoopbackIP4Addresses = new ArrayList<String>();
        Collection<InetAddress> allInetAddresses = getAllHostAddress();

        for (InetAddress address : allInetAddresses) {
            if (!address.isLoopbackAddress() && !address.isSiteLocalAddress()
                    && !Inet6Address.class.isInstance(address)) {
                noLoopbackIP4Addresses.add(address.getHostAddress());
            }
        }
        if (noLoopbackIP4Addresses.isEmpty()) {
            // 降低过滤标准，将site local address纳入结果
            for (InetAddress address : allInetAddresses) {
                if (!address.isLoopbackAddress() && !Inet6Address.class.isInstance(address)) {
                    noLoopbackIP4Addresses.add(address.getHostAddress());
                }
            }
        }
        return noLoopbackIP4Addresses;
    }
    
    public static Collection<InetAddress> getAllHostAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            Collection<InetAddress> addresses = new ArrayList<InetAddress>();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();
                    addresses.add(inetAddress);
                }
            }

            return addresses;
        } catch (SocketException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
	
    public static String getUserIP(final HttpServletRequest request) {
        if (request == null) {
            return "0.0.0.0";
        }
        // 获取cdn-src-ip中的源IP
        String ip = request.getHeader("Cdn-Src-Ip");
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return (ip == null || "".equals(ip)) ? "0.0.0.0" : ip;
    }
    
    public static String addHost(String hosts, String ip, String port) {
	    if(!checkIpAddress(ip)) {
	        throw new RuntimeException("Invalid ip " + ip);
	    }
	    if(!checkNumber(port, 1, 65535)) {
	        throw new RuntimeException("Invalid port " + port);
	    }
	    String host = ip + ":" + port;
	    if(hosts != null && hosts.indexOf(host) != -1) {
	        // if already exists, just return	        
	        return hosts;
	    }
	    hosts = (hosts==null ? "" : hosts.trim());
	    StringBuilder sb = new StringBuilder(hosts);
	    if(hosts.length()>0 && !hosts.endsWith(","))
	        sb.append(',');
	    sb.append(host);
	    return sb.toString();
	}
    
    private static boolean checkIpAddress(String ip) {
	    if(null == ip)
	        return false;
	    return ip.indexOf('.') != -1;
	}
	
	private static boolean checkNumber(String number, int min, int max) {
	    try {
	        int n = Integer.parseInt(number);
	        return (n>=min && n<=max);
	    } catch(NumberFormatException e) {
	        return false;
	    }
	}

	public static String removeHost(String hosts, String ip, String port) {
	    if(!checkIpAddress(ip)) {
            throw new RuntimeException("Invalid ip " + ip);
        }
        if(!checkNumber(port, 1, 65535)) {
            throw new RuntimeException("Invalid port " + port);
        }
	    String host = ip + ":" + port;
	    int idx = -1;
	    if(hosts==null || (idx = hosts.indexOf(host)) == -1) {
	        // if not exist, ignore
	        return hosts;
	    }
	    int idx2 = hosts.indexOf(',', idx);
	    String newHosts = hosts.substring(0, idx) + 
	            ((idx2==-1 || idx2==hosts.length()-1) ? "" : hosts.substring(idx2 + 1));
	    return newHosts;
	}

    /**
     * @author chenchongze
     * @param ipAddr
     * @return
     */
    public static InetAddress validateIpAddr(String ipAddr) {
        InetAddress realip = null;

        try {
            realip = InetAddress.getByName(ipAddr);
        } catch (UnknownHostException e) {
            logger.error("error address [" + ipAddr + "]");
        }

        return realip;
    }

    public static boolean validatePort(String port) {
        try {
            Integer.valueOf(port);
            return true;
        } catch (NumberFormatException e) {
            logger.error("error port [" + port + "]");
            return false;
        }
    }

    /**
     * @author chenchongze
     * @param hosts
     * @return
     */
    public static String getValidHosts(String hosts) {
        return StringUtils.join(Sets.newHashSet(getValidHosts(hosts.split(","))), ",");
    }

    public static String[] getValidHosts(String[] ipPorts) {
        Set<String> result = Sets.newHashSet();

        for(String ipPort : ipPorts) {

            if(StringUtils.isNotBlank(ipPort)) {
                int length = ipPort.split(":").length;

                if(length > 1 && length < 10) {
                    int index = ipPort.lastIndexOf(":");
                    String ip = ipPort.substring(0, index);
                    String port = ipPort.substring(index + 1);

                    if(validateIpAddr(ip) != null && validatePort(port)) {
                        result.add(ipPort);
                    }
                }
            }
        }

        return result.toArray(new String[result.size()]);
    }

    public static String getHost(String ip, String port) {
        if (StringUtils.isNotBlank(ip) && StringUtils.isNotBlank(port)) {
            return ip + ":" + port;
        }

        throw new IllegalArgumentException("ip or port can't be null...please check.");
    }

}
