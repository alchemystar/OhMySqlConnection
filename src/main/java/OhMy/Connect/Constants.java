/*
 * Copyright (C) 2014 alchemystar, Inc. All Rights Reserved.
 */
package OhMy.Connect;

/**
 * @author lizhuyang
 */
public class Constants {
    /**
     * Avoids allocation of empty byte[] when representing 0-length strings.
     */
    public final static byte[] EMPTY_BYTE_ARRAY = new byte[0];

    public final static byte[] SLASH_STAR_SPACE_AS_BYTES = new byte[] {
                                                                          (byte) '/', (byte) '*', (byte) ' ' };

    public final static byte[] SPACE_STAR_SLASH_SPACE_AS_BYTES = new byte[] {
                                                                                (byte) ' ', (byte) '*', (byte) '/', (byte) ' ' };

    /**
     * Prevents instantiation
     */
    private Constants() {
    }
}