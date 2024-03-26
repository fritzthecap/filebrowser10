cd `dirname \$0`

PATH=.:$PATH
export PATH

JAVA=java

$JAVA -Duser.language=de -Duser.country=AT -jar FileBrowser10.jar
