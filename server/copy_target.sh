#!/bin/bash

cp target/server-1.0.war /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/webapps/

rm /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/webapps/orange.war
rm -r /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/webapps/orange/

mv /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/webapps/server-1.0.war /opt/homebrew/Cellar/tomcat@9/9.0.113/libexec/webapps/orange.war
