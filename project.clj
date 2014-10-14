(defproject gadsby-bot "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.twitter/hbc-core "2.2.0" :exclusions [[org.apache.httpcomponents/httpclient]
                                                            [org.apache.httpcomponents/httpcore]
                                                            [commons-codec]]]
                 [com.twitter/twitter-text "1.6.1"]
                 [twitter-api "0.7.7" ]
                 [cheshire "5.3.1"]]
  :main gadsby-bot.core
  :aot [gadsby-bot.core])
