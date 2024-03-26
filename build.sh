# change to the directory this script is in
cd `dirname \$0`

# list JAR files needed to run ANT
CLASSPATH=.:../ant/ant.jar:../ant/xml-apis.jar:../ant/xercesImpl.jar

# run ANT
java -cp "$CLASSPATH" org.apache.tools.ant.Main $*
