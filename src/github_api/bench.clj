(ns github-api.bench
  (:require
    [clojure.pprint :refer [pprint]]
    [com.climate.claypoole :as cp]
    [tentacles.repos :as repos])
  (:import (java.time LocalDateTime Period)))

(defn headers-v3 [token n]
  (merge {:accept     "application/vnd.github.v3.star+json"
          :user-agent "clojj"}
         (if (pos? n) {:per-page n} {:all-pages true}) {:auth token}))

(def repo-name [:repo :name])
(def repo-owner [:repo :owner :login])

(defn getby-keys [keys repos-map] (map #(vec (for [key keys] (get-in % key))) repos-map))

(defn my-starred [token n & keys]
  (getby-keys keys (repos/starring "clojj"
                                   (headers-v3 token n))))


; (pprint (doall (my-starred "token" -1 [:repo :git_url])))

(defn commits-since [token n [user repo] days & keys]
  (getby-keys keys (repos/commits user repo
                                  (merge (headers-v3 token n)
                                         {:since (-> (LocalDateTime/now) (.minus (Period/ofDays days)) (.toString))}))))

; (pprint (doall (commits-since "token" -1 ["TheClimateCorporation" "claypoole"] 23 [:sha] [:commit :message])))


(defn get-commits [[v token n days]]
  {:repo v
   :commits (commits-since token n v days [:sha] [:commit :message])})

; (def pool (cp/threadpool 20))
; (def rs (doall (my-starred "token" -1 [:repo :owner :login] [:repo :name])))
; (pprint (doall (cp/upmap pool get-commits (for [v (take 5 rs)] [v "token" -1 12]))))