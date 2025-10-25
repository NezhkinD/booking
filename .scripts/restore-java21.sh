#!/bin/bash

# Restore Java 21 alternatives

JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64

echo "Installing Java 21 alternatives..."

sudo update-alternatives --install /usr/bin/java java ${JAVA_HOME}/bin/java 2111
sudo update-alternatives --install /usr/bin/javac javac ${JAVA_HOME}/bin/javac 2111
sudo update-alternatives --install /usr/bin/jar jar ${JAVA_HOME}/bin/jar 2111
sudo update-alternatives --install /usr/bin/jarsigner jarsigner ${JAVA_HOME}/bin/jarsigner 2111
sudo update-alternatives --install /usr/bin/javadoc javadoc ${JAVA_HOME}/bin/javadoc 2111
sudo update-alternatives --install /usr/bin/javap javap ${JAVA_HOME}/bin/javap 2111
sudo update-alternatives --install /usr/bin/jcmd jcmd ${JAVA_HOME}/bin/jcmd 2111
sudo update-alternatives --install /usr/bin/jdb jdb ${JAVA_HOME}/bin/jdb 2111
sudo update-alternatives --install /usr/bin/jdeps jdeps ${JAVA_HOME}/bin/jdeps 2111
sudo update-alternatives --install /usr/bin/jfr jfr ${JAVA_HOME}/bin/jfr 2111
sudo update-alternatives --install /usr/bin/jhsdb jhsdb ${JAVA_HOME}/bin/jhsdb 2111
sudo update-alternatives --install /usr/bin/jinfo jinfo ${JAVA_HOME}/bin/jinfo 2111
sudo update-alternatives --install /usr/bin/jlink jlink ${JAVA_HOME}/bin/jlink 2111
sudo update-alternatives --install /usr/bin/jmap jmap ${JAVA_HOME}/bin/jmap 2111
sudo update-alternatives --install /usr/bin/jmod jmod ${JAVA_HOME}/bin/jmod 2111
sudo update-alternatives --install /usr/bin/jpackage jpackage ${JAVA_HOME}/bin/jpackage 2111
sudo update-alternatives --install /usr/bin/jps jps ${JAVA_HOME}/bin/jps 2111
sudo update-alternatives --install /usr/bin/jrunscript jrunscript ${JAVA_HOME}/bin/jrunscript 2111
sudo update-alternatives --install /usr/bin/jshell jshell ${JAVA_HOME}/bin/jshell 2111
sudo update-alternatives --install /usr/bin/jstack jstack ${JAVA_HOME}/bin/jstack 2111
sudo update-alternatives --install /usr/bin/jstat jstat ${JAVA_HOME}/bin/jstat 2111
sudo update-alternatives --install /usr/bin/jstatd jstatd ${JAVA_HOME}/bin/jstatd 2111
sudo update-alternatives --install /usr/bin/keytool keytool ${JAVA_HOME}/bin/keytool 2111
sudo update-alternatives --install /usr/bin/rmiregistry rmiregistry ${JAVA_HOME}/bin/rmiregistry 2111
sudo update-alternatives --install /usr/bin/serialver serialver ${JAVA_HOME}/bin/serialver 2111

echo "Setting Java 21 as default..."

sudo update-alternatives --set java ${JAVA_HOME}/bin/java
sudo update-alternatives --set javac ${JAVA_HOME}/bin/javac

echo "Done! Verifying installation..."
java --version
javac --version

echo ""
echo "JAVA_HOME should be set to: ${JAVA_HOME}"
echo "Add this to your ~/.bashrc or ~/.zshrc:"
echo "export JAVA_HOME=${JAVA_HOME}"
echo "export PATH=\$JAVA_HOME/bin:\$PATH"
