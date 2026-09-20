# Milvus collection: `ai_knowledge_chunk`

The provider uses Milvus REST v2 (`MILVUS_ENDPOINT`) and expects a collection
with these fields. `embedding` is the collection vector field and must use
dimension `1536`, matching `SMS_AI_EMBEDDING_DIMENSIONS` and the selected model.

| field | type | notes |
| --- | --- | --- |
| id | Int64 | primary key |
| knowledgeBaseId | Int64 | scalar filter |
| documentId | Int64 | scalar filter |
| documentNo | VarChar | source identifier |
| documentVersionId | Int64 | source version |
| indexRevision | VarChar | active index revision |
| chunkNo | Int32 | chunk sequence |
| content | VarChar | retrieved text |
| metadata | JSON | source metadata |
| embedding | FloatVector(1536) | semantic vector; dimension must be 1536 |
| tenantId | VarChar | mandatory tenant equality filter |
| status | VarChar | `ACTIVE` records are searchable |
