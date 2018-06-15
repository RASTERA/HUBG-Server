// PROJECT HUBG | SERVER
// Henry Tu, Ryan Zhang, Syed Safwaan
// rastera.xyz
// 2018 ICS4U FINAL
//
// Message.java | Message obj transmitted to client

package com.rastera.Networking;

import java.io.Serializable;

public class Message implements Serializable {
    private static final long serialVersionUID = 653214L;
    public int type;
    public Object message;
}
