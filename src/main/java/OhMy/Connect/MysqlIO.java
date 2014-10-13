/*
 * Copyright (C) 2014 Baidu, Inc. All Rights Reserved.
 */
package OhMy.Connect;

import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.ref.SoftReference;
import java.net.Socket;
import java.nio.ByteOrder;
import java.rmi.ServerError;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

/**
 * @author lizhuyang
 */
public class MysqlIO {
    private static final int UTF8_CHARSET_INDEX = 33;
    private static final String CODE_PAGE_1252 = "Cp1252";
    protected static final int NULL_LENGTH = ~0;
    protected static final int COMP_HEADER_LENGTH = 3;
    protected static final int MIN_COMPRESS_LEN = 50;
    protected static final int HEADER_LENGTH = 4;
    protected static final int AUTH_411_OVERHEAD = 33;
    private static int maxBufferSize = 65535;
    private static final int CLIENT_COMPRESS = 32; /* Can use compression
    protcol */
    protected static final int CLIENT_CONNECT_WITH_DB = 8;
    private static final int CLIENT_FOUND_ROWS = 2;
    private static final int CLIENT_LOCAL_FILES = 128; /* Can use LOAD DATA
    LOCAL */

    private static final String NONE = "none";

    /* Found instead of
       affected rows */
    private static final int CLIENT_LONG_FLAG = 4; /* Get all column flags */
    private static final int CLIENT_LONG_PASSWORD = 1; /* new more secure
    passwords */
    private static final int CLIENT_PROTOCOL_41 = 512; // for > 4.1.1
    private static final int CLIENT_INTERACTIVE = 1024;
    protected static final int CLIENT_SSL = 2048;
    private static final int CLIENT_TRANSACTIONS = 8192; // Client knows about transactions
    protected static final int CLIENT_RESERVED = 16384; // for 4.1.0 only
    protected static final int CLIENT_SECURE_CONNECTION = 32768;
    private static final int CLIENT_MULTI_QUERIES = 65536; // Enable/disable multiquery support
    private static final int CLIENT_MULTI_RESULTS = 131072; // Enable/disable multi-results
    private static final int CLIENT_PLUGIN_AUTH = 524288;
    private static final int CLIENT_CAN_HANDLE_EXPIRED_PASSWORD = 4194304;
    private static final int CLIENT_CONNECT_ATTRS = 1048576;
    private static final int SERVER_STATUS_IN_TRANS = 1;
    private static final int SERVER_STATUS_AUTOCOMMIT = 2; // Server in auto_commit mode
    static final int SERVER_MORE_RESULTS_EXISTS = 8; // Multi query - next query exists
    private static final int SERVER_QUERY_NO_GOOD_INDEX_USED = 16;
    private static final int SERVER_QUERY_NO_INDEX_USED = 32;
    private static final int SERVER_QUERY_WAS_SLOW = 2048;
    private static final int SERVER_STATUS_CURSOR_EXISTS = 64;
    private static final String FALSE_SCRAMBLE = "xxxxxxxx"; //$NON-NLS-1$
    protected static final int MAX_QUERY_SIZE_TO_LOG = 1024; // truncate logging of queries at 1K
    protected static final int MAX_QUERY_SIZE_TO_EXPLAIN = 1024 * 1024; // don't explain queries above 1MB
    protected static final int INITIAL_PACKET_SIZE = 1024;

    String user;
    String password;
    Socket rawSocket;
    private byte readPacketSequence = -1;
    private boolean checkPacketSequence = false;
    private byte protocolVersion = 0;
    private int maxAllowedPacket = 1024 * 1024;
    protected int maxThreeBytes = 255 * 255 * 255;
    protected int port = 3306;
    protected int serverCapabilities;
    private int serverMajorVersion = 0;
    private int serverMinorVersion = 0;
    private int oldServerStatus = 0;
    private int serverStatus = 0;
    private int serverSubMinorVersion = 0;
    private int warningCount = 0;
    protected long clientParam = 0;
    protected long lastPacketSentTimeMs = 0;
    protected long lastPacketReceivedTimeMs = 0;
    private boolean traceProtocol = false;
    private boolean enablePacketDebug = false;
    private boolean useConnectWithDb;
    private boolean needToGrabQueryFromPacket;
    private boolean autoGenerateTestcaseScript;
    private long threadId;
    private boolean useNanosForElapsedTime;
    private long slowQueryThreshold;
    private String queryTimingUnits;
    private boolean useDirectRowUnpack = true;
    private int useBufferRowSizeThreshold;
    private int commandCount = 0;

    private SoftReference<Buffer> compressBufRef;
    protected String host = null;
    protected String seed;
    private String serverVersion = null;
    private String socketFactoryClassName = null;
    private boolean colDecimalNeedsBump = false; // do we need to increment the colDecimal flag?
    private boolean hadWarnings = false;
    private boolean has41NewNewProt = false;

