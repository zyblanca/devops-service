package io.choerodon.devops.infra.config;


import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j
public class RabbitConfig {
    /**
     * 死信交换机
     */
    public static final String PIPELINE_DEAD_EXCHANGE = "pipeline.dead.exchange";

    /**
     * 正常业务交换机
     */
    public static final String PIPELINE_DEFAULT_EXCHANGE = "pipeline.exchange";

    /**
     * GIT_LAB队列
     */
    public static final String PIPELINE_GIT_LAB_QUEUE = "pipeline.gitlab";

    /**
     * 死信队列
     */
    public static final String PIPELINE_DEAD_QUEUE = "pipeline.dead.queue";

    /**
     * 死信队列与死信交换机绑定路由
     */
    public static final String PIPELINE_DEAD_ROUTING_KEY = "pipeline.dead.routing.key.#";

    /**
     * 业务交换机
     *
     * @return
     */
    @Bean("businessExchange")
    public TopicExchange businessExchange() {
        return new TopicExchange(PIPELINE_DEFAULT_EXCHANGE,true,false);
    }

    /**
     * 死信交换机
     *
     * @return
     */
    @Bean("deadLetterExchange")
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(PIPELINE_DEAD_EXCHANGE, true, false);
    }


    /**
     * 业务队列 绑定死信队列
     * @return
     */
    @Bean(name = "deadQueue")
    public Queue deadQueue() {
        return new Queue(PIPELINE_DEAD_QUEUE);
    }

    /**
     * 业务队列绑定业务交换机和路由
     * @return
     */
    @Bean(name = "deadBinging")
    public Binding binding() {
        return BindingBuilder.bind(deadQueue()).to(deadLetterExchange()).with(PIPELINE_DEAD_ROUTING_KEY);
    }

    /**
     * x-dead-letter-exchange    这里声明当前队列绑定的死信交换机
     * x-dead-letter-routing-key  这里声明当前队列的死信路由key
     */
    public Map<String, Object> getArgs() {
        Map<String, Object> args = new HashMap<>(2);
        args.put("x-dead-letter-exchange", PIPELINE_DEAD_EXCHANGE);
        args.put("x-dead-letter-routing-key", PIPELINE_DEAD_ROUTING_KEY);
        return args;
    }

    /**
     * 业务队列 绑定死信队列
     * @return
     */
    @Bean(name = "messageQueue")
    public Queue messageQueue() {
        return new Queue(PIPELINE_GIT_LAB_QUEUE,true,false,false,getArgs());
    }

    /**
     * 业务队列绑定业务交换机和路由
     * @return
     */
    @Bean(name = "default")
    public Binding defaultBinding() {
        return BindingBuilder.bind(messageQueue()).to(businessExchange()).with(PIPELINE_GIT_LAB_QUEUE+".#");
    }

}
