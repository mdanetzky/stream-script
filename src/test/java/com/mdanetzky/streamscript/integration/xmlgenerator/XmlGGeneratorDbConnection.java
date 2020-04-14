package com.mdanetzky.streamscript.integration.xmlgenerator;

public class XmlGGeneratorDbConnection {

    private Database database;
    private String url;

    public XmlGGeneratorDbConnection.Database getDatabase() {
        return this.database;
    }

    public void setDatabase(Database database) {
        this.database = database;
    }

    public String getDriver() {
        return null;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUser() {
        return null;
    }

    public String getPassword() {
        return null;
    }

    public enum Database {
        H2, DB2, MYSQL, ORACLE, POSTGRES, SQL_SERVER
    }
}
