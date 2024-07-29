package dev.langchain4j.store.embedding.coherence.spring;

import com.oracle.bedrock.junit.CoherenceClusterExtension;
import com.oracle.bedrock.runtime.coherence.CoherenceClusterMember;
import com.oracle.bedrock.runtime.coherence.options.ClusterName;
import com.oracle.bedrock.runtime.coherence.options.LocalHost;
import com.oracle.bedrock.runtime.coherence.options.RoleName;
import com.oracle.bedrock.runtime.coherence.options.WellKnownAddress;
import com.oracle.bedrock.runtime.java.options.IPv4Preferred;
import com.oracle.bedrock.runtime.java.options.SystemProperty;
import com.oracle.bedrock.runtime.options.DisplayName;
import com.oracle.bedrock.testsupport.junit.TestLogsExtension;
import com.oracle.coherence.ai.DocumentChunk;
import com.tangosol.net.Coherence;
import com.tangosol.net.NamedMap;
import com.tangosol.net.Session;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.coherence.CoherenceEmbeddingStore;
import dev.langchain4j.store.embedding.spring.EmbeddingStoreAutoConfigurationIT;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;


class CoherenceEmbeddingStoreAutoConfigurationIT extends EmbeddingStoreAutoConfigurationIT {

    public static final String CLUSTER_NAME = "EmbeddingStoreTest";

    @RegisterExtension
    static TestLogsExtension testLogs = new TestLogsExtension();

