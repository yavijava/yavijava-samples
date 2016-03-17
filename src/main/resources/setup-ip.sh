#!/bin/bash
#This script should be executed at boot time. 
#User can add a line a line like "sh /root/setup-ip.sh" into /etc/rc.d/rc.local to run it when the system starts.
#vmware tools are installed under /usr/sbin
#ip_config is stored in machine.id, it can be retrived by "rpctool machine.id.get"
#The format of ip_config is: ip:ip_value,netmask:netmask_value,gateway:gateway_value,dns1:dns1_value,dns2:dns2_value
ip_config=`/usr/sbin/vmware-rpctool machine.id.get`
ip=$(echo ${ip_config} | grep -Po '(?<=ip:)[^,]*')
netmask=$(echo ${ip_config} | grep -Po '(?<=netmask:)[^,]*')
gateway=$(echo ${ip_config} | grep -Po '(?<=gateway:)[^,]*')
dns1=$(echo ${ip_config} | grep -Po '(?<=dns1:)[^,]*')
dns2=$(echo ${ip_config} | grep -Po '(?<=dns2:)[^,]*')
#set ip, netmask, gateway
ifcfg_file='/etc/sysconfig/network-scripts/ifcfg-eth0'
printf "DEVICE=eth0\n" > $ifcfg_file
printf "ONBOOT=yes\n" >> $ifcfg_file
printf "BOOTPROTO=static\n" >> $ifcfg_file
printf "STARTMODE=manual\n" >> $ifcfg_file
printf 'NAME="System eth0"\n' >> $ifcfg_file
printf "IPADDR=${ip}\n" >> $ifcfg_file
printf "NETMASK=${netmask}\n" >> $ifcfg_file
printf "GATEWAY=${gateway}\n" >> $ifcfg_file
#set dns servers
dns_cfg_file='/etc/resolv.conf'
printf "nameserver ${dns1}\n" > $dns_cfg_file
printf "nameserver ${dns2}\n" >> $dns_cfg_file
#restart network to take effect
/etc/init.d/network restart
echo 'Set up static ip done!'