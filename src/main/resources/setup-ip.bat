@echo off
::This script should be executed at boot time. 
::User can add a task in Task Scheduler to run it when the system starts.
::vmware tools are installed under C:/Program Files/VMware/VMware Tools
set rpctool="C:/Program Files/VMware/VMware Tools/rpctool.exe"
::ip_config is stored in machine.id, it can be retrived by "rpctool machine.id.get"
::The format of ip_config is: ip:ip_value,netmask:netmask_value,gateway:gateway_value,dns1:dns1_value,dns2:dns2_value
for /f "tokens=1-5 delims=," %%a in ('%rpctool% machine.id.get') do (
    for /f "tokens=2 delims=:" %%h in ("%%a") do set ip=%%h
    for /f "tokens=2 delims=:" %%h in ("%%b") do set netmask=%%h
    for /f "tokens=2 delims=:" %%h in ("%%c") do set gateway=%%h
    for /f "tokens=2 delims=:" %%h in ("%%d") do set dns1=%%h
    for /f "tokens=2 delims=:" %%h in ("%%e") do set dns2=%%h
)
::set ip, netmask, gateway
netsh interface ip set address "Local Area Connection" static %ip% %netmask% %gateway%
::set dns servers
netsh interface ip set dnsservers "Local Area Connection" static %dns1% validate=no
netsh interface ip add dnsservers "Local Area Connection" %dns2% validate=no
echo Set up static ip done!