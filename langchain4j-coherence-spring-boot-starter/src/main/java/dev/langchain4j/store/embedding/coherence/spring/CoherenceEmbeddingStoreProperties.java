package dev.langchain4j.store.embedding.coherence.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The configuration properties for a {@link dev.langchain4j.store.embedding.coherence.CoherenceEmbeddingStore}.
 */
@ConfigurationProperties(prefix = CoherenceEmbeddingStoreProperties.PREFIX)
@Getter
@Setter
public class CoherenceEmbeddingStoreProperties
    {
    /**
     * The property name prefix.
     */
    static final String PREFIX = "langchain4j.coherence.embedding";

    /**
     * The name of the Coherence session to use.
     */
    private String session;

    /**
     * The name of the Coherence {@link com.tangosol.net.NamedMap} to use to store embeddings.
     */
    private String name;

    /**
     * The index name to use.
     */
    private String index;

    /**
     * Force normalization of embeddings on add and search.
     */
    private boolean forceNormalize;

    /**
     * The number of dimensions in the embeddings.
     * <p>
     * If the embedding store is created with an embedding model, the model's dimensions
     * will take precedence over this property.
     */
    private Integer dimension;
}
