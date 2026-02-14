(ns scc-mcp.tools
  "MCP tool handlers for scc code metrics.

   Exposes handle-scc for hive-mcp IAddon integration
   and tool-def for MCP schema registration."
  (:require [scc-mcp.core :as core]
            [scc-mcp.log :as log]))

;; =============================================================================
;; Command Handlers
;; =============================================================================

(def ^:private command-handlers
  {"analyze"  (fn [{:keys [path]}]
                (let [result (core/analyze-project path)]
                  (if (:error result)
                    {:error (:error result)}
                    {:summary (:summary result)
                     :by-language (:by-language result)
                     :file-count (count (:files result))
                     :path path})))

   "hotspots" (fn [{:keys [path threshold]}]
                (let [threshold-val (or threshold 20)
                      result (core/run-scc path)]
                  (if (:error result)
                    {:error (:error result)}
                    (let [hotspots (core/get-complexity-hotspots (:raw result) threshold-val)]
                      {:hotspots hotspots
                       :count (count hotspots)
                       :threshold threshold-val
                       :path path}))))

   "file"     (fn [{:keys [file_path]}]
                (core/get-file-metrics file_path))

   "compare"  (fn [{:keys [path_a path_b]}]
                (let [result-a (core/analyze-project path_a)
                      result-b (core/analyze-project path_b)]
                  (cond
                    (:error result-a)
                    {:error (str "Error analyzing " path_a ": " (:error result-a))}

                    (:error result-b)
                    {:error (str "Error analyzing " path_b ": " (:error result-b))}

                    :else
                    (let [summary-a (:summary result-a)
                          summary-b (:summary result-b)
                          diff (fn [k] (- (get summary-b k 0) (get summary-a k 0)))]
                      {:path_a {:path path_a :summary summary-a}
                       :path_b {:path path_b :summary summary-b}
                       :diff {:lines (diff :total-lines)
                              :code (diff :total-code)
                              :complexity (diff :total-complexity)
                              :files (diff :file-count)}}))))})

;; =============================================================================
;; MCP Interface (IAddon integration)
;; =============================================================================

(defn handle-scc
  "MCP tool handler for scc commands. Dispatches on :command key.
   Returns MCP-compatible response map with :content vector."
  [{:keys [command] :as params}]
  (if-let [handler (get command-handlers command)]
    (try
      (let [result (handler params)]
        (if (:error result)
          {:content [{:type "text" :text (pr-str result)}]
           :isError true}
          {:content [{:type "text" :text (pr-str result)}]}))
      (catch Exception e
        (log/error "scc command failed:" command (ex-message e))
        {:content [{:type "text" :text (pr-str {:error   "Failed to handle command"
                                                :command command
                                                :details (ex-message e)})}]
         :isError true}))
    {:content [{:type "text" :text (pr-str {:error     "Unknown command"
                                            :command   command
                                            :available (sort (keys command-handlers))})}]
     :isError true}))

(defn tool-def
  "MCP tool definition for the scc tool."
  []
  {:name        "scc"
   :description "scc code metrics: analyze, hotspots, file, compare"
   :inputSchema {:type       "object"
                 :properties {:command   {:type "string"
                                          :enum (sort (keys command-handlers))}
                              :path      {:type        "string"
                                          :description "Path to file or directory to analyze"}
                              :file_path {:type        "string"
                                          :description "Path to specific file (for file command)"}
                              :path_a    {:type        "string"
                                          :description "First directory for comparison"}
                              :path_b    {:type        "string"
                                          :description "Second directory for comparison"}
                              :threshold {:type        "number"
                                          :description "Minimum complexity for hotspots (default: 20)"}}
                 :required   ["command"]}})

(defn invalidate-cache!
  "Placeholder for cache invalidation (scc doesn't cache, but IAddon expects it)."
  []
  nil)
