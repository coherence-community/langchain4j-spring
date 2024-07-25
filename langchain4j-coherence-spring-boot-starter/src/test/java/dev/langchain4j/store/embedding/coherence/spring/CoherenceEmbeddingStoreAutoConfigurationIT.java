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
import com.tangosol.net.Coherence;
import com.tangosol.net.Session;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.coherence.CoherenceEmbeddingStore;
import dev.langchain4j.store.embedding.spring.EmbeddingStoreAutoConfigurationIT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.concurrent.atomic.AtomicInteger;


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
                    SystemProperty.of("coherence.serializer", "pof"))
            .include(3, CoherenceClusterMember.class,
                    DisplayName.of("storage"),
                    RoleName.of("storage"),
                    testLogs);

    @BeforeEach
    void clearEmbeddings() {
    CoherenceClusterMember clusterMember = cluster.getCluster().findAny().get();
    clusterMember.invoke(() -> {
            Session session = Coherence.getInstance().getSession();
            session.getMap(CoherenceEmbeddingStore.DEFAULT_MAP_NAME).truncate();
            return null;
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
        return new String[]{
                "coherence.cluster=" + CLUSTER_NAME,
                "coherence.wka=127.0.0.1",
                "coherence.localhost=127.0.0.1",
                "coherence.serializer=pof",
                "coherence.distributed.localstorage=false"
        };
    }

    @Override
    protected String dimensionPropertyKey() {
        return "langchain4j.coherence.dimension";
    }
}
