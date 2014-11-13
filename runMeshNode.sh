export MAVEN_OPTS="-Djava.util.logging.config.file=logging.properties"
mvn exec:java -Dexec.mainClass="com.zeebo.lithium.mesh.MeshNode" -Dexec.args="serverName=$1 port=$2"
