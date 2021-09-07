%echo off

java -Xmx2G -Dfile.encoding=UTF-8 ^
  -Djava.net.useSystemProxies=true -Dhttp.proxyHost=proxydirect.usz.ch -Dhttp.proxyPort=8080 -Dhttps.proxyHost=proxydirect.usz.ch -Dhttps.proxyPort=8080 ^
  -cp lib/deidentifier-0.1-SNAPSHOT.jar org.ratschlab.deidentifier.pipelines.testing.PipelineTesterCmd ^
  kisim-usz/kisim_usz.conf kisim-usz/testcases

pause
