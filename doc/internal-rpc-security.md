# Internal RPC Security Boundary

The Web, AI, Knowledge and System services must run on a private service network. Only `sms-web` exposes an HTTP port to ingress.

## Required Network Policy

| Destination | Allowed callers | Exposure |
| --- | --- | --- |
| Web `9090` | ingress/load balancer | public application entry |
| AI Dubbo port | Web only | private network |
| Knowledge Dubbo `20881` | AI and approved admin services only | private network |
| System Dubbo `20882` | Web only | private network |
| Nacos `8848/9848` | application service accounts only | private management network |
| MySQL, PostgreSQL, Redis, Milvus | owning providers only | private data network |

Enforce this matrix with Kubernetes `NetworkPolicy`, security groups, or host firewall rules. Do not bind Dubbo, Nacos, database, Redis or Milvus ports to a public interface.

## Service Authentication

Set one 32-character-or-longer `INTERNAL_RPC_SECRET` through the deployment secret manager. Web signs the tenant/user/session/request context for AI calls; AI verifies it and signs a new context for Knowledge calls. Providers reject missing, expired, modified, or unexpected-caller contexts.

The HMAC context authenticates the application caller. Production transport should additionally use Dubbo TLS/mTLS so payloads and service credentials are protected in transit. Rotate the HMAC secret with a rolling restart and rotate mTLS certificates independently.

## Audit

Accepted AI and Knowledge RPC calls emit structured `rpc_audit` records with action, caller service, tenant, user and request ID. Central logging must restrict access and retention; tokens, signatures, prompts and raw tool results must not be indexed.