    /**
     * Does the server support long column info?
     */
    private boolean hasLongColumnInfo = false;
    private boolean isInteractiveClient = false;
    private boolean logSlowQueries = false;
    protected int serverCharsetIndex;
    private int authPluginDataLength = 0;

    private byte packetSequence = 0;

    //输入流
    protected InputStream mysqlInput = null;
    //输出流
    protected BufferedOutputStream mysqlOutput = null;
    private byte[] packetHeaderBuf = new byte[4];

    //如果用的是41协议则是true
    private boolean use41Extensions = true;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public MysqlIO(String host, int port, String user, String password, String database) {
        try {
            //首先进行三次握手
            Socket client = new Socket(host, port);
            //创建输入输出流
            this.mysqlInput = client.getInputStream();
            this.mysqlOutput = new BufferedOutputStream(client.getOutputStream());
            //读取相应包
            doHandshake(user, password, database);
            checkErrorPacket();

            /*Writer writer = new OutputStreamWriter(client.getOutputStream());
            writer.write("Hello Server.");
            writer.flush();//写完后要记得flush
            writer.close();
            client.close();*/
        } catch (IOException ioe) {
            System.out.println("连接出现问题");
        } catch (Exception e) {
            System.out.println("出现异常");
        }

    }






    void doHandshake(String user, String password, String database) throws Exception {
        this.readPacketSequence = 0;
        Buffer buf = readPacket();
        this.protocolVersion = buf.readByte();
        System.out.println(this.protocolVersion);
        this.serverVersion = buf.readString();
        System.out.println(this.serverVersion);
        threadId = buf.readLong();
        System.out.println(threadId);
        this.seed = buf.readString();
        System.out.println(this.seed);

        this.serverCapabilities = 0;

        if (buf.getPosition() < buf.getBufLength()) {
            this.serverCapabilities = buf.readInt();
            System.out.println(this.serverCapabilities);
        }

        this.maxThreeBytes = (256 * 256 * 256) - 1;


        if (this.protocolVersion > 9) {
            int position = buf.getPosition();

            /* New protocol with 16 bytes to describe server characteristics */
            //字符集
            this.serverCharsetIndex = buf.readByte() & 0xff;
            System.out.println("charsetIndex="+this.serverCharsetIndex);
            //服务器状态
            this.serverStatus = buf.readInt();
            //检查事务状态
            // checkTransactionState(0);
            //获取服务器容量
            this.serverCapabilities += 65536 * buf.readInt();

            this.authPluginDataLength = buf.readByte() & 0xff;
            System.out.println(this.authPluginDataLength);
            buf.setPosition(position + 16);
            //最后的一段是第二个seed Part
            String seedPart2 = buf.readString();
            StringBuffer newSeed = new StringBuffer(20);
            newSeed.append(this.seed);
            newSeed.append(seedPart2);
            //将两个seed part组合起来,组成完整的buff
            this.seed = newSeed.toString();
            System.out.println(this.seed);
        }
        if((database != null) && (database.length() > 0)) {
            this.clientParam |= CLIENT_CONNECT_WITH_DB;
        }

        this.clientParam |= CLIENT_MULTI_RESULTS;
        this.use41Extensions = true;

        this.clientParam |= CLIENT_LONG_PASSWORD;

        this.clientParam |= CLIENT_PROTOCOL_41;

        int passwordLength = password.length();
        int userLength = (user != null) ? user.length() : 0;
        int databaseLength = (database != null) ? database.length() : 0;
        int packLength = ((userLength + passwordLength + databaseLength)*2)  + 7 + HEADER_LENGTH +
 AUTH_411_OVERHEAD;
        System.out.println("packetLength = "+packLength);
        Buffer packet = null;
        //当前不考虑SSL
        //由于协议>9所以采用411格式

        System.out.println("当前系统压缩"+(this.serverCapabilities & CLIENT_COMPRESS));
        secureAuth411(null, packLength, user, password, database,true);

    }








    void secureAuth411(Buffer packet, int packLength, String user, String password, String database,
                       boolean writeClientParams) throws Exception {
        if (packet == null) {
            packet = new Buffer(packLength);
        }

        if (writeClientParams) {
            if (this.use41Extensions) {
                this.clientParam=0x0007a685;
                packet.writeLong(this.clientParam);
                packet.writeLong(this.maxThreeBytes);


                packet.writeByte((byte) 8);


                packet.writeBytesNoNull(new byte[23]);
            } else {
                packet.writeInt((int) this.clientParam);
                packet.writeLongInt(this.maxThreeBytes);
            }
        }
       // byte[] userByte = {'r','o','o','t','\0'};
      //  packet.writeBytesNoNull(userByte);
        packet.writeString(user);
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");

        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!");
        System.out.println("Length="+user.getBytes().length);

        System.out.println("User Byte length"+user.getBytes().length);
        byte[] encyptPassword = Security.scramble411(password,seed,"UTF-8");
        System.out.println("密码长度"+encyptPassword.length);
        if(password.length() != 0){
            packet.writeByte((byte) 0x14);

            packet.writeBytesNoNull(encyptPassword);

        }

        System.out.println("Password="+password);
        System.out.println("EncryPassword="+encyptPassword);
      //  System.out.println("EncryPassword2="+encyptPa2);
        //写入database
        if ((database != null) && (database.length() > 0)) {
            packet.writeString(database);
        }

        send(packet, packet.getPosition());
    }




