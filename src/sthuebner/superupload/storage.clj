;;;; Uploads Storage
;;;;
;;;; Commons FileUpload is used to temporarily store uploaded files on disk.
(ns sthuebner.superupload.storage)


;; the uploads storage is wrapped in an atom for save concurrent access
(defonce ^{:doc "Map of recent uploads"}
  uploads (atom {}))

(defn add-upload
  "Thread-safely add entry to the uploads Map."
  [id entry]
  (swap! uploads assoc id entry))

(defn get-upload
  "Returns an upload for a given ID."
  [id]
  (get @uploads id))

(defn update-upload
  "Updates a given entry"
  [id props]
  (swap! uploads assoc id (merge (get-upload id) props)))

(defn exists?
  [id]
  (contains? @uploads id))

(defn remove-upload
  [id]
  (swap! uploads dissoc id))


;;;; accessors to the properties of uploads
;;;; encapsulating the inner structure of upload entries against the outer world

(defn filename
  [id]
  (-> id get-upload :filename))

(defn filesize
  [id]
  (-> id get-upload :size))

(defn bytes-uploaded
  [id]
  (long (or (-> id get-upload :bytes-uploaded) 0)))

(defn set-bytes-uploaded
  [id n]
  (update-upload id {:bytes-uploaded n}))

(defn content-type
  [id]
  (-> id get-upload :content-type))

(defn status
  [id]
  (-> id get-upload :status))

(defn local-file
  [id]
  (-> id get-upload :tempfile))

(defn description
  [id]
  (-> id get-upload :description))
