
@echo off

set HOME=%CD%
set VERSION=1.14.1

cd C:\Users\atolo\eclipse-workspace\odilon-client\deploy-maven-central-%VERSION%\io\odilon\odilon-client\%VERSION%

CertUtil -hashfile odilon-client-%VERSION%.pom MD5  > odilon-client-%VERSION%.pom.md5
CertUtil -hashfile odilon-client-%VERSION%.pom SHA1 > odilon-client-%VERSION%.pom.sha1

CertUtil -hashfile odilon-client-%VERSION%.jar MD5 > odilon-client-%VERSION%.jar.md5
CertUtil -hashfile odilon-client-%VERSION%.jar SHA1 > odilon-client-%VERSION%.jar.sha1

CertUtil -hashfile odilon-client-%VERSION%-javadoc.jar MD5  > odilon-client-%VERSION%-javadoc.jar.md5
CertUtil -hashfile odilon-client-%VERSION%-javadoc.jar SHA1 > odilon-client-%VERSION%-javadoc.jar.sha1

CertUtil -hashfile odilon-client-%VERSION%-sources.jar MD5  > odilon-client-%VERSION%-sources.jar.md5
CertUtil -hashfile odilon-client-%VERSION%-sources.jar SHA1  > odilon-client-%VERSION%-sources.jar.sha1

cd %HOME%









