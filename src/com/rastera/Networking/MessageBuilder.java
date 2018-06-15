// PROJECT HUBG | SERVER
// Henry Tu, Ryan Zhang, Syed Safwaan
// rastera.xyz
// 2018 ICS4U FINAL
//
// MessageBuilder.java | Utility to construct Message object from any object

package com.rastera.Networking;

class MessageBuilder {
    public static Message messageBuilder(int type, Object message) {
        Message nMessage = new Message();

        nMessage.type = type;
        nMessage.message = message;

        return nMessage;
    }
}
