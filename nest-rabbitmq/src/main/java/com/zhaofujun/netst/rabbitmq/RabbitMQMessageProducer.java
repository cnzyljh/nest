package com.zhaofujun.netst.rabbitmq;

import com.rabbitmq.client.*;
import com.zhaofujun.nest.SystemException;
import com.zhaofujun.nest.context.event.channel.distribute.DistributeMessageProducer;
import com.zhaofujun.nest.context.event.message.MessageInfo;
import com.zhaofujun.nest.utils.JsonUtils;

import java.util.Map;

/**
 *
 **/
public class RabbitMQMessageProducer extends DistributeMessageProducer {

    private ConnectionFactory connectionFactory;

    private  Connection connection=null;

    private  Channel channel=null;

    private BuiltinExchangeType exchangeType;

    private String exchangeName;

    private String routingKey;

    private Map<String,Object> arguments;

    public RabbitMQMessageProducer(RabbitMQProviderConfig rabbitMQProviderConfig){
        this.connectionFactory=new ConnectionFactory();
        this.connectionFactory.setAutomaticRecoveryEnabled(true);
        this.connectionFactory.setHost(rabbitMQProviderConfig.getHost());
        this.connectionFactory.setPort(rabbitMQProviderConfig.getPort());
        this.connectionFactory.setUsername(rabbitMQProviderConfig.getUser());
        this.connectionFactory.setPassword(rabbitMQProviderConfig.getPwd());
        this.exchangeType=rabbitMQProviderConfig.getExchangeType();
        this.exchangeName=rabbitMQProviderConfig.getExchangeName();
        this.routingKey=rabbitMQProviderConfig.getRoutingKey();
        this.arguments=rabbitMQProviderConfig.getArguments();
    }


    @Override
    public void commit(String messageGroup, MessageInfo messageInfo) {
        try {
            connection = connectionFactory.newConnection();
            channel= connection.createChannel();
            channel.exchangeDeclare(this.exchangeName,this.exchangeType,true,false,null);
            channel.queueBind(messageGroup,this.exchangeName,this.routingKey);
            channel.queueDeclare(messageGroup,true,false,false,this.arguments);
            channel.basicPublish(this.exchangeName, this.routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, JsonUtils.toJsonString(messageInfo).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            throw new SystemException("发送RabbitMQ消息失败", e);
        } finally {
            destruction();
        }
    }

    @Override
    public void destruction() {
        try {
            channel.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
            throw new SystemException("关闭RabbitMQ消息通道和链接失败", e);
        }
    }
}