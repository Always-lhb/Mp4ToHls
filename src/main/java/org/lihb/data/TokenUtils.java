package org.lihb.data;


/**
 * Token的加解密工具类，根据具体要求自己实现
 *
 * @author lihb
 */
public class TokenUtils {

    public static String encrypt(Token token) {
        try {
            return token.toRawString();
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to encrypt token", e);
        }
    }

    public static Token decrypt(String str) {
        try {
            return Token.parseToken(str);
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to decrypt token", e);
        }
    }
}