    @RegisterExtension
    static CoherenceClusterExtension cluster = new CoherenceClusterExtension()
            .with(ClusterName.of(CLUSTER_NAME),
                    WellKnownAddress.loopback(),
                    LocalHost.only(),
                    IPv4Preferred.autoDetect(),
                    SystemProperty.of("coherence.pof.config", "langchain-coherence-test-pof-config.xml"),
                    SystemProperty.of("coherence.serializer", "pof"))
            .include(3, CoherenceClusterMember.class,
                    DisplayName.of("storage"),
                    RoleName.of("storage"),
                    testLogs);

    ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(autoConfigurationClass()));

    @BeforeAll
    static void beforeAll() {
        System.setProperty("coherence.cluster", CLUSTER_NAME);
        System.setProperty("coherence.wka", "127.0.0.1");
        System.setProperty("coherence.localhost", "127.0.0.1");
        System.setProperty("coherence.serializer", "pof");
        System.setProperty("coherence.distributed.localstorage", "false");
        System.setProperty("coherence.pof.config", "langchain-coherence-test-pof-config.xml");

    }

    @BeforeEach
    void clearEmbeddings() {
        Optional<CoherenceClusterMember> optional = cluster.getCluster().findAny();
        assertThat(optional.isPresent(), is(true));
        CoherenceClusterMember clusterMember = optional.get();
        clusterMember.invoke(() -> {
                Session session = Coherence.getInstance().getSession();
                session.getMap(CoherenceEmbeddingStore.DEFAULT_MAP_NAME).truncate();
                return null;
            });
    }

    @Test
    public void should_use_default_cache_name() {
        contextRunner
            .withPropertyValues(properties())
            .run(context -> {
                CoherenceEmbeddingStoreProperties properties = new CoherenceEmbeddingStoreProperties();
                CoherenceEmbeddingStoreAutoConfiguration configuration = new CoherenceEmbeddingStoreAutoConfiguration();
                CoherenceEmbeddingStore store = configuration.coherenceEmbeddingStore(properties, null);
                NamedMap<DocumentChunk.Id, DocumentChunk> documentChunks = store.getDocumentChunks();
                assertThat(documentChunks.getName(), is(CoherenceEmbeddingStore.DEFAULT_MAP_NAME));
            });
    }

    @Test
    public void should_use_configured_cache_name() {
        contextRunner
            .withPropertyValues(properties())
            .run(context -> {
                String cacheName = "test-embeddings";
                CoherenceEmbeddingStoreProperties properties = new CoherenceEmbeddingStoreProperties();
                properties.setName(cacheName);

                CoherenceEmbeddingStoreAutoConfiguration configuration = new CoherenceEmbeddingStoreAutoConfiguration();
                CoherenceEmbeddingStore store = configuration.coherenceEmbeddingStore(properties, null);
                NamedMap<DocumentChunk.Id, DocumentChunk> documentChunks = store.getDocumentChunks();
                assertThat(documentChunks.getName(), is(cacheName));
            });
    }

    @Test
    public void should_not_force_normalization_by_default() {
        contextRunner
            .withPropertyValues(properties())
            .run(context -> {
                CoherenceEmbeddingStoreProperties properties = new CoherenceEmbeddingStoreProperties();
                CoherenceEmbeddingStoreAutoConfiguration configuration = new CoherenceEmbeddingStoreAutoConfiguration();
                CoherenceEmbeddingStore store = configuration.coherenceEmbeddingStore(properties, null);
                assertThat(store.isForceNormalize(), is(false));
            });
    }

    @Test
    public void should_force_normalization() {
        contextRunner
            .withPropertyValues(properties())
            .run(context -> {
                CoherenceEmbeddingStoreProperties properties = new CoherenceEmbeddingStoreProperties();
                properties.setForceNormalize(true);

                CoherenceEmbeddingStoreAutoConfiguration configuration = new CoherenceEmbeddingStoreAutoConfiguration();
                CoherenceEmbeddingStore store = configuration.coherenceEmbeddingStore(properties, null);
                assertThat(store.isForceNormalize(), is(true));
            });
    }

    @Test
    public void should_add_index_with_model_dimensions() {
        EmbeddingModel embeddingModel = new AllMiniLmL6V2QuantizedEmbeddingModel();

        contextRunner
            .withPropertyValues(properties())
            .run(context -> {
                CoherenceEmbeddingStoreProperties properties = new CoherenceEmbeddingStoreProperties();
                properties.setIndex("hnsw");
                properties.setDimension(1234);

                CoherenceEmbeddingStoreAutoConfiguration configuration = new CoherenceEmbeddingStoreAutoConfiguration();
                CoherenceEmbeddingStore store = configuration.coherenceEmbeddingStore(properties, embeddingModel);
                NamedMap<DocumentChunk.Id, DocumentChunk> documentChunks = store.getDocumentChunks();

                DocumentChunk.Id id = new DocumentChunk.Id("foo", 0);
                int demensions = documentChunks.invoke(id, new GetIndexDimensions());
                assertThat(demensions, is(embeddingModel.dimension()));
            });
    }

    @Test
    public void should_add_index_with_configured_dimensions() {
        contextRunner
            .withPropertyValues(properties())
            .run(context -> {
                CoherenceEmbeddingStoreProperties properties = new CoherenceEmbeddingStoreProperties();
                properties.setIndex("hnsw");
                properties.setDimension(1234);

                CoherenceEmbeddingStoreAutoConfiguration configuration = new CoherenceEmbeddingStoreAutoConfiguration();
                CoherenceEmbeddingStore store = configuration.coherenceEmbeddingStore(properties, null);
                NamedMap<DocumentChunk.Id, DocumentChunk> documentChunks = store.getDocumentChunks();

                DocumentChunk.Id id = new DocumentChunk.Id("foo", 0);
                int demensions = documentChunks.invoke(id, new GetIndexDimensions());
                assertThat(demensions, is(1234));
            });
    }


    @Override
    protected Class<?> autoConfigurationClass() {
        return CoherenceEmbeddingStoreAutoConfiguration.class;
    }

    @Override
    protected Class<? extends EmbeddingStore<TextSegment>> embeddingStoreClass() {
        return CoherenceEmbeddingStore.class;
    }

    @Override
    protected String[] properties() {
        return new String[]{};
    }

    @Override
    protected String dimensionPropertyKey() {
        return "langchain4j.coherence.dimension";
    }
}
