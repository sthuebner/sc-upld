(ns sthuebner.superupload.core
  (:use [ring.handler dump]
	[ring.middleware file file-info lint]
	[ring.util response]))


(comment
  
  (use 'ring.util.serve)
  (binding [clojure.java.browse/*open-url-script* "/usr/bin/chromium"]
    (serve app))

)

(def app
  (-> handle-dump
      wrap-lint
      (wrap-file "html")
      wrap-file-info
      wrap-lint
      ))
