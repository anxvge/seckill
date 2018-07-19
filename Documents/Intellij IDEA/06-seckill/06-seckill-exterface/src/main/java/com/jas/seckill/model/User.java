package com.jas.seckill.model;

import java.io.Serializable;

/**
 * ClassName:User
 * Package:com.jas.seckill.model
 * Descrip:
 *
 * @Date:2018/7/17 下午8:38
 * @Author:jas
 */
public class User implements Serializable {
    private String uid;
    private String uname;

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getUname() {
        return uname;
    }

    public void setUname(String uname) {
        this.uname = uname;
    }
}
