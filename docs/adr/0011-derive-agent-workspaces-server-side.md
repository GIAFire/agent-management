# Derive agent workspaces server-side

Agent workspace roots are controlled exclusively by server configuration, and each runtime workspace is derived by tenant, user, and agent identity; clients cannot submit or override server filesystem paths. The legacy `ai_agent_config.workspace_path` column is removed, trading per-agent path flexibility for tenant isolation and protection from arbitrary filesystem access.
