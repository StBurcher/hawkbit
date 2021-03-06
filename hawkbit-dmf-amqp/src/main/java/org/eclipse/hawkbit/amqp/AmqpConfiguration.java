/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.amqp;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.eclipse.hawkbit.api.ArtifactUrlHandler;
import org.eclipse.hawkbit.api.HostnameResolver;
import org.eclipse.hawkbit.cache.CacheConstants;
import org.eclipse.hawkbit.dmf.amqp.api.AmqpSettings;
import org.eclipse.hawkbit.repository.ArtifactManagement;
import org.eclipse.hawkbit.repository.ControllerManagement;
import org.eclipse.hawkbit.repository.EntityFactory;
import org.eclipse.hawkbit.repository.SystemManagement;
import org.eclipse.hawkbit.repository.TenantConfigurationManagement;
import org.eclipse.hawkbit.security.DdiSecurityProperties;
import org.eclipse.hawkbit.security.SystemSecurityContext;
import org.eclipse.hawkbit.tenancy.TenantAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.ErrorHandler;

import com.google.common.collect.Maps;

/**
 * Spring configuration for AMQP based DMF communication for indirect device
 * integration.
 *
 */
@EnableConfigurationProperties({ AmqpProperties.class, AmqpDeadletterProperties.class })
@ConditionalOnProperty(prefix = "hawkbit.dmf.rabbitmq", name = "enabled", matchIfMissing = true)
public class AmqpConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmqpConfiguration.class);

    @Autowired
    private AmqpProperties amqpProperties;

    @Autowired
    private AmqpDeadletterProperties amqpDeadletterProperties;

    @Autowired
    private ConnectionFactory rabbitConnectionFactory;

    @Configuration
    @ConditionalOnMissingBean(ConnectionFactory.class)
    @ConditionalOnProperty(prefix = "hawkbit.dmf.rabbitmq", name = "enabled", matchIfMissing = true)
    protected static class RabbitConnectionFactoryCreator {

        @Autowired
        private AmqpProperties amqpProperties;

        @Autowired
        @Qualifier("asyncExecutor")
        private Executor threadPoolExecutor;

        @Autowired
        private ScheduledExecutorService scheduledExecutorService;

        /**
         * {@link ConnectionFactory} with enabled publisher confirms and
         * heartbeat.
         * 
         * @param config
         *            with standard {@link RabbitProperties}
         * @return {@link ConnectionFactory}
         */
        @Bean
        public ConnectionFactory rabbitConnectionFactory(final RabbitProperties config) {
            final CachingConnectionFactory factory = new CachingConnectionFactory();
            factory.setRequestedHeartBeat(amqpProperties.getRequestedHeartBeat());
            factory.setExecutor(threadPoolExecutor);
            factory.getRabbitConnectionFactory().setHeartbeatExecutor(scheduledExecutorService);
            factory.setPublisherConfirms(true);

            final String addresses = config.getAddresses();
            factory.setAddresses(addresses);
            if (config.getHost() != null) {
                factory.setHost(config.getHost());
                factory.setPort(config.getPort());
            }
            if (config.getUsername() != null) {
                factory.setUsername(config.getUsername());
            }
            if (config.getPassword() != null) {
                factory.setPassword(config.getPassword());
            }
            if (config.getVirtualHost() != null) {
                factory.setVirtualHost(config.getVirtualHost());
            }
            return factory;
        }
    }

    /**
     * Create a {@link RabbitAdmin} and ignore declaration exceptions.
     * {@link RabbitAdmin#setIgnoreDeclarationExceptions(boolean)}
     * 
     * @return the bean
     */
    @Bean
    public RabbitAdmin rabbitAdmin() {
        final RabbitAdmin rabbitAdmin = new RabbitAdmin(rabbitConnectionFactory);
        rabbitAdmin.setIgnoreDeclarationExceptions(true);
        return rabbitAdmin;
    }

    /**
     * @return {@link RabbitTemplate} with automatic retry, published confirms
     *         and {@link Jackson2JsonMessageConverter}.
     */
    @Bean
    public RabbitTemplate rabbitTemplate() {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(rabbitConnectionFactory);
        rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());

        final RetryTemplate retryTemplate = new RetryTemplate();
        retryTemplate.setBackOffPolicy(new ExponentialBackOffPolicy());
        rabbitTemplate.setRetryTemplate(retryTemplate);

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                LOGGER.debug("Message with {} confirmed by broker.", correlationData);
            } else {
                LOGGER.error("Broker is unable to handle message with {} : {}", correlationData, cause);
            }
        });

        return rabbitTemplate;
    }

    /**
     * Create the DMF API receiver queue for retrieving DMF messages.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue dmfReceiverQueue() {
        return new Queue(amqpProperties.getReceiverQueue(), true, false, false,
                amqpDeadletterProperties.getDeadLetterExchangeArgs(amqpProperties.getDeadLetterExchange()));
    }

    /**
     * Create the DMF API receiver queue for authentication requests called by
     * 3rd party artifact storages for download authorization by devices.
     *
     * @return the receiver queue
     */
    @Bean
    public Queue authenticationReceiverQueue() {
        return QueueBuilder.nonDurable(amqpProperties.getAuthenticationReceiverQueue()).autoDelete()
                .withArguments(getTTLMaxArgsAuthenticationQueue()).build();
    }

    /**
     * Create DMF exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange dmfSenderExchange() {
        return new FanoutExchange(AmqpSettings.DMF_EXCHANGE);
    }

    /**
     * Create the Binding {@link AmqpConfiguration#dmfReceiverQueue()} to
     * {@link AmqpConfiguration#dmfSenderExchange()}.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindDmfSenderExchangeToDmfQueue() {
        return BindingBuilder.bind(dmfReceiverQueue()).to(dmfSenderExchange());
    }

    /**
     * Create authentication exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange authenticationExchange() {
        return new FanoutExchange(AmqpSettings.AUTHENTICATION_EXCHANGE, false, true);
    }

    /**
     * Create the Binding
     * {@link AmqpConfiguration#authenticationReceiverQueue()} to
     * {@link AmqpConfiguration#authenticationExchange()}.
     *
     * @return the binding and create the queue and exchange
     */
    @Bean
    public Binding bindAuthenticationSenderExchangeToAuthenticationQueue() {
        return BindingBuilder.bind(authenticationReceiverQueue()).to(authenticationExchange());
    }

    /**
     * Create dead letter queue.
     *
     * @return the queue
     */
    @Bean
    public Queue deadLetterQueue() {
        return amqpDeadletterProperties.createDeadletterQueue(amqpProperties.getDeadLetterQueue());
    }

    /**
     * Create the dead letter fanout exchange.
     *
     * @return the fanout exchange
     */
    @Bean
    public FanoutExchange deadLetterExchange() {
        return new FanoutExchange(amqpProperties.getDeadLetterExchange());
    }

    /**
     * Create the Binding deadLetterQueue to deadLetterExchange.
     *
     * @return the binding
     */
    @Bean
    public Binding bindDeadLetterQueueToDeadLetterExchange() {
        return BindingBuilder.bind(deadLetterQueue()).to(deadLetterExchange());
    }

    /**
     * Create AMQP handler service bean.
     * 
     * @param rabbitTemplate
     *            for converting messages
     * @param amqpMessageDispatcherService
     *            to sending events to DMF client
     * @param controllerManagement
     *            for target repo access
     * @param entityFactory
     *            to create entities
     *
     * @return handler service bean
     */
    @Bean
    public AmqpMessageHandlerService amqpMessageHandlerService(final RabbitTemplate rabbitTemplate,
            final AmqpMessageDispatcherService amqpMessageDispatcherService,
            final ControllerManagement controllerManagement, final EntityFactory entityFactory) {
        return new AmqpMessageHandlerService(rabbitTemplate, amqpMessageDispatcherService, controllerManagement,
                entityFactory);
    }

    /**
     * Create AMQP handler service bean for authentication messages.
     * 
     * @param rabbitTemplate
     *            for converting messages
     * @param authenticationManager
     *            for target authentication
     * @param artifactManagement
     *            for artifact URI generation
     * @param cache
     *            for download IDs
     * @param hostnameResolver
     *            for resolving the host for downloads
     * @param controllerManagement
     *            for target repo access
     * @return handler service bean
     */
    @Bean
    public AmqpAuthenticationMessageHandler amqpAuthenticationMessageHandler(final RabbitTemplate rabbitTemplate,
            final AmqpControllerAuthentication authenticationManager, final ArtifactManagement artifactManagement,
            @Qualifier(CacheConstants.DOWNLOAD_ID_CACHE) final Cache cache, final HostnameResolver hostnameResolver,
            final ControllerManagement controllerManagement) {
        return new AmqpAuthenticationMessageHandler(rabbitTemplate, authenticationManager, artifactManagement, cache,
                hostnameResolver, controllerManagement);
    }

    /**
     * Create default amqp sender service bean.
     *
     * @return the default amqp sender service bean
     */
    @Bean
    @ConditionalOnMissingBean
    public AmqpSenderService amqpSenderServiceBean() {
        return new DefaultAmqpSenderService(rabbitTemplate());
    }

    /**
     * Returns the Listener factory.
     * 
     * @param errorHandler
     *            the error hander
     * @return the {@link SimpleMessageListenerContainer} that gets used receive
     *         AMQP messages
     */
    @Bean(name = { "listenerContainerFactory" })
    public RabbitListenerContainerFactory<SimpleMessageListenerContainer> listenerContainerFactory(
            final ErrorHandler errorHandler) {
        return new ConfigurableRabbitListenerContainerFactory(amqpProperties, rabbitConnectionFactory, errorHandler);
    }

    @Bean
    @ConditionalOnMissingBean(AmqpControllerAuthentication.class)
    public AmqpControllerAuthentication amqpControllerAuthentication(final SystemManagement systemManagement,
            final ControllerManagement controllerManagement,
            final TenantConfigurationManagement tenantConfigurationManagement, final TenantAware tenantAware,
            final DdiSecurityProperties ddiSecruityProperties, final SystemSecurityContext systemSecurityContext) {
        return new AmqpControllerAuthentication(systemManagement, controllerManagement, tenantConfigurationManagement,
                tenantAware, ddiSecruityProperties, systemSecurityContext);
    }

    @Bean
    @ConditionalOnMissingBean(AmqpMessageDispatcherService.class)
    public AmqpMessageDispatcherService amqpMessageDispatcherService(final RabbitTemplate rabbitTemplate,
            final AmqpSenderService amqpSenderService, final ArtifactUrlHandler artifactUrlHandler,
            final SystemSecurityContext systemSecurityContext, final SystemManagement systemManagement) {
        return new AmqpMessageDispatcherService(rabbitTemplate, amqpSenderService, artifactUrlHandler,
                systemSecurityContext, systemManagement);
    }

    private static Map<String, Object> getTTLMaxArgsAuthenticationQueue() {
        final Map<String, Object> args = Maps.newHashMapWithExpectedSize(2);
        args.put("x-message-ttl", Duration.ofSeconds(30).toMillis());
        args.put("x-max-length", 1_000);
        return args;
    }

}
