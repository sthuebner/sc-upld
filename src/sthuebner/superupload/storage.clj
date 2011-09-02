;;;; Uploads Storage
;;;;
;;;; Commons FileUpload is used to temporarily store uploaded files on disk.
(ns sthuebner.superupload.storage
  (:use clojure.test))


;;;; Upload record - defining the protocol of storage entries

(defrecord Upload
    [filename content-type filesize status local-file bytes-uploaded description])

(defn make-upload
  ([filename content-type filesize]
     (make-upload filename content-type filesize nil nil nil nil))
  ([filename content-type filesize status local-file bytes-uploaded description]
     (Upload. filename
              content-type
              (when filesize (long filesize))
              status
              local-file
              (when bytes-uploaded (long bytes-uploaded))
              description)))


;; the uploads storage is wrapped in an atom for save concurrent access
(defonce ^{:doc "Map of recent uploads"}
  uploads (atom {}))

(defn add-upload
  "Thread-safely add entry to the uploads Map."
  [id ^Upload entry]
  (if-not (= (type entry) Upload)
    (throw (IllegalArgumentException. "Provided entry must be of type Upload record")))
  (swap! uploads assoc id entry)
  entry)

(defn get-upload
  "Returns an upload for a given ID."
  [id]
  (get @uploads id))

(defn update-upload
  "Updates a given entry"
  [id props]
  (swap! uploads assoc id (merge (get-upload id) props))
  (get-upload id))

(defn exists-upload?
  [id]
  (contains? @uploads id))

(defn remove-upload
  [id]
  (let [entry (get-upload id)]
    (swap! uploads dissoc id)
    entry))

(defn reset-uploads
  "FOR TESTING PURPOSES ONLY. Resets the storage."
  []
  (swap! uploads {}))


;;;; storage tests

(defn- reset-storage-fixture
  [f]
  ;; run test
  (f)
  ;; reset storage
  (swap! uploads {}))

(use-fixtures :each reset-storage-fixture)

(deftest test-make-upload
  (let [upload (make-upload "Test" "text/plain" 3)]
    (are [key value] (= value (key upload))
         :filename "Test"
         :content-type "text/plain"
         :filesize 3)))

(deftest test-upload-storage
  (let [upload (make-upload "foo.txt" "text/plain" 3)
        uuid (.toString (java.util.UUID/randomUUID))]
    
    (testing "add-upload"
      (is (identical? upload (add-upload uuid upload)) "Should return the entry")
      (is (thrown? IllegalArgumentException (add-upload {:a "b"})) "Should only accept Upload records"))
    
    (testing "get-upload"
      (is (identical? upload (get-upload uuid)) "Should return the entry with the given ID"))

    (testing "update-upload"
      (let [updated-entry (update-upload uuid {:status "incomplete"})]
        (is (= Upload (type updated-entry)) "Should return the updated entry")
        (are [k v] (= v (k updated-entry))
             :filename "foo.txt"
             :content-type "text/plain"
             :filesize 3
             :status "incomplete")))

    (testing "remove-upload"
      (let [entry (get-upload uuid)]
        (is (= entry (remove-upload uuid)) "Should return the removed upload")
        (is (nil? (get-upload uuid)) "Entry should be removed from storage")
        ;; putting it back
        (add-upload uuid entry)))

    (testing "exists-upload?"
      (let [entry (get-upload uuid)]
        (is (exists-upload? uuid))
        (remove-upload uuid) ; removing the entry
        (is (not (exists-upload? uuid)))
        (add-upload uuid entry) ; putting it back
        ))))

(deftest test-reset-storage
  (let [upload (make-upload "foo.txt" "text/plain" 3)
        uuid (.toString (java.util.UUID/randomUUID))]
    (add-upload uuid upload)
    (is (exists-upload? uuid))
    (reset-uploads)
    (is (not (exists-upload? uuid)))))


(run-tests *ns*
           ;;'sthuebner.superupload.core
           )
