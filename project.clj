(defproject soundcloud-challenge "1-SNAPSHOT"
  :description "sthuebner's solution to SoundCloud's dev challenge"

  :dependencies [[org.clojure/clojure "1.2.1"]
		 [org.clojure/clojure-contrib "1.2.0"]
		 [ring-core "0.3.11"]]

  ;; first do 'lein plugin install lein-ring 0.4.5'
  :ring {:handler sthuebner.superupload.core/app})
