/*
 * Copyright (C) 2014 alchemystar, Inc. All Rights Reserved.
 */
package OhMy.Connect;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author lizhuyang
 */
public class Security {

    public static byte[] scramble411(String password, String seed, String encoding)
        throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-1"); //$NON-NLS-1$
        String passwordEncoding = encoding;

        byte[] passwordHashStage1 = md
                                        .digest((passwordEncoding == null || passwordEncoding.length() == 0) ?
                                                    StringUtils.getBytes(password)
                                                    : StringUtils.getBytes(password, passwordEncoding));
        md.reset();

        byte[] passwordHashStage2 = md.digest(passwordHashStage1);
        md.reset();

        byte[] seedAsBytes = StringUtils.getBytes(seed, "ASCII"); // for debugging
        md.update(seedAsBytes);
        md.update(passwordHashStage2);

        byte[] toBeXord = md.digest();

        int numToXor = toBeXord.length;

        for (int i = 0; i < numToXor; i++) {
            toBeXord[i] = (byte) (toBeXord[i] ^ passwordHashStage1[i]);
        }

        return toBeXord;
    }


    public static final byte[] scramble411Hei(byte[] pass, byte[] seed) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        byte[] pass1 = md.digest(pass);
        md.reset();
        byte[] pass2 = md.digest(pass1);
        md.reset();
        md.update(seed);
        byte[] pass3 = md.digest(pass2);
        for (int i = 0; i < pass3.length; i++) {
            pass3[i] = (byte) (pass3[i] ^ pass1[i]);
        }
        return pass3;
    }
}
