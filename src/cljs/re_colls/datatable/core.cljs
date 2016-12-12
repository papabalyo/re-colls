(ns re-colls.datatable.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [debug]]))


(def root-db-path [:re-colls :datatable])



(re-frame/reg-sub-raw
  ::data
  (fn [db [_ db-id data-sub]]
    (let [data (re-frame/subscribe data-sub)]
      (reaction
        @data))))



(defn datatable [db-id data-sub columns-def & [options]]
  (let [view-data (re-frame/subscribe [::data db-id data-sub])]
    (fn [db-id data-sub columns-def & [options]]
      [:div (str @view-data)])))