    private final void send(Buffer packet, int packetLen) throws SQLException {
        try {

            //这边的包序列号++
            this.packetSequence++;

            Buffer packetToSend = packet;
            packetToSend.setPosition(0);
            //写入包的长度
            packetToSend.writeLongInt(57);//packetLen - HEADER_LENGTH);
            //写入序列号
            packetToSend.writeByte(this.packetSequence);

            //在这边写入outputStream
            this.mysqlOutput.write(packetToSend.getByteBuffer(), 0, packetLen);
            //把buffer直接刷出去
            this.mysqlOutput.flush();
        }catch(IOException e) {
            System.out.println("发送包错误 " + e);
        }


}

    protected final Buffer readPacket() throws IOException {
        int lengthRead = readFully(this.mysqlInput, this.packetHeaderBuf, 0, 4);

        //这个长度的高低字节方式不同
        int packetLength = (this.packetHeaderBuf[0] & 0xff) +
                               ((this.packetHeaderBuf[1] & 0xff) << 8) +
                               ((this.packetHeaderBuf[2] & 0xff) << 16);
        //MySql包的序列号,保持幂等性
        byte multiPacketSeq = this.packetHeaderBuf[3];

        //重新记录下这个最新的序列号
        this.readPacketSequence = multiPacketSeq;

        // Read data
        //准备packetLength+1大小的缓冲
        byte[] buffer = new byte[packetLength + 1];
        //这个函数是在里面循环的读,直到独到packetLength为止
        int numBytesRead = readFully(this.mysqlInput, buffer, 0, packetLength);
        //如果长度不相等,抛出异常
        if (numBytesRead != packetLength) {
            throw new IOException("Short read, expected " +
                                      packetLength + " bytes, only read " + numBytesRead);
        }
        buffer[packetLength] = 0;
        //初始化Buffer
        Buffer packet = new Buffer(buffer);
        packet.setBufLength(packetLength + 1);
        return packet;
    }

    private final void forceClose() {
        try {
            this.rawSocket.close();
        } catch (IOException e) {
            System.out.println("关闭socket失败" + e);
        } finally {
            this.mysqlOutput = null;
            this.mysqlInput = null;
            this.rawSocket = null;
        }

    }

    private void checkErrorPacket() throws IOException{
        Buffer resultPacket = readPacket();
        int statusCode = resultPacket.readByte();

        //field_count 如果是0 表明ok包 如果是ff 表明是 错误包
        System.out.println("StatusCode"+statusCode);
        if (statusCode == (byte) 0xff) {
            String serverErrorMessage;
            int errno = 2000;
            if (this.protocolVersion > 9) {
                errno = resultPacket.readInt();
                System.out.println(errno);
            }
            serverErrorMessage = resultPacket.readString();
            System.out.println("ROOTBytes"+" "+"root".getBytes());
            System.out.println("======================>");
            for(byte unit : "root".getBytes()){
                System.out.print(unit);
            }
            System.out.println();
            System.out.println("======================>");

            for(byte unit : serverErrorMessage.getBytes()){
                System.out.print(unit);
            }

            System.out.println();
            System.out.println("======================>");
            for(byte unit : "Access denied for user ".getBytes()){
                System.out.print(unit);
            }


          //  String s1 = serverErrorMessage.substring();
          //  String s2 = s1.endsWith('\'');
            System.out.println();
            System.out.println("======================>");
            System.out.println(serverErrorMessage);

        }else if(statusCode == (byte) 0x0000){
            System.out.println("Yeah 三次握手成功");
        }else{
            System.out.println("未知");
        }
    }

    private final int readFully(InputStream in, byte[] b, int off, int len) throws IOException {
        if (len < 0) {
            throw new IndexOutOfBoundsException();
        }

        int n = 0;

        while (n < len) {
            int count = in.read(b, off + n, len - n);

            if (count < 0) {
                throw new IOException("读取对应数据长度错误");
            }

            n += count;
        }

        return n;
    }

    public Socket getRawSocket() {
        return rawSocket;
    }

    public void setRawSocket(Socket rawSocket) {
        this.rawSocket = rawSocket;
    }
}
