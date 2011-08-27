;;;; Application Core

(ns sthuebner.superupload.core
  (:use clojure.test
	[ring.handler dump]
	[ring.middleware file-info lint resource]
	[ring.util codec response]
	[net.cgrand.moustache :only [app]]))


(comment
  
  (use 'ring.util.serve)
  (binding [clojure.java.browse/*open-url-script* "/usr/bin/chromium"]
    (serve app))

)



;;;; requeset handlers

(defn- upload-form
  "Provides the Upload form"
  [req]
  (let [path (.substring (url-decode (:uri req)) 1)]
    (-> (resource-response "public/upload.html")
	(content-type "text/html"))))

(defn- upload-handler
  "Handler for file uploads"
  [req]
  (response ""))

(defn- progress-handler
  "Provides progress information on file uploads"
  [req]
  (response ""))



;;;; application routing

(def ^{:doc "Application Endpoints"}
  endpoints
  (app ["upload"] {:get upload-form
		   :post upload-handler}
       ["progress"] progress-handler
       [""] (fn [req] (redirect "/upload"))))




;;;; Tests

(defn- http-ok? [res]
  (= 200 (:status res)))

(defn- http-redirect? [res]
  (= 302 (:status res)))

(deftest test-endpoints
  (is (http-ok? (endpoints {:uri "/upload" :request-method :get})))
  (is (http-ok? (endpoints {:uri "/progress" :request-method :get})))
  (is (http-redirect? (endpoints {:uri "/" :request-method :get})))
  )
