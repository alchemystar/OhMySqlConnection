/*
 * Copyright (C) 2014 Baidu, Inc. All Rights Reserved.
 */
package OhMy;

import OhMy.Connect.MysqlIO;

/**
 * @author lizhuyang
 */
public class MainStart {

    public static void main(String args[]){
        System.out.println("It' my connection to mysql");
        int port=3306;
        //String host = "127.0.0.1";
        String host = "10.95.20.24";
        String user = "pay";
        String password = "MiraCle";
        String database = null;//"data_config_center";
        MysqlIO mysqlIO = new MysqlIO(host,port,user,password,database);

        System.out.println("Yeah");
    }
}
