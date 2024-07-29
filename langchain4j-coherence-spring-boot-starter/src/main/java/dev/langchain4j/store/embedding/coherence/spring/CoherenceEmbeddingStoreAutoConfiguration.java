package dev.langchain4j.store.embedding.coherence.spring;

import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.ai.VectorIndexExtractor;
import com.oracle.coherence.ai.hnsw.HnswIndex;
import com.oracle.coherence.common.base.Logger;
import com.oracle.coherence.spring.boot.autoconfigure.CoherenceAutoConfiguration;
import com.oracle.coherence.spring.configuration.annotation.EnableCoherence;
import com.tangosol.net.Coherence;
import com.tangosol.util.ValueExtractor;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.coherence.CoherenceEmbeddingStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

import java.util.Optional;

import static dev.langchain4j.store.embedding.coherence.spring.CoherenceEmbeddingStoreProperties.PREFIX;

/**
 * The automatic configuration for a Coherence embedding store.
 */
@AutoConfiguration
@EnableCoherence
@AutoConfigureAfter(CoherenceAutoConfiguration.class)
@EnableConfigurationProperties(CoherenceEmbeddingStoreProperties.class)
@ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class CoherenceEmbeddingStoreAutoConfiguration
    {
    /**
     * Create a {@link CoherenceEmbeddingStore}.
     *
     * @param properties      the Spring properties
     * @param embeddingModel  the embedding model to be used
     *
     * @return a new instance of a {@link CoherenceEmbeddingStore}
     */
    @Bean
    @ConditionalOnMissingBean
    @SuppressWarnings({"rawtypes", "unchecked"})
    public CoherenceEmbeddingStore coherenceEmbeddingStore(CoherenceEmbeddingStoreProperties properties,
                                                   @Nullable EmbeddingModel embeddingModel) {

        String session = Optional.ofNullable(properties.getSession()).orElse(Coherence.DEFAULT_NAME);
        String cache = Optional.ofNullable(properties.getName()).orElse(CoherenceEmbeddingStore.DEFAULT_MAP_NAME);
        String index = properties.getIndex();
        boolean force = properties.isForceNormalize();
        Integer dimension = embeddingModel == null ? properties.getDimension() : (Integer) embeddingModel.dimension();

        VectorIndexExtractor extractor = null;

        if (index != null && !index.isEmpty()) {
            if ("hnsw".equalsIgnoreCase(index)) {
                if (dimension != null) {
                    extractor = new HnswIndex<>(ValueExtractor.of(DocumentChunk::vector), dimension);
                } else {
                    Logger.warn("Cannot create embedding store index - HNSW has been specified as the index name for the Coherence embedding store, but the dimensions property has not been set");
                }
            }
        }

        return CoherenceEmbeddingStore.builder()
                .session(session)
                .name(cache)
                .index(extractor)
                .forceNormalize(force)
                .build();
    }
}
