(ns re-colls.datatable.core
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame :refer [debug]]))


(def root-db-path [:re-colls :datatable])


(defn as-is [val]
  val)



(re-frame/reg-sub-raw
  ::data
  (fn [db [_ db-id data-sub]]
    (let [data (re-frame/subscribe data-sub)]
      (reaction
        @data))))



(defn datatable [db-id data-sub columns-def & [options]]
  (let [view-data (re-frame/subscribe [::data db-id data-sub])]
    (fn [db-id data-sub columns-def & [options]]
      [:table
       [:thead
        [:tr
         (doall
           (for [{:keys [key label]} columns-def]
             ^{:key key}
             [:th label]))]]

       [:tbody
        (doall
          (for [[i data-entry] (map-indexed vector @view-data)]
            ^{:key i}
            [:tr
             (doall
               (for [{:keys [key render-fn]} columns-def]
                 ^{:key key}
                 [:td
                  (if render-fn
                    [render-fn (get data-entry key)]
                    (get data-entry key))]))]))]])))
