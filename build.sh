# Display Java version
java -version

# Refer your ANT tool in following CLASSPATH
CLASSPATH=.:../ant/ant.jar:../ant/xml-apis.jar:../ant/xercesImpl.jar

java -cp "$CLASSPATH" org.apache.tools.ant.Main $*
# there may be simple command lines ...
