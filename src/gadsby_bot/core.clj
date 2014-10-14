(ns gadsby-bot.core
  (:require [cheshire.core :as json]
            [clojure.string :as string])
  (:import (com.twitter Extractor)
           (com.twitter.hbc ClientBuilder)
           (com.twitter.hbc.core Constants)
           (com.twitter.hbc.core.endpoint StatusesSampleEndpoint)
           (com.twitter.hbc.core.processor StringDelimitedProcessor)
           (com.twitter.hbc.httpclient.auth OAuth1)
           (java.util.concurrent LinkedBlockingQueue)))

(defn strip-non-alpha [text]
  (clojure.string/replace text #"[^A-Za-z]" ""))

(defn strip-spaces [text]
  (string/replace text #"\s+" ""))

(defn strip-urls [text]
  (let [ex (Extractor.)
        urls (.extractURLsWithIndices ex text)]
    (reduce (fn [t _]
              (let [u (first (.extractURLsWithIndices ex t))]
                (string/join [(subs t 0 (.getStart u))
                              (subs t (.getEnd u))])))
            text urls)))

(def creds {:app "LarY1aNxy9QAFY72ilIV3e6WX"
            :app-secret "nd8UPgRA4mih1V7DYmNJ0Tvh51dNve0Rt7atCxz3Agltx7euoP"
            :client "2829808823-tUecgISqnb1Z3CMS2ah9md2UHNkm2duim0M535E"
            :client-secret "4UqWUFKOr93h23Dwj8N4pUSuqFSDnPLKElLpX9OdDdrCi"})

(defn gadsby? [tweet]
  (let [text (get tweet "text" "")
        original? (and (= false (get tweet "retweeted"))
                       (> 0 (.indexOf text "RT")))
        e-less? (nil? (re-find #"(?i)e" text))
        long-enough-to-care? (<= 50 (-> text
                                        strip-urls
                                        strip-spaces
                                        strip-non-alpha
                                        count))
        english? (= "en" (get tweet "lang"))
        not-reply? (and (nil? (get tweet "in_reply_to_status_id"))
                       (nil? (get tweet "in_reply_to_user_id")))]
    (and original? e-less? long-enough-to-care? english? not-reply?)))

(defn create-client [queue creds]
  (let [endpoint (StatusesSampleEndpoint.)
        auth (OAuth1. (:app creds) (:app-secret creds) (:client creds) (:client-secret creds))
        client (-> (ClientBuilder.)
            (. hosts (. Constants STREAM_HOST))
            (. endpoint endpoint)
            (. authentication auth)
            (. processor (StringDelimitedProcessor. queue))
            (. build))]
    (. client connect)
    client))

(defn create-queue [size]
  (LinkedBlockingQueue. size))

(defn find-a-tweet [queue]
  (loop []
    (let [tweet (json/parse-string (. queue take))]
      (if (get tweet "text")
        (println (get tweet "text")))
      (if (gadsby? tweet)
        tweet
        (recur)))))
