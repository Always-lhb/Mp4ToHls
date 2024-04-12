package org.lihb.data;

/**
 * @author lihb
 */
public class Token {

    private int userId;

    private long createdTime;


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public String toRawString() {
        return userId + "::" + createdTime;
    }

    public static Token parseToken(String str) {
        if (str == null || str.length() == 0) {
            return null;
        }

        Token token = new Token();
        String[] tmp = str.split("::");
        if (tmp.length != 2) {
            return null;
        }
        int userId = Integer.parseInt(tmp[0]);
        if (userId < 0) {
            return null;
        }
        long createdTime = Long.parseLong(tmp[1]);
        if (createdTime == 0L) {
            return null;
        }
        token.setUserId(userId);
        token.setCreatedTime(createdTime);
        return token;
    }
}
