(ns scratch.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:gen-class))

(defn get-page-body [url]
  (html/html-resource (java.net.URL. url)))

(defn get-nested-page-data [page-data]
  (html/select page-data [:blockquote :blockquote :blockquote :blockquote :blockquote :blockquote :blockquote :blockquote :p :a]))

(defn get-elem-attr-content
  [urls]
  (map (fn [elem]
         (get-in elem [:attrs :content]))
       urls))

(defn get-elem-with-redirect
  [url]
  (as-> url $
    (get-page-body $)
    (html/select $ [:meta])
    (filter #(contains? (:attrs %1) :http-equiv) $)))

(defn get-song-urls-from-page
  [url ]
  (->> url
      (get-page-body)
      (get-nested-page-data)
      (reduce #(merge %1 {(html/text %2)
                          (-> %2
                              (get-in [:attrs :href])
                              (get-elem-with-redirect)
                              (get-elem-attr-content)
                              (first)
                              (string/replace #"0;URL=" ""))})
              {})))

(defn write-csv
  [filename contents]
  (with-open [writer (io/writer filename)]
    (csv/write-csv writer
                   contents)))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (write-csv "atmospheric-music.csv" 
             (get-song-urls-from-page "https://nyriad.tumblr.com/post/176288223047/ambient-sounds-for-writers")))
