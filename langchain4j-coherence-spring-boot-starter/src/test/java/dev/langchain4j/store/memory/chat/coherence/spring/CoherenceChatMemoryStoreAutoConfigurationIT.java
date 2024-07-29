package dev.langchain4j.store.memory.chat.coherence.spring;

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
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.coherence.CoherenceChatMemoryStore;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CoherenceChatMemoryStoreAutoConfigurationIT {

    public static final String CLUSTER_NAME = "ChatMemoryStoreTest";

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
        .withConfiguration(AutoConfigurations.of(CoherenceChatMemoryStoreAutoConfiguration.class));

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
    void clearChats() {
        Optional<CoherenceClusterMember> optional = cluster.getCluster().findAny();
        assertThat(optional.isPresent()).isEqualTo(true);
        CoherenceClusterMember clusterMember = optional.get();
        clusterMember.invoke(() -> {
                Session session = Coherence.getInstance().getSession();
                session.getMap(CoherenceChatMemoryStore.DEFAULT_MAP_NAME).truncate();
                return null;
            });
    }

    protected Class<? extends ChatMemoryStore> chatMemoryStoreClass() {
        return CoherenceChatMemoryStore.class;
    }

    @Test
    public void should_provide_chat_memory_store() {

    contextRunner
            .run(context -> {
                assertThat(context.getBean(chatMemoryStoreClass())).isExactlyInstanceOf(chatMemoryStoreClass());
                ChatMemoryStore store = context.getBean(chatMemoryStoreClass());

                String id = "test-one";
                List<ChatMessage> messages = store.getMessages(id);
                assertThat(messages).isNotNull();
                assertThat(messages).hasSize(0);

                List<ChatMessage> updated = store.getMessages(id);
                updated.add(new UserMessage("message one"));
                updated.add(new UserMessage("message two"));
                updated.add(new UserMessage("message three"));

                store.updateMessages(id, updated);

                messages = store.getMessages(id);
                assertThat(messages).isNotNull();
                assertThat(messages).hasSize(3);
                assertThat(messages).isEqualTo(updated);

                updated.add(new UserMessage("message four"));
                store.updateMessages(id, updated);

                messages = store.getMessages(id);
                assertThat(messages).isNotNull();
                assertThat(messages).hasSize(4);
                assertThat(messages).isEqualTo(updated);

                store.deleteMessages(id);
                messages = store.getMessages(id);
                assertThat(messages).isNotNull();
                assertThat(messages).hasSize(0);
            });

    }
}
