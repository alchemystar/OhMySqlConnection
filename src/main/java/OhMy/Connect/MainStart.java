package OhMy.Connect;

/**
 * Created by alchemystar on 16/1/5.
 */
public class MainStart {
    public static void main(String args[]) {
        String host = "127.0.0.1";
        String user = "pay";
        String password = "MiraCle";
        MysqlIO mysqlIO = new MysqlIO(host, 8080, user, password, "wms_promo");
    }
}
