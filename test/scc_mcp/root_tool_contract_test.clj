(ns scc-mcp.root-tool-contract-test
  (:require [hive-addon.tool-contract.test :refer [deftest-root-tools]]
            [hive-addon.protocol :as proto]
            [scc-mcp.init :as a]))

(deftest-root-tools root-tools
  (proto/tools (a/addon-ctor {}))
  :allow-empty? true)
