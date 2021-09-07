%echo off

java -Xmx2G -jar -Dfile.encoding=UTF-8 ^
  -Djava.net.useSystemProxies=true -Dhttp.proxyHost=proxydirect.usz.ch -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxydirect.usz.ch -Dhttps.proxyPort=8080 ^
  lib/deidentifier-0.1-SNAPSHOT.jar ^
  annotate -i kisim-usz/fake_reports -o fake_reports_processed --xml-input -c kisim-usz/kisim_usz.conf -t 1

pause
