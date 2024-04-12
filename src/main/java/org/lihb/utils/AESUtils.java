package org.lihb.utils;

import javax.crypto.KeyGenerator;
import java.security.NoSuchAlgorithmException;

/**
 * @author lihb
 */
public class AESUtils {

    public static byte[] genAES128Key() {
        try {
            // 生成一个AES密钥生成器实例
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");

            // 初始化密钥生成器，指定密钥长度为128位
            keyGenerator.init(128);

            // 生成AES密钥
            byte[] aesKey = keyGenerator.generateKey().getEncoded();
            return aesKey;
        } catch (NoSuchAlgorithmException e) {
            return new byte[0];
        }
    }
}
