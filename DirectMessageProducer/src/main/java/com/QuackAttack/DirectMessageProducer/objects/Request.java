package com.QuackAttack.DirectMessageProducer.objects;

public interface Request {
    String correlationID = null;
    String initiator = null;
    String receiver = null;

    Object getInitiator();

    Object getReceiver();
}
