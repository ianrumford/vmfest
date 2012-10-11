(defproject vmfest "0.2.5-vbox4.2.0-SNAPSHOT"
  :description "Manage local VMs from the REPL"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [slingshot "0.10.2"]
                 [org.clojure/tools.logging "0.2.3"]
                 [local/vboxjws "4.2.0"]
                 ;;[org.clojars.tbatchelli/vboxjws "4.1.8"]
                 [fs "1.0.0"]]
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :multi-deps {"1.4" [[org.clojure/clojure "1.4.0"]]
               ;; "1.4" [[org.clojure/clojure "1.4.0-beta1"]]
               :all [[slingshot "0.10.0"]
                     [org.clojure/tools.logging "0.2.3"]
                     ;;[org.clojars.tbatchelli/vboxjws "4.1.8"]
                     [local/vboxjws "4.2.0"]
                     [fs "1.0.0"]]}
  :dev-dependencies [[robert/hooke "1.1.2"]
                     [log4j/log4j "1.2.16"]
                     [lein-clojars "0.8.0"]]
  :repositories [["project" "file:repo"]]
  :aot [#"vmfest.virtualbox.model"]
  :test-selectors {:default (fn [v] (not (:integration v)))
                   :integration :integration
                   :all (fn [_] true)}
  :jar-exclusions [#"log4j.xml"])
