java -version
CLASSPATH=.:../ant/ant.jar:../ant/xml-apis.jar:../ant/xercesImpl.jar
java -cp "$CLASSPATH" org.apache.tools.ant.Main $*