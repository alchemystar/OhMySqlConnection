/*
 * Copyright (C) 2014 alchemystar, Inc. All Rights Reserved.
 */
package OhMy.Connect;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;

/**
 * @author lizhuyang
 */
//当前不考虑encoding的问题
public class Buffer {
    static final int MAX_BYTES_TO_DUMP = 512;

    static final int NO_LENGTH_LIMIT = -1;

    static final long NULL_LENGTH = -1;

    private int bufLength = 0;

    private byte[] byteBuffer;

    private int position = 0;

    public Buffer(byte[] buf) {
        this.byteBuffer = buf;
        setBufLength(buf.length);
    }

    Buffer(int size) {
        this.byteBuffer = new byte[size];
        setBufLength(this.byteBuffer.length);
        this.position = MysqlIO.HEADER_LENGTH;
    }

    final void clear() {
        this.position = MysqlIO.HEADER_LENGTH;
    }

    int getCapacity() {
        return this.byteBuffer.length;
    }

    byte[] getBytes(int offset, int len) {
        byte[] dest = new byte[len];
        System.arraycopy(this.byteBuffer, offset, dest, 0, len);

        return dest;
    }

    public byte[] getByteBuffer() {
        return byteBuffer;
    }

    public void setByteBuffer(byte[] byteBuffer) {
        this.byteBuffer = byteBuffer;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public int getBufLength() {
        return bufLength;
    }

    public void setBufLength(int bufLength) {
        this.bufLength = bufLength;
    }


    final long newReadLength() {
        int sw = this.byteBuffer[this.position++] & 0xff;

        switch (sw) {
            case 251:
                return 0;

            case 252:
                return readInt();

            case 253:
                return readLongInt();

            case 254: // changed for 64 bit lengths
                return readLongLong();

            default:
                return sw;
        }
    }

    final byte readByte() {
        return this.byteBuffer[this.position++];
    }

    final byte readByte(int readAt) {
        return this.byteBuffer[readAt];
    }

    final byte[] getBytes(int len) {
        byte[] b = new byte[len];
        System.arraycopy(this.byteBuffer, this.position, b, 0, len);
        this.position += len; // update cursor

        return b;
    }


    final long readFieldLength() {
        int sw = this.byteBuffer[this.position++] & 0xff;

        switch (sw) {
            case 251:
                return NULL_LENGTH;

            case 252:
                return readInt();

            case 253:
                return readLongInt();

            case 254:
                return readLongLong();

            default:
                return sw;
        }
    }

    // 2000-06-05 Changed
    final int readInt() {
        byte[] b = this.byteBuffer; // a little bit optimization

        return (b[this.position++] & 0xff) | ((b[this.position++] & 0xff) << 8);
    }

    final int readIntAsLong() {
        byte[] b = this.byteBuffer;

        return (b[this.position++] & 0xff) | ((b[this.position++] & 0xff) << 8)
                   | ((b[this.position++] & 0xff) << 16)
                   | ((b[this.position++] & 0xff) << 24);
    }

    final byte[] readLenByteArray(int offset) {
        long len = this.readFieldLength();

        if (len == NULL_LENGTH) {
            return null;
        }

        if (len == 0) {
            return Constants.EMPTY_BYTE_ARRAY;
        }

        this.position += offset;

        return getBytes((int) len);
    }

    final long readLength() {
        int sw = this.byteBuffer[this.position++] & 0xff;

        switch (sw) {
            case 251:
                return 0;

            case 252:
                return readInt();

            case 253:
                return readLongInt();

            case 254:
                return readLong();

            default:
                return sw;
        }
    }

    // 2000-06-05 Fixed
    final long readLong() {
        byte[] b = this.byteBuffer;

        return ((long) b[this.position++] & 0xff)
                   | (((long) b[this.position++] & 0xff) << 8)
                   | ((long) (b[this.position++] & 0xff) << 16)
                   | ((long) (b[this.position++] & 0xff) << 24);
    }

    // 2000-06-05 Changed
    final int readLongInt() {
        byte[] b = this.byteBuffer;

        return (b[this.position++] & 0xff) | ((b[this.position++] & 0xff) << 8)
                   | ((b[this.position++] & 0xff) << 16);
    }

    // 2000-06-05 Fixed
    final long readLongLong() {
        byte[] b = this.byteBuffer;

        return (b[this.position++] & 0xff)
                   | ((long) (b[this.position++] & 0xff) << 8)
                   | ((long) (b[this.position++] & 0xff) << 16)
                   | ((long) (b[this.position++] & 0xff) << 24)
                   | ((long) (b[this.position++] & 0xff) << 32)
                   | ((long) (b[this.position++] & 0xff) << 40)
                   | ((long) (b[this.position++] & 0xff) << 48)
                   | ((long) (b[this.position++] & 0xff) << 56);
    }

    final int readnBytes() {
        int sw = this.byteBuffer[this.position++] & 0xff;

        switch (sw) {
            case 1:
                return this.byteBuffer[this.position++] & 0xff;

            case 2:
                return this.readInt();

            case 3:
                return this.readLongInt();

            case 4:
                return (int) this.readLong();

            default:
                return 255;
        }
    }

    //
    // Read a null-terminated string
    //
    // To avoid alloc'ing a new byte array, we
    // do this by hand, rather than calling getNullTerminatedBytes()
    //
    public final String readString() {
        int i = this.position;
        int len = 0;
        int maxLen = getBufLength();

        while ((i < maxLen) && (this.byteBuffer[i] != 0)) {
            len++;
            i++;
        }

        String s = StringUtils.toString(this.byteBuffer, this.position, len);
        this.position += (len + 1); // update cursor

        return s;
    }

    final String readString(String encoding) throws Exception {
        int i = this.position;
        int len = 0;
        int maxLen = getBufLength();

        while ((i < maxLen) && (this.byteBuffer[i] != 0)) {
            len++;
            i++;
        }

        try {
            return StringUtils.toString(this.byteBuffer, this.position, len, encoding);
        } catch (UnsupportedEncodingException uEE) {
           throw new Exception("Read String error");
        } finally {
            this.position += (len + 1); // update cursor
        }
    }

    /**
     * Read a fixed length string
     */
    final String readString(String encoding, int expectedLength) throws Exception {
        int i = this.position;
        int len = 0;
        int maxLen = getBufLength();

        while ((i < maxLen) && (len < expectedLength) && (this.byteBuffer[i] != 0)) {
            len++;
            i++;
        }

        if (len < expectedLength) {
            throw new Exception("read String error");
        }

        try {
            return StringUtils.toString(this.byteBuffer, this.position, len, encoding);
        } catch (UnsupportedEncodingException uEE) {
            //$NON-NLS-1$
            throw new Exception();
        } finally {
            this.position += len; // update cursor
        }
    }


    public final void writeByte(byte b) throws SQLException {
        ensureCapacity(1);

        this.byteBuffer[this.position++] = b;
    }

    // Write a byte array
    public final void writeBytesNoNull(byte[] bytes) throws SQLException {
        int len = bytes.length;
        ensureCapacity(len);
        System.arraycopy(bytes, 0, this.byteBuffer, this.position, len);
        this.position += len;
    }

    // Write a byte array with the given offset and length
    final void writeBytesNoNull(byte[] bytes, int offset, int length)
        throws SQLException {
        ensureCapacity(length);
        System.arraycopy(bytes, offset, this.byteBuffer, this.position, length);
        this.position += length;
    }

    final void writeDouble(double d) throws SQLException {
        long l = Double.doubleToLongBits(d);
        writeLongLong(l);
    }

    final void writeFieldLength(long length) throws SQLException {
        if (length < 251) {
            writeByte((byte) length);
        } else if (length < 65536L) {
            ensureCapacity(3);
            writeByte((byte) 252);
            writeInt((int) length);
        } else if (length < 16777216L) {
            ensureCapacity(4);
            writeByte((byte) 253);
            writeLongInt((int) length);
        } else {
            ensureCapacity(9);
            writeByte((byte) 254);
            writeLongLong(length);
        }
    }

    final void writeFloat(float f) throws SQLException {
        ensureCapacity(4);

        int i = Float.floatToIntBits(f);
        byte[] b = this.byteBuffer;
        b[this.position++] = (byte) (i & 0xff);
        b[this.position++] = (byte) (i >>> 8);
        b[this.position++] = (byte) (i >>> 16);
        b[this.position++] = (byte) (i >>> 24);
    }

    // 2000-06-05 Changed
    final void writeInt(int i) throws SQLException {
        ensureCapacity(2);

        byte[] b = this.byteBuffer;
        b[this.position++] = (byte) (i & 0xff);
        b[this.position++] = (byte) (i >>> 8);
    }

    // Write a String using the specified character
    // encoding
    final void writeLenBytes(byte[] b) throws SQLException {
        int len = b.length;
        ensureCapacity(len + 9);
        writeFieldLength(len);
        System.arraycopy(b, 0, this.byteBuffer, this.position, len);
        this.position += len;
    }



    // 2000-06-05 Changed
    final void writeLong(long i) throws SQLException {
        ensureCapacity(4);

        byte[] b = this.byteBuffer;
        b[this.position++] = (byte) (i & 0xff);
        b[this.position++] = (byte) (i >>> 8);
        b[this.position++] = (byte) (i >>> 16);
        b[this.position++] = (byte) (i >>> 24);
    }

    // 2000-06-05 Changed
    final void writeLongInt(int i) throws SQLException {
        ensureCapacity(3);
        byte[] b = this.byteBuffer;
        b[this.position++] = (byte) (i & 0xff);
        b[this.position++] = (byte) (i >>> 8);
        b[this.position++] = (byte) (i >>> 16);
    }

    final void writeLongLong(long i) throws SQLException {
        ensureCapacity(8);
        byte[] b = this.byteBuffer;
        b[this.position++] = (byte) (i & 0xff);
        b[this.position++] = (byte) (i >>> 8);
        b[this.position++] = (byte) (i >>> 16);
        b[this.position++] = (byte) (i >>> 24);
        b[this.position++] = (byte) (i >>> 32);
        b[this.position++] = (byte) (i >>> 40);
        b[this.position++] = (byte) (i >>> 48);
        b[this.position++] = (byte) (i >>> 56);
    }

    // Write null-terminated string
    final void writeString(String s) throws SQLException {
        ensureCapacity((s.length() * 2) + 1);
        writeStringNoNull(s);
        this.byteBuffer[this.position++] = 0;
    }



    // Write string, with no termination
    final void writeStringNoNull(String s) throws SQLException {
        int len = s.length();
        ensureCapacity(len * 2);
        System.arraycopy(StringUtils.getBytes(s), 0, this.byteBuffer, this.position, len);
        this.position += len;

        // for (int i = 0; i < len; i++)
        // {
        // this.byteBuffer[this.position++] = (byte)s.charAt(i);
        // }
    }




    final void ensureCapacity(int additionalData) throws SQLException {
        if ((this.position + additionalData) > getBufLength()) {
            if ((this.position + additionalData) < this.byteBuffer.length) {
                // byteBuffer.length is != getBufLength() all of the time
                // due to re-using of packets (we don't shrink them)
                //
                // If we can, don't re-alloc, just set buffer length
                // to size of current buffer
                setBufLength(this.byteBuffer.length);
            } else {
                //
                // Otherwise, re-size, and pad so we can avoid
                // allocing again in the near future
                //
                int newLength = (int) (this.byteBuffer.length * 1.25);

                if (newLength < (this.byteBuffer.length + additionalData)) {
                    newLength = this.byteBuffer.length
                                    + (int) (additionalData * 1.25);
                }

                if (newLength < this.byteBuffer.length) {
                    newLength = this.byteBuffer.length + additionalData;
                }

                byte[] newBytes = new byte[newLength];

                System.arraycopy(this.byteBuffer, 0, newBytes, 0,
                                    this.byteBuffer.length);
                this.byteBuffer = newBytes;
                setBufLength(this.byteBuffer.length);
            }
        }
    }
}
