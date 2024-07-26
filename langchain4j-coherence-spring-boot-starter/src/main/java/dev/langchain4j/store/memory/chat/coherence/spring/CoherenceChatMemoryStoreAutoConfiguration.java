package dev.langchain4j.store.memory.chat.coherence.spring;

import com.oracle.coherence.spring.boot.autoconfigure.CoherenceAutoConfiguration;
import com.oracle.coherence.spring.configuration.annotation.EnableCoherence;
import com.tangosol.net.Coherence;
import dev.langchain4j.store.memory.chat.coherence.CoherenceChatMemoryStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.Optional;

import static dev.langchain4j.store.memory.chat.coherence.spring.CoherenceChatMemoryStoreProperties.PREFIX;


/**
 * The automatic configuration for a Coherence chat memory store.
 */
@AutoConfiguration
@EnableCoherence
@AutoConfigureAfter(CoherenceAutoConfiguration.class)
@EnableConfigurationProperties(CoherenceChatMemoryStoreProperties.class)
@ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class CoherenceChatMemoryStoreAutoConfiguration
    {
    /**
     * Create a {@link CoherenceChatMemoryStore}.
     *
     * @param properties  the Spring properties
     *
     * @return a new instance of a {@link CoherenceChatMemoryStore}
     */
    @Bean
    @ConditionalOnMissingBean
    public CoherenceChatMemoryStore coherenceChatMemoryStore(CoherenceChatMemoryStoreProperties properties) {

        String session = Optional.ofNullable(properties.getSession()).orElse(Coherence.DEFAULT_NAME);
        String cache = Optional.ofNullable(properties.getName()).orElse(CoherenceChatMemoryStore.DEFAULT_MAP_NAME);

        return CoherenceChatMemoryStore.builder()
                .session(session)
                .name(cache)
                .build();
    }
}
