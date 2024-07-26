package dev.langchain4j.store.memory.chat.coherence.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The configuration properties for a {@link dev.langchain4j.store.memory.chat.coherence.CoherenceChatMemoryStore}.
 */
@ConfigurationProperties(prefix = CoherenceChatMemoryStoreProperties.PREFIX)
@Getter
@Setter
public class CoherenceChatMemoryStoreProperties
    {
    /**
     * The property name prefix.
     */
    static final String PREFIX = "langchain4j.coherence.chat";

    /**
     * The name of the Coherence session to use.
     */
    private String session;

    /**
     * The name of the Coherence {@link com.tangosol.net.NamedMap} to use to store chat history.
     */
    private String name;
}
