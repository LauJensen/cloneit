(ns cloneit
  (:gen-class)
  (:use    compojure clojure.contrib.duck-streams)
  (:import (org.joda.time DateTime Duration Period)))

(defmacro this-file [] (str "src/" *file*))
(def data  (ref {"http://www.bestinclass.dk" {:title "Best in Class" :points 1 :date (DateTime.)}}))

(def formatter
     (.toPrinter (doto (org.joda.time.format.PeriodFormatterBuilder.)
		   .appendDays    (.appendSuffix " day "    " days ")
		   .appendHours   (.appendSuffix " hour "   " hours ")
		   .appendMinutes (.appendSuffix " minute " " minutes ")
		   .appendSeconds (.appendSuffix " second " " seconds "))))

(defn pprint [stamp]
  (let [retr   (StringBuffer.)
	period (Period. (Duration. stamp (DateTime.)))]
    (.printTo formatter retr period (java.util.Locale. "US"))
    (str retr)))

(defn render-links [keyfn cmp]
  (for [link (sort-by keyfn cmp @data)]
    (let [[url {:keys [title points date]}] link]
      [:li
       (link-to url title)
       [:span (format " Posted %s ago. %d %s " (pprint date) points "points")]
       (link-to (str "/up/" url)   "Up")
       (link-to (str "/down/" url) "Down")])))

(defn with-head [title & body]
  (html
   [:head
    [:title title]
    (include-css "/styles/reddit.css")]
   [:body body]))

(defn reddit-new-link [msg]
  (with-head "Reddit.Clojure - Submit to our authority"
    [:h1 "Reddit.Clojure - Submit a new link"]
    [:h3 "Submit a new link"]
    (when msg [:p {:style "color: red;"} msg])
    (form-to [:post "/new/"]
     [:input {:type "Text" :name "url" :value "http://" :size 48 :title "URL"}]
     [:input {:type "Text" :name "title" :value "" :size 48 :title "Title"}]
     (submit-button "Add link"))
    (link-to "/" "Home")]))

(defn reddit-home []
  (html
   [:head
    [:title "Reddit.Clojure"]
    (include-css "/styles/reddit.css")]
   [:body
    [:h1 "Reddit.Clojure"]
    [:h3 (format "In exactly %d lines of gorgeous Clojure"
		 (->> (this-file) reader line-seq count))]
    [:a {:href "/"} "Refresh"] [:a {:href "/new/"} "Add link"]
    [:h1 "Highest ranking list"]
    [:ol (render-links #(:points (val %))  >)]
    [:h1 "Latest link"]  
    [:ol (render-links #(.getMillis (Duration. (:date %) (DateTime.))) >)]]))

(defn invalid-url? [url]
  (or (empty? url)
      (not (try (java.net.URL. url) (catch Exception e nil)))))

(defn add-link [[title url]]
  (redirect-to
   (cond
    (invalid-url? url) "/new/?msg=Invalid URL"
    (empty? title)     "/new/?msg=Invalid Title"
    (@data url)        "/new/?msg=Link already submitted"
    :else
    (dosync
     (alter data assoc url {:title title :date (DateTime.) :points 1})
     "/"))))

(defn rate [url mfn]
  (dosync
   (when (@data url)
     (alter data update-in [url :points] mfn)))
  (redirect-to "/"))

(defroutes reddit
  (GET  "/"         (reddit-home))
  (GET  "/new/*"    (reddit-new-link (:msg params)))
  (POST "/new/"     (add-link (map #(params %) [:title :url])))
  (GET  "/up/*"     (rate (:* params) inc))
  (GET  "/down/*"   (rate (:* params) dec))
  (GET  "/styles/*" (serve-file "res" (params :*)))
  (ANY "*"  404))

(defn -main [& args]
  (run-server {:port 8080} "/*" (servlet reddit)))