# scc-mcp

Standalone [MCP](https://modelcontextprotocol.io/) server for [scc](https://github.com/boyter/scc) code metrics. Runs on [Babashka](https://babashka.org/) via the [modex-bb](https://github.com/hive-agi/modex-bb) framework.

Provides 4 tools for code metrics: project analysis, complexity hotspots, per-file metrics, and directory comparison.

## Requirements

- [Babashka](https://github.com/babashka/babashka) v1.3.0+
- [scc](https://github.com/boyter/scc) on PATH

## Quick Start

```bash
bb --config bb.edn run server
```

### Claude Code MCP config

Add to `~/.claude/settings.json`:

```json
{
  "mcpServers": {
    "scc": {
      "command": "bb",
      "args": ["--config", "/path/to/scc-mcp/bb.edn", "run", "server"]
    }
  }
}
```

## Tools

| Tool | Description |
|------|-------------|
| `analyze` | Project metrics — LOC, complexity, language breakdown |
| `hotspots` | Complexity hotspots — files exceeding a threshold (default: 20) |
| `file` | Detailed metrics for a single file |
| `compare` | Compare metrics between two directories (diff LOC, code, complexity) |

## IAddon Integration

scc-mcp implements the `IAddon` protocol for dynamic registration in [hive-mcp](https://github.com/hive-agi/hive-mcp). When loaded as an addon, its handlers override the embedded scc handlers in the consolidated `analysis` tool.

```clojure
(require '[scc-mcp.init :as init])
(init/init-as-addon!)
;; => {:registered ["scc"] :total 1}
```

The addon exposes a single `scc` supertool with command dispatch:

```clojure
(require '[scc-mcp.tools :as tools])
(tools/handle-scc {:command "analyze" :path "src/"})
```

## Project Structure

```
src/scc_mcp/
  core.clj    — Shell wrapper for scc binary, JSON parsing, hotspot extraction
  tools.clj   — Command handlers + MCP tool schema (IAddon interface)
  init.clj    — IAddon reify + nil-railway registration pipeline
  server.clj  — modex-bb standalone MCP server (4 tools)
  log.clj     — Logging shim (timbre on JVM, stderr on bb)
```

## Dependencies

- [modex-bb](https://github.com/hive-agi/modex-bb) — MCP server framework
- [scc](https://github.com/boyter/scc) — external binary (not bundled)

## License

MIT
