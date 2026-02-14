(ns scc-mcp.server
  "Standalone babashka MCP server for scc code metrics.

   Uses modex-bb framework for stdio JSON-RPC.
   Start: bb --config bb.edn run server"
  (:require [modex-bb.mcp.server :as mcp-server]
            [modex-bb.mcp.tools :refer [tools]]
            [scc-mcp.core :as core]
            [scc-mcp.log :as log]))

;; =============================================================================
;; Tool Definitions (modex-bb DSL)
;; =============================================================================

(def scc-tools
  (tools
   (analyze "Analyze project/directory for code metrics — LOC, complexity, language breakdown"
            [{:keys [path]
              :type {:path :string}
              :doc  {:path "Path to file or directory to analyze"}}]
            (let [result (core/analyze-project path)]
              (if (:error result)
                [{:error (:error result)}]
                [{:summary (:summary result)
                  :by-language (:by-language result)
                  :file-count (count (:files result))
                  :path path}])))

   (hotspots "Get complexity hotspots — files exceeding a complexity threshold"
             [{:keys [path threshold]
               :type {:path :string :threshold :number}
               :doc  {:path "Path to directory to analyze"
                      :threshold "Minimum complexity to include (default: 20)"}
               :or   {threshold 20}}]
             (let [result (core/run-scc path)]
               (if (:error result)
                 [{:error (:error result)}]
                 (let [hotspots (core/get-complexity-hotspots (:raw result) threshold)]
                   [{:hotspots hotspots
                     :count (count hotspots)
                     :threshold threshold
                     :path path}]))))

   (file "Get detailed metrics for a specific file"
         [{:keys [file_path]
           :type {:file_path :string}
           :doc  {:file_path "Path to the file to analyze"}}]
         (let [result (core/get-file-metrics file_path)]
           (if (:error result)
             [{:error (:error result)}]
             [result])))

   (compare "Compare metrics between two directories"
            [{:keys [path_a path_b]
              :type {:path_a :string :path_b :string}
              :doc  {:path_a "Path to first directory"
                     :path_b "Path to second directory"}}]
            (let [result-a (core/analyze-project path_a)
                  result-b (core/analyze-project path_b)]
              (cond
                (:error result-a)
                [{:error (str "Error analyzing " path_a ": " (:error result-a))}]

                (:error result-b)
                [{:error (str "Error analyzing " path_b ": " (:error result-b))}]

                :else
                (let [summary-a (:summary result-a)
                      summary-b (:summary result-b)
                      diff (fn [k] (- (get summary-b k 0) (get summary-a k 0)))]
                  [{:path_a {:path path_a :summary summary-a}
                    :path_b {:path path_b :summary summary-b}
                    :diff {:lines (diff :total-lines)
                           :code (diff :total-code)
                           :complexity (diff :total-complexity)
                           :files (diff :file-count)}}]))))))

;; =============================================================================
;; Server
;; =============================================================================

(def mcp-server
  (mcp-server/->server
   {:name    "scc-mcp"
    :version "0.1.0"
    :tools   scc-tools}))

(defn -main
  "Start the scc MCP server.
   Reads JSON-RPC from stdin, writes responses to stdout."
  [& _args]
  (log/info "Starting scc-mcp server v0.1.0")
  (mcp-server/start-server! mcp-server)
  (Thread/sleep 500))
