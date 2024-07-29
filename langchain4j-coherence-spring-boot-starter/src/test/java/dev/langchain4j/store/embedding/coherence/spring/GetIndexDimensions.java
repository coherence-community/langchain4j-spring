package dev.langchain4j.store.embedding.coherence.spring;

import com.oracle.coherence.ai.DocumentChunk;
import com.oracle.coherence.ai.hnsw.HnswIndex;
import com.tangosol.io.pof.PofReader;
import com.tangosol.io.pof.PofWriter;
import com.tangosol.io.pof.PortableObject;
import com.tangosol.net.BackingMapContext;
import com.tangosol.util.BinaryEntry;
import com.tangosol.util.InvocableMap;
import com.tangosol.util.MapIndex;
import com.tangosol.util.ValueExtractor;

import java.io.IOException;
import java.util.Map;

/**
 * A Coherence entry processor to use in tests to determine the number of dimensions in
 * the embedding store index.
 */
public class GetIndexDimensions
        implements InvocableMap.EntryProcessor<DocumentChunk.Id, DocumentChunk, Integer>, PortableObject
    {
    @Override
    @SuppressWarnings({"rawtypes"})
    public Integer process(InvocableMap.Entry<DocumentChunk.Id, DocumentChunk> entry)
        {
        BinaryEntry<DocumentChunk.Id, DocumentChunk> binaryEntry = entry.asBinaryEntry();
        BackingMapContext ctx = binaryEntry.getBackingMapContext();
        Map<ValueExtractor, MapIndex> indexMap = ctx.getIndexMap(binaryEntry.getKeyPartition());
        MapIndex mapIndex = indexMap.get(ValueExtractor.of(DocumentChunk::vector));
        if (mapIndex == null) {
            throw new IllegalStateException("No index found");
        }
        if (mapIndex instanceof HnswIndex.HnswMapIndex) {
            HnswIndex.HnswMapIndex vectorIndex = (HnswIndex.HnswMapIndex) mapIndex;
            return vectorIndex.getDimensions();
        }
        throw new IllegalStateException("Index is not a HNSW vector index: " + mapIndex.getClass());
        }

    @Override
    public void readExternal(PofReader pofReader) throws IOException
        {
        }

    @Override
    public void writeExternal(PofWriter pofWriter) throws IOException
        {
        }
    }
