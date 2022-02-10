(ns twtr-cli.core
  (:require [clojure.string :as str]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [arco.core :as arco]
            [clj-http.client :as http]
            [nrepl.server :as nrepl]
            [io.aviso.ansi :as ansi]
            [oauth.client :as oa])
  (:import (java.text SimpleDateFormat)
           (org.jsoup Jsoup)))


(defn creds
  [{:keys [api-key api-key-secret
           access-token access-token-secret]}]
  {:consumer (oa/make-consumer
              api-key
              api-key-secret
              "https://twitter.com/oauth/request_token"
              "https://twitter.com/oauth/access_token"
              "https://twitter.com/oauth/authorize"
              :hmac-sha1)
   :token    access-token
   :secret   access-token-secret})



(defn fetch!
  [{:keys [consumer token secret]} method url query]
  (let [creds    (oa/credentials consumer
                                 token
                                 secret
                                 method
                                 url
                                 query)
        response (http/get url
                           {:query-params (merge creds query)})]
    (-> response :body (json/read-str :key-fn keyword))))


(defn poast!
  [{:keys [consumer token secret]} method url query]
  (let [creds    (oa/credentials consumer
                                 token
                                 secret
                                 method
                                 url
                                 query)
        response (http/post url
                                {:query-params (merge creds query)
                                 :content-type "application/json"})]

    (-> response :body (json/read-str :key-fn keyword))))



(defn lines
  [t]
  (re-seq
   (re-pattern (str ".{1,72}\\s|.{1,72}"))
   (str/replace t #"\n" " ")))


(defn html-decoded
  [t]
  (-> t Jsoup/parse .text))


(defn text
  [t]
  (->> t
       :full_text
       html-decoded
       lines
       (str/join "\n")))


(defn quoted-text
  [t]
  (->> t
       :full_text
       html-decoded
       lines
       (map #(str "\t" %))
       (str/join "\n")))


(defn screen-name
  [t]
  (-> t :user :screen_name))


(defn created-at
  [t]
  (let [ts-fmt "EEE MMM dd HH:mm:ss ZZZZZ yyyy"]
    (-> [(-> (java.text.SimpleDateFormat. ts-fmt)
             (.parse (:created_at t)))]
        arco/time-since)))


(defn tweet-formatted
  [t]
  (let [t (get t :retweeted_status t)]
    (if-let [qt (:quoted_status t)]
      (apply format
             "%s \n \n - %s, %s \n \n %s \n \n \t - %s, %s"
             [(-> t text ansi/bold-blue)
              (-> t screen-name ansi/bold-cyan)
              (-> t created-at ansi/bold-green)
              (-> qt quoted-text ansi/italic ansi/bold-magenta)
              (-> qt screen-name ansi/italic ansi/bold-cyan)
              (-> qt created-at ansi/italic ansi/bold-green)])
      (apply format
             "%s \n \n - %s, %s"
             [(-> t text ansi/bold-blue)
              (-> t screen-name ansi/bold-cyan)
              (-> t created-at ansi/bold-green)]))))


(defn flush!
  [ts]
  (try
    (doseq [t (map tweet-formatted ts)]
      (println t)
      (println "\n\n")
      (flush))
    (catch Exception ex
      (println "There was an error while calling Twitter. Trying again."))))


(defn query-params
  [sid]
  (if sid
    {:tweet_mode "extended",
     :count      100,
     :since_id   sid}
    {:tweet_mode "extended",
     :count      100}))


(defn -main
  [& args]
  (let [[account op arg] args]
    (println (str "USING ACCOUNT:" account))
    (prn op)
    (prn arg)
    (let [credentials (creds
                       ((keyword account)
                        (edn/read-string (slurp "secrets.edn"))))]
      (case op
        "poast" (let [url "https://api.twitter.com/1.1/statuses/update.json"]
                  (poast!
                   credentials
                   :POST
                   url
                   {:status arg}))
        "fetch" (let [sid    nil
                      url "https://api.twitter.com/1.1/statuses/home_timeline.json"
                      query  (query-params sid)
                      tweets (sort-by :id
                                      (fetch!
                                       credentials
                                       :GET
                                       url
                                       query))]
                  (flush! tweets))

        "Unsupported Command!"))))



(comment

  (require '[arco.core :as arco])
  (arco/time-since ["Fri Jan 10 19:28:56 +0000 2020"])
  (arco/time-since [(-> (java.text.SimpleDateFormat. "EEE MMM dd HH:mm:ss ZZZZZ yyyy")
                        (.parse "Fri Jan 10 19:28:56 +0000 2020"))])


  ;; post body
  (map #(select-keys % [:sender_id :message_data :target])
       (map :message_create (-> ts :events)))


  (fetch!
   (creds (edn/read-string (slurp "secrets.edn")))
   :GET
   url
   (query-params nil))

  )
