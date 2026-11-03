@echo off
rem Starts the server. Extra settings can be passed as arguments,
rem for example: run-server.cmd --server.port=6000 --db.password=secret
chcp 65001 > nul
java -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 -jar server\target\tour-server.jar %*
pause
