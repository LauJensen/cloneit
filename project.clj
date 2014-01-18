(defproject cloneit "1.0.0"
  :description      "Cloning Reddit using Clojure"
  :url              "http://www.bestinclass.dk"
  :library-path     "lib/"
  :namespaces       [cloneit]
  :main             cloneit
  :dependencies     [[org.clojure/clojure "1.2.0"]
		     [org.clojure/clojure-contrib "1.2.0"]
		     [compojure "0.3.2"] [joda-time "1.6"]]
  :dev-dependencies [[swank-clojure "1.1.0"]])
