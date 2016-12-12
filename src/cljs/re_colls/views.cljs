(ns re-colls.views
  (:require [re-frame.core :as re-frame]
            [re-colls.datatable.core :as datatable]
            [re-colls.subs :as subs]))



(defn main-panel []
  [datatable/datatable
   :songs
   [::subs/songs-list]
   {}])
