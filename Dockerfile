FROM maven:3.9.7-eclipse-temurin-8-alpine

# Please add the following line when using MSSQL instead of PostgresSQL and download the jar file here: https://learn.microsoft.com/en-us/sql/connect/jdbc/download-microsoft-jdbc-driver-for-sql-server?view=sql-server-ver16  
# COPY mssql-jdbc-12.6.2.jre8.jar /mssql-jdbc-12.6.2.jre8.jar
COPY db_conf.txt /db_conf.txt
COPY configs/ /configs/
COPY deidentifier-pipeline/ /deidentifier-pipeline/
RUN mvn package --file deidentifier-pipeline/pom.xml
