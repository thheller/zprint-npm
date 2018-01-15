(ns zprint.npm
  (:require [cljs.reader :refer [read-string]]
            [zprint.core :as zp]
            ["fs" :as fs]
            ["os" :as os]))

(defn slurp [file]
  (-> (fs/readFileSync file "utf8")
      (.toString)))

(defn spit [file data]
  (fs/writeFileSync file data))

(defn get-zprintrc-file-str
  "Look up 'HOME' in the environment, and build a string to find ~/.zprintrc"
  []
  (let [home-str (os/homedir)]
    (when home-str (str home-str "/.zprintrc"))))

(defn set-zprintrc!
  "Read in any ~/.zprintrc file and set it in the options."
  []
  (let [zprintrc-file-str (get-zprintrc-file-str)]
    (try (when zprintrc-file-str
           (let [zprintrc-str (slurp zprintrc-file-str)]
             (when zprintrc-str
               (zp/set-options! (read-string zprintrc-str)
                 (str "File: " zprintrc-file-str)))))
         (catch :default e
           (str "Failed to use .zprintrc file: '"
                zprintrc-file-str
                "' because: "
                e
                ".")))))

(defn main
  "Read a file from stdin, format it, and write it to sdtout."
  [options]
  (set-zprintrc!)
  (when (seq options)
    (try
      (zp/set-options! (read-string options))
      (catch :default e
        (println "Failed to use command line options: '"
          options
          "' because: "
          e
          "."))))

  (let [input-ref (atom "")]

    (js/process.stdin.on "data"
      (fn [chunk]
        (swap! input-ref str chunk)))

    (js/process.stdin.on "end"
      (fn []
        (let [input @input-ref]
          (try
            (println (zp/zprint-file-str input "<stdin>"))
            (catch :default e
              (js/process.stderr.write (str "Failed to zprint: " e "\n" input))
              (js/process.exit 1)
              )))))))
