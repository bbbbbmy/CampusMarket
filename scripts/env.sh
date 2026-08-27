# CampusMarket 本地开发环境变量
# 用法： source ./scripts/env.sh
export JAVA_HOME="${JAVA_HOME:-/home/pi/workspace/jdk/jdk-17.0.2}"
export PATH="$JAVA_HOME/bin:/home/pi/workspace/apache-maven-3.9.16/bin:$PATH"
echo "JAVA_HOME=$JAVA_HOME"
java -version 2>&1 | head -1
mvn -v 2>&1 | head -2
