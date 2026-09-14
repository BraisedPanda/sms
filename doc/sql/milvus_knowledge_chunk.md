# Milvus collection: `ai_knowledge_chunk`

The provider uses Milvus REST v2 (`SMS_MILVUS_ENDPOINT`) and expects a collection
with these fields. `embedding` is the collection vector field; its dimension
must match the configured embedding model.

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
| embedding | FloatVector | semantic vector |
| status | VarChar | `ACTIVE` records are searchable |
