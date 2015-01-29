(ns github-api.core
  (:gen-class)
  (:require [tentacles.repos :as repos]
            [clojure.core.async :as async]
            [org.httpkit.client :as http]
            [cheshire.core :as json])
  (:use [clojure.pprint])
  (:import (java.time Period LocalDateTime)
           (java.time.format DateTimeFormatter)))

(defn save-starred [token]
  (let [starred (repos/starring "clojj"
                                {:accept    "application/vnd.github.v3.star+json" :auth token
                                 ;:per-page 15})
                                 :all-pages true})
        filename (str "/Users/jwin/starred-" (.format (LocalDateTime/now) (DateTimeFormatter/ofPattern "yyyyMMdd")) ".txt")]

    (doseq [repo-url (map #(get-in % [:repo :git_url]) starred)]
      (spit (clojure.java.io/file filename) (str repo-url "\n") :append true))))

(defn latest [days token]
  (let [starred (repos/starring "clojj"
                                {:accept    "application/vnd.github.v3.star+json" :auth token
                                 ;:per-page 15})
                                 :all-pages true})

        users-and-names (map (fn [s] [(get-in s [:repo :owner :login]) (get-in s [:repo :name])]) starred)

        results (pmap (fn [[user repo]]
                        [repo (count (repos/commits user repo {:accept    "application/vnd.github.v3.star+json" :auth token
                                                               :all-pages true :since (-> (LocalDateTime/now) (.minus (Period/ofDays days)) (.toString))}))])
                      users-and-names)]

    (doseq [repo-url (map #(get-in % [:repo :git_url]) starred)]
      (spit (clojure.java.io/file "/Users/jwin/starred-github-repos.txt") repo-url))

    ;(pprint users-and-names)
    (pprint (sort (fn [[_ count1] [_ count2]] (> count1 count2)) (filter (fn [[_ count]] (> count 0)) results)))))


;(http/get "https://api.github.com/repos/clojj/watchservice/commits" #(pprint (json/parse-string (get-in % [:body]))))

(defn callback [url response result-ch]
  (async/>!! result-ch [url response]))

(defn go-all [token since]
  (let [starred (repos/starring "clojj"
                                {:accept     "application/vnd.github.v3.star+json"
                                 :auth       token
                                 ;:all-pages  true
                                 :per-page   30
                                 :user-agent "clojj"})
        repos (map (fn [s] [(get-in s [:repo :owner :login]) (get-in s [:repo :name])]) starred)
        urls-1 (vec (for [[user repo] repos] (format "https://api.github.com/repos/%s/%s/commits?per_page=50&since=%s&page=1" user repo since)))
        page-ch (async/to-chan urls-1)
        result-ch (async/chan)
        options {:timeout    1000
                 :user-agent "clojj"
                 :headers    {"Authorization" (str "token " token)
                              "Accept"        "application/vnd.github.v3.star+json"}}]

    (async/go-loop []
      (when-let [[url response] (async/<! result-ch)]
        (let [json (json/parse-string (get-in response [:body]))]
          (println url ": " (count json))
          (recur))))

    (async/go-loop []
      (when-let [next-page (async/<! page-ch)]
        (println next-page)
        (http/get next-page options #(callback next-page % result-ch))
        (recur)))

    (println "Ready")))

#_(defn async-commits-since [user repo per-page since]
  (let [url (format "%s/repos/%s/%s/commits?per_page=%d&since=%s" base-url user repo per-page since)]

    (let [page-chan (async/chan)
          res (atom [])]

      (async/go-loop []

        (when-let [next-page (async/<! page-chan)]
          (async-get (str url "&page=" next-page) (handle-page page-chan)))
        (recur))

      ;; gather results
      ;(swap! res conj (async/<!! c))

      res)))

;https://api.github.com/repos/tinkerpop/tinkerpop3/commits?per_page=100&page=2&since=2015-⁠01-01T00:00Z