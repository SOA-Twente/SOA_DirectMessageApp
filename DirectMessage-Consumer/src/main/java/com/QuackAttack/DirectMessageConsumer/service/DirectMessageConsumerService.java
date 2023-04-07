package com.QuackAttack.DirectMessageConsumer.service;

import com.QuackAttack.DirectMessageConsumer.objects.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@Slf4j
public class DirectMessageConsumerService {

    private RestTemplate restTemplate;

    private JdbcTemplate jdbcTemplate;

    @Value("${service-profile-url}")
    private String profileServiceURL;

    @Value("${service-postmessage-url}")
    private String postMessageServiceURL;

    // TODO specify the get conversation message queue

    /**
     * This JmsListener listens on the "create a conversation" queue. It takes requests from that queue and first checks
     * if a conversation with the 2 users already exists, if so, it returns a message that the conversation already
     * exists. Else, it will create a new entry in the database for the conversation and return a message that the
     * conversation has been created.
     * @param request it receives from the message queue.
     */
    @JmsListener(destination = "${active-mq.create-conversation-queue}")
    public void createConvo(GetConvoRequest request) {
        // create a conversation with the request values, but first check if the conversation exists
        List<Conversation> conversations = doesConvoExists(request);

        // if the conversation does not exist, create a new entry with request values
        if (conversations.size() == 0) {
            log.info("Conversation does not exist, creating new conversation");
            String sql = "INSERT INTO conversations (UserInitiator, UserReceiver)";

            int rows = jdbcTemplate.update(sql, request.getInitiator(), request.getReceiver());

            if (rows > 0) {
                log.info("A new conversation has been created");
            } else {
                log.error("An error in creating a conversation occurred");
                // TODO return message to original requester that the
            }
        } else {
            log.info("Conversation exists");
        }

        // TODO return the created conversation or message to the original requester

    }


    /**
     * This JmsListener listens on the "get conversation" queue. It takes requests from that queue and first checks
     * if the conversations exists, if it exists, it will then select the messages from the database with a matching
     * conversation ID. It will then return a list of messages to the original requester. // TODO to be implemented
     * @param request a conversation request.
     */
    @JmsListener(destination = "${active-mq.get-conversation-queue}")
    public void getConvo(GetConvoRequest request) {
        // request the messages with the found convoID

        List<Conversation> conversations = doesConvoExists(request);
        if (conversations.size() > 0) {
            // return list of messages from the messages table
            int conversationID = conversations.get(0).getConvoID();

            String sql = "SELECT * FROM messages WHERE convoID = ?";

            try {
                List<Message> messages = jdbcTemplate.query(sql
                        , new MessageRowMapper()
                        , new Object[]{conversationID});

                // TODO return the messages to the original requester
            } catch (DataAccessException e) {
                log.error("Error querying messages from request init:" + request.getInitiator()
                        + ", receiver: " + request.getReceiver() + " and convoID: " + conversationID);
                throw new RuntimeException(e);
            }

        } else {
            log.info("Conversation does not yet exist, please first make a conversation");
        }
    }

    // TODO specify the send message message queue
    @JmsListener(destination = "${active-mq.send-msg-queue}")
    public void sendMsg(MessageRequest request) {
        String sql = "INSERT INTO messages (convoID, sender, receiver, message) VALUES ( ?, ?, ?, ?)";

        try {
            int rows = jdbcTemplate.update(sql, request.getConvoID(), request.getSender(), request.getReceiver(), request.getMessage());
            if (rows > 0) {
                // message was send successfully
                log.info("message was sent");
            } else {
                log.error("message was not sent, error location is directquack");
            }

        } catch (DataAccessException e) {
            log.error("Error sending the message for request: " + request.getReceiver() + ", sender: " + request.getSender()
                    + ", receiver: " + request.getReceiver() + ", message : " + request.getMessage());
        }
    }

    /**
     * Help function to check if the conversation already exists in the database.
     * @param request for a conversation with a conversation ID.
     * @return a list containing the conversation.
     */
    public List<Conversation> doesConvoExists(GetConvoRequest request) {

        String sqlIfExist = "SELECT convoID FROM conversations WHERE " +
                "(UserInitiator = ? AND UserReceiver = ?) OR (UserInitiator = ? AND UserReceiver = ?)";

        List<Conversation> conversationList = jdbcTemplate.query(sqlIfExist
                , new ConversationRowMapper()
                , new Object[]{request.getInitiator(), request.getReceiver(), request.getReceiver(), request.getInitiator()});

        return conversationList;
    }
}
