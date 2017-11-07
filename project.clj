(defproject us.edwardstx.service/sac "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0-beta4"]
                 [org.clojure/tools.nrepl "0.2.12"]
                 [cheshire "5.8.0"]
                 [crypto-random "1.2.0"]
                 [yogthos/config "0.9"]
                 [us.edwardstx/edwardstx_common "1.0.2-SNAPSHOT"]]

  :uberjar-name "service-account-sercive.jar"

  :main us.edwardstx.service.sac

  :profiles {:dev {:repl-options {:init-ns us.edwardstx.service.sac}

                   :dependencies [[binaryage/devtools "0.9.4"]
                                  [org.clojure/tools.nrepl "0.2.13"]]

                   :resource-paths ["env/dev/resources" "resources"]

                   :env {:dev true}}

             :uberjar {:env {:production true}
                       :aot :all
                       :omit-source true}}

  )
