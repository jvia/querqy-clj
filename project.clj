(defproject com.nytimes/querqy-clj "0.7.1-SNAPSHOT"
  :description "Querqy in Clojure"
  :url "https://github.com/nytimes/querqy-clj"
  :license {:name "Apache License",
            :url  "http://www.apache.org/licenses/LICENSE-2.0"}

  :repositories [["releases"  {:url           "https://clojars.org/repo"
                               :sign-releases false
                               :username      [:env/clojars_username]
                               :password      [:env/clojars_password]}]
                 ["snapshots" {:url           "https://clojars.org/repo"
                               :sign-releases false
                               :username      [:env/clojars_username]
                               :password      [:env/clojars_password]}]]

  :plugins [[com.github.clj-kondo/lein-clj-kondo "0.2.1"]
            [lein-cljfmt "0.8.0"]]

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.querqy/querqy-core "3.12.0"]]

  :repl-options {:init-ns com.nytimes.querqy}
  :global-vars {*warn-on-reflection* true}

  :test-selectors {:default     (complement :integration)
                   :unit        (complement :integration)
                   :integration :integration}

  :profiles {:dev {:source-paths ["dev"]
                   :jvm-opts     ["-Dorg.slf4j.simpleLogger.defaultLogLevel=debug"]
                   :dependencies [[metosin/testit "0.4.1"]
                                  [lambdaisland/kaocha "1.66.1034"]
                                  [org.clojure/test.check "1.1.1"]
                                  [criterium "0.4.6"]
                                  [org.slf4j/slf4j-api "1.7.36"]
                                  [org.slf4j/slf4j-simple "1.7.36"]]}}

  :aliases {"kaocha" ["run" "-m" "kaocha.runner"]}

  :cljfmt {:indents {facts [[:block 1]]
                     fact  [[:block 1]]}}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])
