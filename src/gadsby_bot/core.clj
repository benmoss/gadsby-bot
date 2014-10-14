(ns gadsby-bot.core
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [twitter.api.restful :as twitter]
            [twitter.oauth :as tw-oauth])
  (:import (com.twitter Extractor)
           (com.twitter.hbc ClientBuilder)
           (com.twitter.hbc.core Constants)
           (com.twitter.hbc.core.endpoint StatusesSampleEndpoint)
           (com.twitter.hbc.core.processor StringDelimitedProcessor)
           (com.twitter.hbc.httpclient.auth OAuth1)
           (java.util.concurrent LinkedBlockingQueue))
  (:gen-class))

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

(defn gadsby? [tweet]
  (let [text (get tweet "text" "")
        conditions {:original?  #(and (= false (get tweet "retweeted"))
                                      (> 0 (.indexOf text "RT")))
                    :e-less? #(nil? (re-find #"(?i)e" text))
                    :long-enough-to-care? #(<= 60 (-> text
                                                     strip-urls
                                                     strip-spaces
                                                     strip-non-alpha
                                                     count))
                    :english? #(= "en" (get tweet "lang"))
                    :not-reply? #(and (nil? (get tweet "in_reply_to_status_id"))
                                     (nil? (get tweet "in_reply_to_user_id")))
                    :not-simple-repetition? #(<= 6 (-> text strip-urls strip-non-alpha set count))}]
    (every? (fn [[_ f]] (f)) conditions)))

(defn create-client [queue creds]
  (let [endpoint (StatusesSampleEndpoint.)
        auth (OAuth1. (:consumer creds) (:consumer-secret creds)
                      (:client creds) (:client-secret creds))
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

(def oauth-creds (tw-oauth/make-oauth-creds (:consumer creds) (:consumer-secret creds)
                                            (:client creds) (:client-secret creds)))

(defn fav [tweet creds]
  (twitter/favorites-create :oauth-creds creds :params {:id (get tweet "id")}))

(defn find-a-tweet [queue]
  (loop []
    (let [tweet (json/parse-string (. queue take))]
      (if (gadsby? tweet)
        tweet
        (recur)))))

(defn -main [& args]
  (let [q (create-queue 1000)
        c (create-client q creds)]
    (try (loop []
           (-> (find-a-tweet q)
               (fav oauth-creds)
               (get-in [:body :text])
               println)
           (recur))
         (finally (.stop c)))))
