;;;; Application Core
;;;;
;;;; Used libraries:
;;;; - Ring https://github.com/mmcgrana/ring
;;;; - Moustache https://github.com/cgrand/moustache
;;;; - Ring uses Apache Commons FileUpload

(ns sthuebner.superupload.core
  (:require [sthuebner.superupload.storage :as storage]
	    [clojure.contrib.json :as json])
  (:use sthuebner.superupload.middleware
	clojure.test
	[ring.handler dump]
	[ring.middleware file-info keyword-params params resource]
	[ring.util codec response]
	[net.cgrand.moustache :only [app]]))


(comment
  ;;; start the app, fire up a browser
  (use 'ring.util.serve)
  (binding [clojure.java.browse/*open-url-script* "/usr/bin/chromium"]
    (serve app))
)


;;; extending clojure.contrib.json/Write-JSON to java.io.File
;;; to conveniently serialize file names
(extend java.io.File json/Write-JSON
        {:write-json (fn [x ^java.io.PrintWriter out]
                       (json/write-json (.toString x) out))})



;;;; request handlers

(def not-found {:status 404})

(defn- upload-post-handler
  "Creates an upload handler for a given file ID."
  [id]
  (fn [req]
    {:status 204
     :headers {"Location" (str "/upload/" id)}}))

(defn- upload-get-handler
  "Provide a human readable description about a given upload"
  [id]
  (fn [req]
    (when-let [upload (storage/get-upload id)]
      (-> (str "<html><body>"
	       "Thanks! Your file <a href=\"/upload/" id "/file\""
               " type=\"" (:content-type upload) "\""
               ">" (:filename upload)
	       "</a> has been stored as <i>" (:local-file upload)
	       "</i>. Description: '" (:description upload)
	       "'</body></html>")
	  response
	  (content-type "text/html")))))

(defn- upload-get-json-handler
  "Provide data about a given upload - for machine consumption"
  [id]
  (fn [req]
    (when-let [upload (storage/get-upload id)]
      (-> upload json/json-str response))))


(defn- progress-handler
  "Creates a progress handler for a given file ID.
Progress is represented as <bytes-uploaded>/<filesizes>"
  [id]
  (fn [req]
    (when-let [upload (storage/get-upload id)]
      (response (str (:bytes-uploaded upload) "/" (:filesize upload))))))


(defn- download-handler
  "Creates a handler to download a given upload"
  [id]
  (fn [req]
    (when-let [upload (storage/get-upload id)]
      (let [file (.toString (:local-file upload))
	    type (:content-type upload)]
	(-> file file-response
	    (content-type type)
            (header "Content-disposition"
                    (str "attachment; filename=\""
                         (:filename upload)
                         "\"")))))))


(defn- description-post-handler
  "Handler to update a given upload's description.
Responds with redirecting the user to the upload information page."
  [id]
  (fn [{params :params}]
    (when (storage/exists-upload? id)
      (storage/update-upload id (select-keys params [:description]))
      (redirect-after-post (str "/upload/" id)))))





;;;; application routing

(def ^{:doc "Application Endpoints"}
  endpoints

  ;; app is the central Macro provided by Moustache
  (app

   ;; provide static resources off the "public" folder
   [#".*\.(css|html|js)"] (app wrap-file-info
			       (wrap-resource "public")
			       not-found)
   
   ;; specific endpoints and their handlers:
   
   ;; POST /upload/[id] => upload files
   ;; GET /upload/[id] => get status for uploaded files
   ["upload" id] {:post (app
			 ;; parses multipart forms, uploads contained
			 ;; files, and updates the upload storage
			 (wrap-multipart-params-with-storage id)

			 ;; return 204
			 (upload-post-handler id))

		  :get (app
			;; respond with JSON if requested so
			(content-type-dispatch "application/json" (upload-get-json-handler id))
			;; otherwise
			(upload-get-handler id))}

   ;; GET /upload/[id]/progress => get progress for uploading file
   ["upload" id "progress"] {:get (progress-handler id)}

   ;; GET /upload/[id]/file => download the uploaded file
   ["upload" id "file"] {:get (download-handler id)}

   ;; POST /upload/[id]/description => save description => redirect to /upload/[id]
   ["upload" id "description"] {:post (app wrap-params
					   wrap-keyword-params
					   (description-post-handler id))}
   
   ;; /
   [""] (fn [req] (redirect "/upload.html"))))




;;;; Test Helpers

(defn- GET
  "Requests the given URI from the endpoints"
  [uri]
  (endpoints {:request-method :get, :uri uri, :headers {}}))

(defn- http-ok? [res]
  (= 200 (:status res)))

(defn- http-redirect? [res]
  (= 302 (:status res)))

(defn- http-not-found? [res]
  (= 404 (:status res)))


;;;; Test fixtures

(defn- dummy-upload-fixture
  [f]
  ;; populate storage
  (storage/add-upload "lolcat" (-> (storage/make-upload "lolcat.png" "image/jped" 57951)
                                   (assoc :local-file "test/lolcat.jpg"
                                          :status "complete")))
  
  ;; run the test
  (f)
  )

(defn- reset-storage-fixture
  [f]
  ;; run the test
  (f)
  ;; reset storage
  (storage/reset-uploads)
  )

(use-fixtures :once dummy-upload-fixture reset-storage-fixture)

;;;; Tests

(deftest test-endpoints
  (is (http-ok? (GET "/upload.html")))
  (is (http-ok? (GET "/upload/lolcat")))
  (is (http-ok? (GET "/upload/lolcat/progress")))
  (is (http-ok? (GET "/upload/lolcat/file")))
  (is (http-not-found? (GET "/upload/foo")))
  (is (http-not-found? (GET "/upload/foo/progress")))
  (is (http-not-found? (GET "/upload/foo/file")))
  (is (http-redirect? (GET "/")))
  )

(import [java.io ByteArrayInputStream])

(deftest test-full-cycle
  (let [uuid (.toString (java.util.UUID/randomUUID))
        
        upload-url (str "/upload/" uuid)
        progress-url (str upload-url "/progress")
        file-url (str upload-url "/file")
        
        form-body (str "--XXXX\r\n"
                       "Content-Disposition: form-data;"
                       "name=\"file\"; filename=\"test.txt\"\r\n"
                       "Content-Type: text/plain\r\n\r\n"
                       "foo\r\n"
                       "--XXXX--")
        request {:content-type "multipart/form-data; boundary=XXXX"
                 :content-length (count form-body)
                 :params {"foo"}
                 :body (ByteArrayInputStream. (.getBytes form-body))}]

    (testing "POST /upload/[id]"
      (let [response (endpoints (merge {:request-method :post
                                        :uri upload-url}
                                       request))]
        (is (= 204 (:status response)))
        (is (= upload-url (get-in response [:headers "Location"])))))

    (testing "GET /upload/[id] JSON response"
      (let [response (endpoints {:uri upload-url
                                 :request-method :get
                                 :headers {"accept" "application/json"}})]
        (is (http-ok? response))
        (is (= "application/json" (get-in response [:headers "Content-Type"])) "Content-Type should be application/json")
        (let [json-body (json/read-json (:body response))]
          (are [key expected] (= expected (key json-body))
               :filename "test.txt"
               :filesize 3
               :content-type "text/plain"
               :status "complete"
               :description nil))))
    
    (testing "GET /upload/[id]/progress"
      (let [progress-response (GET progress-url)]
       (is (http-ok? progress-response))
       (is (= "3/3" (:body progress-response)))))
    
    (testing "GET /upload/[id]/file"
      (let [file-response (GET file-url)]
        (is (http-ok? file-response))
        (is (= (:body file-response) (:local-file (storage/get-upload uuid))))
        (are [header expected] (= expected (get-in file-response [:headers header]))
             "Content-disposition" "attachment; filename=\"test.txt\""
             "Content-Type" "text/plain")))))



;; run them automatically
(run-tests *ns*)
