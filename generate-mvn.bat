
@echo off

set HOME=%CD%
set VERSION=1.14.1

mvn clean -o install 

mvn install source:jar  
mvn install javadoc:jar  
copy -v .\target\*.jar .\deploy-maven-central-%VERSION%\io\odilon\odilon-client\%VERSION%\ 

cd C:\Users\atolo\eclipse-workspace\odilon-client\deploy-maven-central-%VERSION%\io\odilon\odilon-client\1.14.1

CertUtil -hashfile odilon-client-1.14.1.pom MD5
CertUtil -hashfile odilon-client-1.14.1.pom SHA1
CertUtil -hashfile odilon-client-1.14.1.jar MD5
CertUtil -hashfile odilon-client-1.14.1.jar SHA1
CertUtil odilon-client-1.14.1-javadoc.jar MD5 
CertUtil odilon-client-1.14.1-javadoc.jar SHA1
CertUtil odilon-client-1.14.1-sources.jar MD5
CertUtil odilon-client-1.14.1-sources.jar SHA1 

cd %HOME%









