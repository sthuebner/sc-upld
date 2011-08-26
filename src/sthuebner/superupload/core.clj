(ns sthuebner.superupload.core
  (:use [ring.handler dump]
	[ring.middleware file-info lint resource]
	[ring.util response]))


(comment
  
  (use 'ring.util.serve)
  (binding [clojure.java.browse/*open-url-script* "/usr/bin/chromium"]
    (serve app))

)

(def app
  (-> handle-dump
      wrap-lint
      (wrap-resource "public")
      wrap-file-info
      wrap-lint
      ))
