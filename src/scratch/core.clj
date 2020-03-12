(ns scratch.core
  (:require [net.cgrand.enlive-html :as html]
            [clojure.string :as string]
            [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:gen-class))

(defn print-n-return [thing-to-print]
  (println thing-to-print "\n")
  thing-to-print)

(defn get-page-body [url]
  (html/html-resource (java.net.URL. url)))

(defn get-nested-page-data [nesting page-data]
  (html/select page-data nesting))

(defn get-elem-attr-content
  [urls]
  (map #(get-in %1 [:attrs :content])
       urls))

(defn follow-link-for-redirect
  [url]
    (as-> url $
      (get-page-body $)
      (html/select $ [:meta])
      (filter #(contains? (:attrs %1) :http-equiv) $)
      (filter #(= "redirect" (get-in %1 [:attrs :http-equiv])) $)))

(defn filter-valid-links [page-data]
  (filter #(as-> %1 $
             (get-in $ [:attrs :href])
             (re-seq #"^https?:\/\/" $)
             (empty? $)
             (not $))
          page-data))

(defn get-urls-from-page
  [url nesting]
  (->> url
       (get-page-body)
       (get-nested-page-data nesting)
       (filter-valid-links)
       (reduce #(merge %1 {(html/text %2)
                           (as-> %2 $
                             (get-in $ [:attrs :href])
                             (try (let [redirect-elems (follow-link-for-redirect $)]
                                    (if (empty? redirect-elems)
                                      $
                                      (-> redirect-elems
                                          (print-n-return)
                                          (get-elem-attr-content)
                                          (first)
                                          (string/replace #"0;URL=" ""))))
                                  (catch Exception e (.getMessage e))))})
               {})))

(defn write-csv
  [filename contents]
  (with-open [writer (io/writer filename)]
    (csv/write-csv writer
                   contents)))

(defn ambient-sounds []
  (write-csv "ambient-sounds.csv"
             (get-urls-from-page "https://nyriad.tumblr.com/post/176288223047/ambient-sounds-for-writers" [:blockquote :blockquote :blockquote :blockquote :blockquote :blockquote :blockquote :blockquote :p :a])))

(defn hn []
  (write-csv "hn.csv"
             (get-urls-from-page "https://news.ycombinator.com/" [:td.title :a])))

(defn -main
  [& args]
  (ambient-sounds)
  (hn))
